#!/usr/bin/env python3
"""Convert Markdown files in a directory into separate PDF files."""

from __future__ import annotations

import argparse
import html
import re
from pathlib import Path

import fitz


HEADING_RE = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE_RE = re.compile(r"^([`~]{3,})(.*)$")
LIST_ITEM_RE = re.compile(r"^(\s*)([-*+]|\d+\.)\s+(.*)$")
TABLE_DELIMITER_RE = re.compile(
    r"^\s*\|?(?:\s*:?-{3,}:?\s*\|)+\s*:?-{3,}:?\s*\|?\s*$"
)


def is_blank(line: str) -> bool:
    return not line.strip()


def is_thematic_break(line: str) -> bool:
    stripped = line.strip()
    if not stripped:
        return False
    if stripped in {"---", "***", "___"}:
        return True
    return bool(re.match(r"^([-*_])(?:\s*\1){2,}\s*$", stripped))


def escape_attr(value: str) -> str:
    return html.escape(value, quote=True)


def render_inline(text: str) -> str:
    code_placeholders: dict[str, str] = {}

    def save_code(match: re.Match[str]) -> str:
        key = f"__CODE_{len(code_placeholders)}__"
        code_placeholders[key] = f"<code>{html.escape(match.group(1))}</code>"
        return key

    text = re.sub(r"`([^`]+)`", save_code, text)
    text = html.escape(text)

    text = re.sub(
        r"!\[([^\]]*)\]\(([^)]+)\)",
        lambda m: (
            f'<img alt="{escape_attr(m.group(1))}" '
            f'src="{escape_attr(m.group(2).strip())}" />'
        ),
        text,
    )
    text = re.sub(
        r"\[([^\]]+)\]\(([^)]+)\)",
        lambda m: (
            f'<a href="{escape_attr(m.group(2).strip())}">{m.group(1)}</a>'
        ),
        text,
    )
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"__([^_]+)__", r"<strong>\1</strong>", text)
    text = re.sub(r"~~([^~]+)~~", r"<del>\1</del>", text)
    text = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", text)
    text = re.sub(r"(?<!_)_([^_]+)_(?!_)", r"<em>\1</em>", text)

    for key, value in code_placeholders.items():
        text = text.replace(key, value)
    return text


def split_table_row(line: str) -> list[str]:
    row = line.strip()
    if row.startswith("|"):
        row = row[1:]
    if row.endswith("|"):
        row = row[:-1]
    return [cell.strip() for cell in row.split("|")]


