# Modern Build Tools: esbuild, Rollup, Turbopack, Metro Gold Sheet

> Topic: modern tool philosophies and how they map to React, Next.js, and React Native.

---

## 1. Intuition

Modern build tools are specialized engines. Some are speed-first, some are library-output-first, some are framework-integrated, and some target native app runtimes.

Beginner version:

> Do not ask only "which tool is fastest?" Ask "which tool matches this app's runtime and output needs?"

---

## 2. Tool Philosophy Map

| Tool | Philosophy | Best Fit |
|---|---|---|
| esbuild | Extremely fast transform/bundle/minify | Tooling, scripts, dev acceleration |
| Rollup | ESM-first optimized bundling | Libraries and clean production output |
| Turbopack | Incremental framework bundling | Next.js apps |
| Expo CLI + Metro | React Native framework workflow plus Metro bundling | Expo apps |
| Metro | React Native graph and asset pipeline | iOS and Android apps |

---

## 3. esbuild

esbuild is a fast build tool written in Go.

Mental model:

```txt
source files
  -> fast parser
  -> transform TS/JSX/CSS
  -> optional bundle
  -> optional minify
  -> output
```

Example:

```js
import * as esbuild from 'esbuild';

await esbuild.build({
  entryPoints: ['src/main.tsx'],
  bundle: true,
  minify: true,
  sourcemap: true,
  outfile: 'dist/app.js',
});
```

Strengths:

- Very fast.
- Built-in TypeScript and JSX transform.
- Minification.
- Simple API.

Limits:

- Not always a full replacement for highly customized bundler ecosystems.
- TypeScript transform is not full type checking.

---

## 4. Rollup

Rollup is ESM-first and strong for library builds.

Pipeline:

```txt
ESM entry
  -> static import analysis
  -> tree shaking
  -> chunk generation
  -> ESM/CJS/UMD output
```

Example:

```js
// rollup.config.mjs
export default {
  input: 'src/index.ts',
  output: [
    { file: 'dist/index.mjs', format: 'esm' },
    { file: 'dist/index.cjs', format: 'cjs' },
  ],
  external: ['react'],
};
```

Why libraries like Rollup:

- Clean ESM output.
- Good tree shaking.
- Multiple output formats.
- Peer dependency handling.

---

## 5. Turbopack

Turbopack is a modern incremental bundler associated with Next.js.

Mental model:

```txt
Next.js route graph
  -> incremental computation
  -> cache repeated work
  -> update only affected parts
  -> support framework semantics
```

Why it exists:

- Large apps suffer when every change triggers too much rebuild work.
- Frameworks know more than generic bundlers about routes, server/client boundaries, and caching.
- Incremental architecture can improve dev and build performance.

Use it when:

- You are in a Next.js app and the framework supports your use case.
- You want framework-integrated performance improvements.

Avoid assuming:

- Turbopack is a generic drop-in replacement for every Webpack configuration.
- Every plugin ecosystem behavior maps perfectly.

---

## 6. Metro

Metro is the default React Native JavaScript bundler.

Official mental model:

```txt
Resolution
  -> find modules and assets

Transformation
  -> transpile modules for target platform

Serialization
  -> combine modules into bundle output
```

React Native example:

```txt
index.js
  -> App.tsx
  -> Button.ios.tsx or Button.android.tsx
  -> image assets
  -> JS bundle served to device
```

Metro config concerns:

- `projectRoot`
- `watchFolders`
- resolver extensions
- asset extensions
- source extensions
- transformer options
- cache stores

Monorepo example:

```js
// metro.config.js
const { getDefaultConfig } = require('@react-native/metro-config');

const config = getDefaultConfig(__dirname);

config.watchFolders = [
  // path to shared workspace package
];

module.exports = config;
```

---

## 7. Expo CLI + Metro

Expo is a React Native framework and toolchain. In bundling terms, Expo CLI orchestrates the local workflow, while Metro handles the JavaScript and asset bundle.

Development flow:

```txt
npx expo start
   |
   v
Expo CLI terminal UI
   |
   v
Metro dev server
   |
   v
Expo Go or development build connects
   |
   v
JS bundle and assets served to device
```

Build/release flow:

```txt
source code + app config
   |
   v
Expo prebuild / config plugins when native generation is needed
   |
   v
EAS Build or local native build
   |
   v
native binary + JS bundle/assets
   |
   v
app store / internal distribution / OTA update boundaries
```

Expo concepts that matter for build thinking:

| Concept | Build Meaning |
|---|---|
| Expo Go | Prebuilt runtime; fast learning loop but fixed native libraries |
| Development build | Your native debug app; better production parity |
| `expo-dev-client` | Adds development launcher and tooling to your custom app |
| CNG / prebuild | Generates native `ios` and `android` projects from app config/plugins |
| EAS Build | Cloud build service for native binaries |
| EAS Update | OTA JavaScript/assets updates within native binary compatibility |
| Expo Router | File-based routing that affects app organization, not a replacement for Metro |

