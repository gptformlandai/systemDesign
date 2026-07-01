# Linux Shell, Bash/Zsh, Environment, Pipes, and Redirection - Gold Sheet

> Track File #4 of 30 - Group 01: Foundations
> For: terminal fluency | Level: beginner to intermediate | Mode: shell mechanics

## 1. Core Idea

The shell parses your command before the program runs.

```text
shell parses -> expands variables/globs -> connects pipes/redirects -> starts process
```

Understanding shell parsing prevents many script and terminal mistakes.

---

## 2. Shell Concepts

| Concept | Example | Meaning |
|---|---|---|
| variable | `name=value` | shell variable |
| environment | `export PORT=8080` | inherited by child processes |
| pipe | `ps aux | grep nginx` | stdout of one command to stdin of next |
| redirect stdout | `cmd > out.txt` | write output to file |
| redirect stderr | `cmd 2> err.txt` | write errors to file |
| append | `cmd >> out.txt` | append instead of overwrite |
| command substitution | `$(date)` | insert command output |
| glob | `*.log` | shell filename pattern |

---

## 3. Daily Commands

```bash
echo "$PATH"
export APP_ENV=prod
which java
type python3
env | sort
history | tail
set -euo pipefail  # useful in scripts, with care
```

---

## 4. Quoting Rules

| Form | Behavior |
|---|---|
| unquoted | variables/globs/word splitting happen |
| single quotes | literal string |
| double quotes | variables expand, word splitting is controlled |

Use double quotes around variables in scripts:

```bash
rm -- "$target_file"
```

---

## 5. Production Failure Modes

- command works interactively but fails under systemd because PATH differs
- cron job cannot find binaries
- unquoted variable deletes or modifies wrong path
- glob expands unexpectedly
- stderr is not captured, hiding the real error
- environment variable not exported to child process

---

## 6. Interview Summary

```text
The shell is a command interpreter that expands variables and globs, sets up pipes and redirects, and launches programs. In production I check PATH, working directory, exported environment, quoting, and stdout/stderr handling when a command works manually but fails in a script, service, or cron job.
```

---

## 7. Revision Notes

- One-line summary: The shell transforms your command before Linux runs the program.
- Three keywords: expansion, environment, redirection.
- One trap: assuming interactive shell environment exists inside cron or systemd.