class MarkdownRenderer:
    def __init__(self, lines: list[str]) -> None:
        self.lines = lines
        self.count = len(lines)

    def render(self) -> str:
        parts: list[str] = []
        i = 0
        while i < self.count:
            line = self.lines[i]
            if is_blank(line):
                i += 1
                continue

            fence = FENCE_RE.match(line)
            if fence:
                html_block, i = self.parse_code_block(i, fence)
                parts.append(html_block)
                continue

            if self.is_table_start(i):
                html_block, i = self.parse_table(i)
                parts.append(html_block)
                continue

            heading = HEADING_RE.match(line)
            if heading:
                level = len(heading.group(1))
                parts.append(f"<h{level}>{render_inline(heading.group(2).strip())}</h{level}>")
                i += 1
                continue

            if is_thematic_break(line):
                parts.append("<hr />")
                i += 1
                continue

            if line.lstrip().startswith(">"):
                html_block, i = self.parse_blockquote(i)
                parts.append(html_block)
                continue

            if LIST_ITEM_RE.match(line):
                html_block, i = self.parse_list(i)
                parts.append(html_block)
                continue

            html_block, i = self.parse_paragraph(i)
            parts.append(html_block)

        return "\n".join(parts)

    def is_table_start(self, index: int) -> bool:
        if index + 1 >= self.count:
            return False
        current = self.lines[index]
        nxt = self.lines[index + 1]
        if "|" not in current:
            return False
        return bool(TABLE_DELIMITER_RE.match(nxt.strip()))

    def parse_code_block(self, index: int, fence_match: re.Match[str]) -> tuple[str, int]:
        opener = fence_match.group(1)
        fence_char = opener[0]
        fence_len = len(opener)
        language = fence_match.group(2).strip().split()[0] if fence_match.group(2).strip() else ""

        index += 1
        code_lines: list[str] = []
        while index < self.count:
            line = self.lines[index]
            stripped = line.strip()
            if stripped and all(ch == fence_char for ch in stripped) and len(stripped) >= fence_len:
                index += 1
                break
            code_lines.append(line)
            index += 1

        language_attr = f' data-lang="{escape_attr(language)}"' if language else ""
        code = html.escape("\n".join(code_lines))
        return f"<pre><code{language_attr}>{code}</code></pre>", index

    def parse_table(self, index: int) -> tuple[str, int]:
        header_cells = split_table_row(self.lines[index])
        index += 2
        body_rows: list[list[str]] = []
        while index < self.count:
            line = self.lines[index]
            if is_blank(line) or "|" not in line:
                break
            body_rows.append(split_table_row(line))
            index += 1

        parts = [
            "<table>",
            "<thead>",
            "<tr>",
            *[f"<th>{render_inline(cell)}</th>" for cell in header_cells],
            "</tr>",
            "</thead>",
            "<tbody>",
        ]
        for row in body_rows:
            parts.append("<tr>")
            for cell in row:
                parts.append(f"<td>{render_inline(cell)}</td>")
            parts.append("</tr>")
        parts.extend(["</tbody>", "</table>"])
        return "\n".join(parts), index

    def parse_blockquote(self, index: int) -> tuple[str, int]:
        quote_lines: list[str] = []
        while index < self.count:
            line = self.lines[index]
            if is_blank(line):
                quote_lines.append("")
                index += 1
                continue
            stripped = line.lstrip()
            if not stripped.startswith(">"):
                break
            content = stripped[1:]
            if content.startswith(" "):
                content = content[1:]
            quote_lines.append(content)
            index += 1

        inner = MarkdownRenderer(quote_lines).render()
        return f"<blockquote>\n{inner}\n</blockquote>", index

    def parse_list(self, index: int) -> tuple[str, int]:
        start_match = LIST_ITEM_RE.match(self.lines[index])
        assert start_match is not None
        current_indent = len(start_match.group(1).replace("\t", "    "))
        ordered = start_match.group(2).endswith(".")
        tag = "ol" if ordered else "ul"

        parts = [f"<{tag}>"]
        while index < self.count:
            line = self.lines[index]
            if is_blank(line):
                next_index = index + 1
                while next_index < self.count and is_blank(self.lines[next_index]):
                    next_index += 1
                if next_index >= self.count:
                    index = next_index
                    break
                next_match = LIST_ITEM_RE.match(self.lines[next_index])
                if not next_match or len(next_match.group(1).replace("\t", "    ")) < current_indent:
                    index = next_index
                    break
                index = next_index
                line = self.lines[index]

            match = LIST_ITEM_RE.match(line)
            if not match:
                break

            indent = len(match.group(1).replace("\t", "    "))
            if indent < current_indent:
                break
            if indent > current_indent:
                nested_html, index = self.parse_list(index)
                if len(parts) > 1 and parts[-1] == "</li>":
                    parts[-1] = nested_html + "\n</li>"
                elif len(parts) > 1:
                    parts.append(nested_html)
                continue

            item_ordered = match.group(2).endswith(".")
            if item_ordered != ordered:
                break

            item_lines = [match.group(3).strip()]
            index += 1

            while index < self.count:
                next_line = self.lines[index]
                if is_blank(next_line):
                    lookahead = index + 1
                    while lookahead < self.count and is_blank(self.lines[lookahead]):
                        lookahead += 1
                    if lookahead >= self.count:
                        index = lookahead
                        break
                    next_match = LIST_ITEM_RE.match(self.lines[lookahead])
                    if next_match:
                        look_indent = len(next_match.group(1).replace("\t", "    "))
                        if look_indent <= current_indent:
                            index = lookahead
                            break
                    item_lines.append("")
                    index += 1
                    continue

                next_match = LIST_ITEM_RE.match(next_line)
                if next_match:
                    look_indent = len(next_match.group(1).replace("\t", "    "))
                    if look_indent <= current_indent:
                        break
                    if look_indent > current_indent:
                        break

                next_indent = len(next_line) - len(next_line.lstrip(" "))
                if next_indent > current_indent:
                    item_lines.append(next_line.strip())
                    index += 1
                    continue
                break

            content = " ".join(part for part in item_lines if part != "").strip()
            parts.append(f"<li>{render_inline(content)}</li>")

        parts.append(f"</{tag}>")
        return "\n".join(parts), index

    def parse_paragraph(self, index: int) -> tuple[str, int]:
        lines: list[str] = []
        while index < self.count:
            line = self.lines[index]
            if is_blank(line):
                break
            if (
                FENCE_RE.match(line)
                or self.is_table_start(index)
                or HEADING_RE.match(line)
                or is_thematic_break(line)
                or line.lstrip().startswith(">")
                or LIST_ITEM_RE.match(line)
            ):
                break
            lines.append(line.strip())
            index += 1

        paragraph = " ".join(lines)
        return f"<p>{render_inline(paragraph)}</p>", index


