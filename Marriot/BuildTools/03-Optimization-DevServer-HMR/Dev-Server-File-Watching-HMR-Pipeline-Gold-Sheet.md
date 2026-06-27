# Dev Server: npm run dev, File Watching, HMR Pipeline Gold Sheet

> Topic: what happens when `npm run dev` runs.

---

## 1. Intuition

A dev server is a local development assistant. It serves your app, watches files, transforms changed modules, and tells the browser what changed.

Beginner version:

> `npm run dev` starts a local server that keeps your browser connected to your source code changes.

---

## 2. Definition

- Definition: A development server serves app modules locally and coordinates rebuilds, reloads, or HMR updates when files change.
- Category: Local development infrastructure.
- Core idea: Fast feedback loop over perfect production optimization.

---

## 3. What Happens When `npm run dev` Runs

Package script:

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0"
  }
}
```

Execution flow:

```txt
npm run dev
   |
   v
npm reads package.json
   |
   v
starts tool command
   |
   v
tool loads config
   |
   v
starts local HTTP server
   |
   v
initial transform or graph setup
   |
   v
browser opens app
   |
   v
file watcher waits for changes
```

For Next.js:

```txt
npm run dev
  -> next dev
  -> framework dev server
  -> route compilation on demand
```

For React Native:

```txt
npm start
  -> Metro dev server
  -> app requests JS bundle from Metro
```

---

## 4. Local Dev Server Architecture

```txt
Browser
  | HTTP requests modules/assets
  | WebSocket receives updates
  v
Dev server
  | resolves imports
  | transforms files
  | tracks module graph
  | watches filesystem
  v
Source files
```

Common components:

- HTTP server.
- Module resolver.
- Transform pipeline.
- In-memory cache.
- File watcher.
- WebSocket connection.
- Error overlay.
- HMR runtime client.

---

## 5. File Watching

File watching detects changes.

```txt
save file
  -> OS emits file change event
  -> watcher receives event
  -> dev server invalidates cache
  -> graph is updated
  -> browser gets update
```

Common watcher problems:

- Monorepo files outside project root are not watched.
- Docker or network filesystem misses events.
- Too many watched files can slow the machine.
- Generated files trigger repeated rebuild loops.

---

## 6. Dev Server Pipeline

```txt
Request /src/App.tsx
      |
      v
Resolve file path
      |
      v
Transform TSX -> JS
      |
      v
Rewrite imports if needed
      |
      v
Send module to browser
      |
      v
Cache result
```

In Vite-style native ESM dev:

```txt
browser requests modules individually
dev server transforms on demand
```

In older Webpack-style dev:

```txt
bundler builds graph before serving bundle/chunks
```

---

## 7. Error Overlay

When compilation fails:

```txt
source save
  -> transform error
  -> server sends error payload
  -> browser displays overlay
  -> fix file
  -> overlay disappears after successful update
```

Example error:

```txt
Unexpected token
  src/App.tsx:12:8
```

Source maps and transform diagnostics make the error point back to the original file.

---

## 8. React, Next.js, React Native Notes

### React Web

The dev server usually serves HTML, source modules, transformed JS, CSS updates, and HMR payloads.

### Next.js

The dev server compiles routes on demand and coordinates server/client output. A change to a Server Component may trigger a different update path than a Client Component.

### React Native

Metro serves the bundle to an emulator or device. Fast Refresh updates React components while trying to preserve state. Watch configuration is important in monorepos.

---

## 9. Expo Dev Server Flow

Expo sits on top of React Native and Metro. For most Expo projects, the local development command is:

```bash
npx expo start
```

Internal flow:

```txt
npx expo start
   |
   v
Expo CLI starts
   |
   v
Metro dev server starts, commonly on localhost:8081
   |
   v
Terminal UI shows QR code and shortcuts
   |
   v
Launch target chosen
  Expo Go or development build
   |
   v
Device/simulator connects to dev server
   |
   v
Metro serves manifest, JS bundle, and assets
   |
   v
App runs inside Expo Go or your development client
```

Launch target decision:

| Target | Meaning | Use When |
|---|---|---|
| Expo Go | Prebuilt Expo app with fixed native libraries | Learning, simple SDK-compatible apps |
| Development build | Your own debug native app with `expo-dev-client` | Production-grade apps, custom native modules, realistic testing |
| Bare React Native | Native projects owned directly | Maximum native control |

Useful Expo Terminal UI shortcuts:

| Shortcut | Meaning |
|---|---|
| `A` | open Android |
| `I` | open iOS Simulator |
| `W` | open web |
| `R` | reload connected app |
| `S` | switch Expo Go vs development build target |
| `M` | open native dev menu |
| `J` | open React Native DevTools for Hermes app |
| `?` | show terminal commands |

Interview insight:

> Expo does not replace Metro. Expo CLI orchestrates the developer workflow, while Metro still resolves, transforms, and serves the React Native JavaScript bundle and assets.

---

## 10. Reading Frontend Startup Logs

When a frontend app starts, read logs as a timeline, not as random noise.

```txt
command invoked
   |
   v
tool config loaded
   |
   v
server port and URL printed
   |
   v
initial graph/dependency work
   |
   v
device/browser connects
   |
   v
bundle or module request happens
   |
   v
compile/bundle success or failure
   |
   v
