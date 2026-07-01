# X01. Cross-IDE Debugging Mental Model: Breakpoints, State, Call Stack

> Bridge module for IntelliJ, VS Code, and PyCharm.
> Use this when you want the common debugging workflow independent of language or IDE.

---

## 1. Core Idea

All debuggers answer the same three questions:

```text
Where am I?     -> current line, stack frame, thread, coroutine, or callback
How did I get here? -> call stack, async stack, request path, event chain
What is true now?   -> variables, heap objects, env, config, locks, pending tasks
```

IDE buttons differ, but the mental model is stable:

```text
reproduce -> pause -> inspect -> step -> compare expected vs actual -> verify fix
```

---

## 2. Universal Debugger Vocabulary

| Concept | IntelliJ | VS Code | PyCharm |
|---|---|---|---|
| Debug config | Run/Debug Configuration | `launch.json` / Run and Debug | Run/Debug Configuration |
| Pause point | Breakpoint | Breakpoint | Breakpoint |
| Current execution | Frames + Variables | Call Stack + Variables | Frames + Variables |
| Expression check | Evaluate Expression | Debug Console / Watch | Evaluate Expression |
| Pinned variable | Watches | Watch | Watches |
| Thread view | Threads tab | Call Stack by thread/process | Threads/Frames |
| Remote attach | JDWP attach | Node/Python attach | pydevd/debugpy attach |

---

## 3. Breakpoint Selection Map

| Bug Symptom | Best Breakpoint |
|---|---|
| Value becomes wrong somewhere | field watchpoint, data breakpoint, or conditional breakpoint |
| Exception stack trace points too late | exception breakpoint at throw site |
| Loop fails only after many iterations | hit-count or conditional breakpoint |
| Race condition disappears when paused | log breakpoint or non-suspending breakpoint |
| Async code loses context | breakpoint at await/callback boundary plus async stack |
| Remote service behaves differently | attach debugger with same env/profile/config |
| Only one user/request fails | conditional breakpoint on request id/user id/correlation id |

---

## 4. Step Controls

| Action | Meaning | Use When |
|---|---|---|
| Step Over | run current line without entering called function | current line calls known-good code |
| Step Into | enter the called function | you suspect the called function |
| Force Step Into | enter framework/library code | framework behavior matters |
| Step Out | finish current function and return to caller | you stepped too deep |
| Run To Cursor | continue until selected line | skip boring setup code |
| Resume | continue until next breakpoint | you have enough evidence for now |
| Pause | interrupt running program | app is hanging or spinning |

Strong habit:

```text
Step slowly at boundaries, not through every line.
Boundaries: controller -> service, service -> repository, handler -> async task, request -> worker.
```

---

## 5. Call Stack Reading

Read from top to bottom:

```text
top frame    = code executing right now
middle frame = caller context
bottom frame = entry point, thread start, request handler, test runner, event loop
```

Debugging pattern:

1. Look at the current line.
2. Read the method/function name.
3. Move one frame up.
4. Ask what input was passed down.
5. Compare caller expectation vs callee behavior.

Common trap:

```text
Only inspecting the top frame misses the reason the wrong input arrived.
```

---

## 6. Variable Inspection Rules

| Variable Type | What To Check |
|---|---|
| Local variable | current function logic |
| Object field | object lifecycle and mutation |
| Static/global | shared mutable state risk |
| Closure variable | async callback captured stale value |
| Environment variable | wrong profile, key, port, feature flag |
| Request context | correlation id, tenant id, auth principal |
| Thread-local/context-local | lost context across threads/coroutines |

Use watches for values you repeatedly compare:

```text
request.id
user.role
order.status
Thread.currentThread().getName()
process.env.NODE_ENV
asyncio.current_task()
```

---

## 7. Debugging Without Changing Timing

Some bugs disappear when you pause because timing changes.

Use this for timing-sensitive issues:

- log breakpoints instead of suspending
- conditional breakpoints with narrow conditions
- thread dumps instead of line stepping
- async stack traces
- profiler snapshots
- repeated samples over time

Rule:

```text
If pausing changes the bug, observe first and suspend later.
```

---

## 8. Cross-IDE Shortcut Core

Shortcuts vary by keymap, but the first set to memorize is stable:

| Action | Typical JetBrains Keymap | Typical VS Code Key |
|---|---|---|
| Resume | F9 | F5 |
| Step Over | F8 | F10 |
| Step Into | F7 | F11 |
| Step Out | Shift+F8 | Shift+F11 |
| Evaluate | Alt+F8 | Debug Console / Watch |
| Toggle breakpoint | Cmd+F8 or Ctrl+F8 | F9 |

Interview-safe phrasing:

```text
I rely on the debugger concepts more than exact key bindings: resume, step over, step into, step out, evaluate, watch, and thread view.
```

---

## 9. Strong Debugging Answer

```text
I first reproduce the issue with a stable input. Then I choose a breakpoint based on the symptom: exception breakpoint for thrown errors, conditional breakpoint for a specific request, watchpoint for unexpected mutation, or log breakpoint if timing matters. Once paused, I inspect the current frame, walk up the call stack, compare actual variables to expected invariants, and step only across important boundaries. For concurrency or async issues, I inspect threads, tasks, promises, or dumps instead of blindly stepping line by line.
```

---

## 10. Revision Notes

- One-line summary: IDEs differ, but debugging always means pause, inspect state, read stack, test a theory, and verify.
- Three keywords: breakpoint, frame, state.
- One trap: stepping line by line without a hypothesis.
- Memory trick: `P-S-C` = Pause, State, Call stack.