def markdown_to_html(markdown_text: str, title: str) -> str:
    body = MarkdownRenderer(markdown_text.splitlines()).render()
    return (
        "<!DOCTYPE html>"
        "<html>"
        "<head>"
        f"<meta charset=\"utf-8\" /><title>{html.escape(title)}</title>"
        "</head>"
        "<body>"
        f"<main>{body}</main>"
        "</body>"
        "</html>"
    )


CSS = """
body {
  font-family: Helvetica, Arial, sans-serif;
  font-size: 11pt;
  line-height: 1.45;
  color: #1a1a1a;
}
main {
  width: 100%;
}
h1, h2, h3, h4, h5, h6 {
  font-family: Helvetica, Arial, sans-serif;
  font-weight: 700;
  color: #0f172a;
  margin: 0.75em 0 0.35em 0;
}
h1 {
  font-size: 22pt;
  border-bottom: 1px solid #d1d5db;
  padding-bottom: 8px;
}
h2 {
  font-size: 16pt;
  margin-top: 1.2em;
}
h3 {
  font-size: 13pt;
}
p, li, blockquote {
  margin: 0.35em 0;
}
ul, ol {
  margin: 0.3em 0 0.6em 1.2em;
  padding-left: 0.8em;
}
blockquote {
  margin: 0.8em 0;
  padding: 0.3em 0.8em;
  border-left: 3px solid #94a3b8;
  color: #334155;
  background: #f8fafc;
}
code {
  font-family: Courier, "Courier New", monospace;
  font-size: 9pt;
  background: #f3f4f6;
  padding: 1px 3px;
}
pre {
  font-family: Courier, "Courier New", monospace;
  font-size: 9pt;
  line-height: 1.35;
  white-space: pre-wrap;
  word-break: break-word;
  background: #0f172a;
  color: #e2e8f0;
  padding: 10px 12px;
  margin: 0.8em 0;
  border-radius: 4px;
}
pre code {
  background: transparent;
  color: inherit;
  padding: 0;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin: 0.8em 0;
  font-size: 10pt;
}
th, td {
  border: 1px solid #cbd5e1;
  padding: 6px 8px;
  vertical-align: top;
}
th {
  background: #e2e8f0;
  font-weight: 700;
}
hr {
  border: 0;
  border-top: 1px solid #cbd5e1;
  margin: 1em 0;
}
a {
  color: #0f4c81;
  text-decoration: underline;
}
img {
  max-width: 100%;
}
"""


def render_pdf(html_text: str, output_path: Path) -> None:
    story = fitz.Story(html=html_text, user_css=CSS, em=11)
    page_rect = fitz.paper_rect("letter")
    content_rect = page_rect + (36, 36, -36, -36)

    def rectfn(_rect_num: int, _filled: fitz.Rect) -> tuple[fitz.Rect, fitz.Rect, fitz.Matrix]:
        return page_rect, content_rect, fitz.Matrix(1, 1)

    document = story.write_with_links(rectfn)
    document.save(output_path)
    document.close()


def convert_file(source_path: Path, output_dir: Path) -> Path:
    output_path = output_dir / f"{source_path.stem}.pdf"
    markdown_text = source_path.read_text(encoding="utf-8")
    html_text = markdown_to_html(markdown_text, source_path.stem.replace("-", " "))
    render_pdf(html_text, output_path)
    return output_path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_dir", type=Path, help="Directory containing Markdown files")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Directory for generated PDFs. Defaults to <source_dir>/PDFs",
    )
    args = parser.parse_args()

    source_dir = args.source_dir.expanduser().resolve()
    output_dir = (args.output_dir or (source_dir / "PDFs")).expanduser().resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    markdown_files = sorted(path for path in source_dir.glob("*.md") if path.is_file())
    if not markdown_files:
        raise SystemExit(f"No Markdown files found in {source_dir}")

    for path in markdown_files:
        convert_file(path, output_dir)
        print(f"Created {output_dir / (path.stem + '.pdf')}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