runtime console logs/errors
```

### React Web Startup Logs

Example signals:

```txt
VITE ready in 420 ms
Local: http://localhost:5173/
Network: http://192.168.1.10:5173/
```

How to read:

- `ready in ... ms`: dev server started.
- `Local`: browser on same machine can open this URL.
- `Network`: another device on same network can open this URL.
- `Module not found`: resolver stage failed.
- `Unexpected token`: transform stage failed.
- Runtime error overlay: app code executed and crashed.

### Next.js Startup Logs

Example signals:

```txt
ready started server on 0.0.0.0:3000
compiled /dashboard in 1.8s
```

How to read:

- Server ready means the framework dev server started.
- Route compile logs show on-demand compilation.
- Error before ready usually means config/dependency failure.
- Error after route compile usually means route code, server/client boundary, or runtime problem.

### Expo / React Native Startup Logs

Example signals:

```txt
npx expo start
Metro waiting on exp://192.168.1.10:8081
Press a | open Android
Press i | open iOS simulator
Bundling index.js
Bundled 1420ms index.js
LOG App mounted
```

How to read:

- `Metro waiting`: dev server is alive and waiting for a client.
- QR code / `exp://` URL: device connection endpoint.
- `Opening on Android/iOS`: CLI requested simulator/device launch.
- `Bundling index.js`: device requested the JS bundle.
- `Bundled ...`: transform and serialization completed.
- `LOG`, `WARN`, `ERROR`: JavaScript runtime logs from the app.
- Red screen / LogBox: app code or runtime warning surfaced on device.
- No JS logs and app closes: check native logs through Xcode or Android Logcat.

Startup debugging checklist:

```txt
1. Did the command start the correct tool?
2. Did it bind to the expected port?
3. Did the browser/device connect?
4. Did module resolution begin?
5. Did transformation succeed?
6. Did the bundle/module reach the runtime?
7. Did the app crash before first render?
8. Are errors JS-level, framework-level, or native-level?
```

Common mistakes:

- Reading only the final error instead of the first error.
- Ignoring whether the app connected to the dev server.
- Confusing bundler errors with runtime errors.
- Looking only at Metro logs when the failure is native startup.
- Leaving noisy `console.log` statements in release builds.

---

## 11. Tool Comparison Lens

| Tool | Dev Server Style |
|---|---|
| Webpack dev server | Builds and serves bundle/chunks from memory |
| Vite dev server | Serves native ESM modules on demand |
| Parcel dev server | Low-config dev server with automatic transforms |
| Next.js dev server | Framework route compilation and server/client coordination |
| Expo CLI + Metro | Starts Expo workflow, chooses target, serves RN bundle/assets |
| Metro | Serves React Native bundles to emulator/device |

---

## 12. Real-World Example

You save `CheckoutForm.tsx`.

```txt
watch event
  -> dev server invalidates CheckoutForm module
  -> transforms TSX to JS
  -> checks HMR boundary
  -> sends WebSocket update
  -> browser replaces module
  -> React re-renders checkout UI
```

If the update cannot be safely applied:

```txt
HMR rejected
  -> full page reload
```

---

## 13. Common Mistakes

### Mistake: Believing dev server performance predicts production performance

- Why wrong: Dev mode often skips minification, full tree shaking, and final chunking.
- Better approach: Measure both dev feedback speed and production output.

### Mistake: Ignoring file watcher configuration in monorepos

- Why wrong: Shared packages may not trigger updates.
- Better approach: Configure workspace roots, aliases, and watch folders.

### Mistake: Debugging stale cache by changing random code

- Why wrong: Dev servers cache transformed modules aggressively.
- Better approach: clear the tool cache or restart the dev server when graph state is suspicious.

### Mistake: Running dev server with production assumptions

- Why wrong: Dev serves unoptimized code and may use different module boundaries.
- Better approach: Run `build` and preview before shipping.

### Mistake: Treating Expo Go as production parity

- Why wrong: Expo Go is a prebuilt runtime with a fixed set of native libraries.
- Better approach: use a development build when native modules, app icon/splash, push notifications, deep links, or release parity matter.

### Mistake: Ignoring the first startup log line that failed

- Why wrong: Later errors are often symptoms of the first resolver, transform, or runtime failure.
- Better approach: read startup logs top-to-bottom and identify the pipeline stage.

---

## 14. Trade-Offs

| Dev Server Design | Benefit | Cost |
|---|---|---|
| Prebuild graph | Predictable graph state | Slow startup for large apps |
| On-demand modules | Fast startup | More per-request transform behavior |
| Aggressive cache | Fast repeated updates | Stale cache bugs |
| Broad file watching | Catches monorepo changes | Higher CPU/file descriptor use |
| HMR | Preserves state and speed | Boundary complexity |
| Expo Go | Fastest learning loop | Limited native parity |
| Expo development build | Production-like native runtime | Requires building/installing client |

---

## 15. Interview Insight

Strong answer:

> `npm run dev` starts the configured dev command, which loads build config, starts an HTTP server, watches files, transforms modules, and connects to the browser through a client runtime. On changes, it invalidates affected modules and either sends an HMR update or triggers a full reload.

Follow-up trap:

> Why can a dev server start quickly but production build still be slow?

Good answer:

> Dev can transform only what the browser requests and skip deep optimizations. Production builds usually analyze the full graph, split chunks, minify, hash, and emit final assets.

Expo follow-up:

> What do you check when an Expo app starts but does not show the first screen?

Good answer:

> I read the terminal logs in order: did Expo CLI start, did Metro bind to the port, did the device connect, did bundling start, did bundling finish, and did JS runtime logs appear? If the app exits before JS logs, I check native logs. If bundling fails, I inspect resolver/transform errors. If bundling succeeds but UI is blank, I check runtime errors, LogBox, and React Native DevTools.

---

## 16. Revision Notes

- One-line summary: A dev server is the fast local feedback loop between source files and runtime.
- Three keywords: serve, watch, logs.
- One interview trap: Dev mode is not a production performance proxy, and Expo Go is not full native parity.
- Memory trick: Startup logs are the app's flight recorder.