Common Expo build mistakes:

- Installing a native library and expecting Expo Go to contain its native code.
- Forgetting that OTA updates cannot add new native modules or permissions.
- Editing native config without regenerating/rebuilding the native app.
- Debugging a production-only native issue only inside Expo Go.
- Treating `npx expo start` logs as only "terminal noise" instead of a pipeline timeline.

Strong interview answer:

> Expo improves the React Native product workflow, but it still depends on Metro for bundling. I use Expo Go for learning and simple SDK-compatible work, a development build for production-grade apps with native modules, EAS Build for native binaries, and EAS Update only for compatible JavaScript and asset changes.

---

## 8. Comparison By Output

| Need | Best Tool Direction |
|---|---|
| React SPA dev speed | Vite |
| Existing enterprise custom app | Webpack |
| Low-config web app | Parcel |
| React component library | Rollup or esbuild wrapper |
| CLI/tooling build speed | esbuild |
| Next.js framework app | Next.js compiler/Turbopack/Webpack path |
| Expo React Native app | Expo CLI + Metro, with EAS for builds/updates |
| React Native app | Metro |
| Micro-frontend with Module Federation | Webpack ecosystem |

---

## 9. Real-World Examples

### Design System Package

```txt
source TS components
  -> Rollup build
  -> emit ESM + CJS + types
  -> mark React as peer dependency
```

### Next.js SaaS App

```txt
app routes
  -> Next compiler
  -> server/client chunks
  -> route-level output
  -> deploy to Vercel or Node/edge platform
```

### React Native Mobile App

```txt
index.js
  -> Metro
  -> platform-specific transform
  -> Hermes bytecode or JS bundle
  -> app startup on device
```

### Expo Production App

```txt
app config + source code
  -> Expo CLI starts Metro in dev
  -> development build provides native runtime
  -> EAS Build creates native binary
  -> EAS Update ships compatible JS/assets
```

---

## 10. Common Mistakes

### Mistake: Choosing tools only by benchmark

- Why wrong: Runtime compatibility, plugin ecosystem, framework integration, and team knowledge matter.
- Better approach: choose based on project constraints.

### Mistake: Using app bundling strategy for libraries

- Why wrong: Libraries need external peer dependencies and multiple module outputs.
- Better approach: use Rollup or library-focused tooling.

### Mistake: Trying to force Vite into React Native native bundling

- Why wrong: Metro understands React Native platform resolution and native app workflow.
- Better approach: use Metro for native, Vite for web companion surfaces.

### Mistake: Thinking Expo is a separate bundler from Metro

- Why wrong: Expo CLI uses Metro for React Native JavaScript and asset bundling.
- Better approach: think "Expo workflow, Metro bundler."

### Mistake: Assuming Turbopack equals Webpack config compatibility

- Why wrong: Different architecture and plugin model.
- Better approach: verify framework support before migration.

---

## 11. Trade-Offs

| Tool | Gain | Cost |
|---|---|---|
| esbuild | Speed | Less ecosystem depth than Webpack |
| Rollup | Excellent library output | App dev server story usually needs surrounding tooling |
| Turbopack | Framework-aware incremental work | Best understood inside Next.js constraints |
| Expo CLI + Metro | Productive RN workflow and EAS integration | Native parity requires development builds |
| Metro | Native platform fit | Not a general-purpose web bundler |

---

## 12. Interview Insight

Strong answer:

> Modern build tools differ by philosophy. esbuild optimizes for raw speed, Rollup for ESM and library output, Turbopack for incremental Next.js builds, Expo CLI orchestrates React Native app workflow, and Metro performs React Native platform bundling. The right choice depends on runtime, output format, team constraints, and framework integration, not only benchmark speed.

Follow-up trap:

> Can we use the same bundler for React web, Next.js, and React Native?

Good answer:

> Not usually. React web can use Vite or Webpack, Next.js needs framework-aware compilation for routing and rendering, and React Native uses Metro for platform resolution and native bundle delivery.

Expo follow-up:

> Where does Expo fit in the bundling story?

Good answer:

> Expo is the framework/toolchain around React Native development and release. It starts Metro during development, manages config and native generation when needed, supports development builds for production-like native code, and uses EAS for builds and OTA updates. Metro remains the bundler for the JavaScript and assets.

---

## 13. Revision Notes

- One-line summary: Modern build tools are specialized around speed, output quality, framework integration, native runtime needs, or Expo's RN workflow.
- Three keywords: speed, output, runtime.
- One interview trap: Expo is not a replacement for Metro; it orchestrates a Metro-based React Native workflow.
- Memory trick: Pick the engine for the road, and Expo for the RN travel kit.
