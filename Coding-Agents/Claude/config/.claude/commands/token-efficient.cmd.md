---
description: Rewrite a verbose or ineffective prompt to be compact and high-signal
---

Rewrite the following prompt to be more token-efficient and higher-signal:

$ARGUMENTS

Apply these improvements:
1. Remove courtesy preamble ("Hi, I'm working on...", "Can you help me...")
2. Replace description with file reference (use @file:path instead of describing the code)
3. Add explicit output format ("diff only", "table", "under 200 words", "1 sentence")
4. Add the most important constraint ("keep public API", "no new dependencies")
5. Remove information Claude can infer from context
6. Merge redundant phrases
7. Target: under 60 words for the full prompt

Output:
1. Rewritten prompt (complete, ready to use)
2. Word count: original → rewritten
3. What was removed and why (bullet list)
4. Any quality improvements beyond compression (clearer intent, better constraints)
