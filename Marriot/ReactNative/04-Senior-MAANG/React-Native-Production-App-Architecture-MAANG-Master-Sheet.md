# React Native Production App Architecture - MAANG Master Sheet

> Track File #13 of 20 - Group 4: Senior MAANG
> Level: system design for mobile applications

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Mobile architecture layers | Very high | Senior design clarity |
| Feature flags | High | Safe rollout |
| API compatibility | High | Old app versions persist |
| Design system | High | Team scale |
| Accessibility | Very high | Product quality and compliance |
| Internationalization | High | Global apps |
| Monorepo/module boundaries | Medium | Large org pattern |
| Analytics and experimentation | High | Product iteration |

MAANG signal:
You can design the app as a long-lived product platform, not just a set of screens.

---

## 2. Reference Architecture

```text
React Native App
  App shell
    providers
    navigation
    error boundary
    telemetry
  Design system
    tokens
    primitives
    accessibility defaults
  Features
    auth
    feed
    search
    checkout
    profile
  Core services
    api client
    auth/session
    storage
    config
    feature flags
    logging
  Native/platform layer
    permissions
    notifications
    secure storage
    device APIs
```

Dependency direction:

```text
feature -> core abstractions
feature -> design system
core -> no feature imports
design system -> no product/business imports
native wrapper -> stable JS interface
```

---

## 3. App Shell Responsibilities

App shell owns:
- root providers
- navigation container/router
- auth restoration
- crash/error boundary
- theme provider
- query/cache provider
- feature flag initialization
- telemetry initialization
- deep link handling
- app state listener

App shell should not own:
- product-specific business logic
- screen-specific API calls
- random feature state

---

## 4. Backend Compatibility

Mobile app versions live for a long time.

Backend must handle:
- old app versions
- phased rollout
- users who disabled auto-update
- API field additions/removals
- feature flags by version
- forced upgrade for unsupported versions

Mobile app must handle:
- missing optional fields
- unknown enum values
- server maintenance
- incompatible feature disabled by config

Interview answer:

```text
Unlike web, we cannot assume all clients update instantly. I design APIs to be
backward compatible, use additive changes, support old app versions for a defined
window, and use feature flags or minimum-version gates for risky capabilities.
```

---

## 5. Feature Flags And Experimentation

Feature flags support:
- staged rollout
- kill switches
- A/B tests
- permissioned beta
- server-driven config
- emergency disable

Rules:
- Fetch flags early but avoid blocking first paint unnecessarily.
- Cache last-known flags.
- Define default behavior if flag fetch fails.
- Include app version/platform in evaluation.
- Keep flags typed.
- Remove stale flags.

Typed flag:

```ts
type FeatureFlagKey =
  | 'new_checkout'
  | 'feed_video_autoplay'
  | 'offline_drafts';

function isEnabled(flag: FeatureFlagKey): boolean {
  return featureFlagStore.get(flag) === true;
}
```

---

## 6. Accessibility

Accessibility is production functionality, not polish.

Checklist:
- meaningful labels
- correct roles
- readable contrast
- dynamic text support
- screen reader order
- touch target size
- no color-only state
- reduced motion support where needed
- focus management for modals

Example:

```tsx
<Pressable
  accessibilityRole="button"
  accessibilityLabel="Add product to cart"
  accessibilityHint="Adds this item to your shopping cart"
  onPress={onAddToCart}
>
  <Text>Add to cart</Text>
</Pressable>
```

Interview point:
Accessibility should be part of component primitives so every feature gets it by default.

---

## 7. Internationalization

Consider:
- text expansion
- right-to-left layouts
- pluralization
- date/time/number/currency formatting
- locale-specific legal copy
- images with embedded text
- server-driven translations

Bad:

```tsx
<Text>{count + ' items'}</Text>
```

Better:

```tsx
<Text>{t('cart.itemCount', {count})}</Text>
```

Production rule:
Do not concatenate translated strings.

---

## 8. Mobile System Design Scenario: Social Feed App

Requirements:
- infinite feed
- pull to refresh
- like/comment/share
- image/video media
- offline read cache
- notifications
- deep links to posts
- experiments for ranking UI

Architecture:

```text
FeedScreen
  useFeedQuery(cursor)
  FlatList/FlashList
  memoized FeedCard
  media component with cache
  optimistic like mutation
  deep link post route
  telemetry: feed_load_time, scroll_depth, like_latency
```

Trade-offs:
- Optimistic likes are okay with rollback.
- Comments should show pending state and retry.
- Video autoplay should be feature-flagged and device/network-aware.
- Feed cache improves cold start but can show stale data.
- Media prefetch improves UX but costs bandwidth/storage.

---

## 9. Common Mistakes

| Mistake | Why It Is Wrong | Better Approach |
|---|---|---|
| App shell contains feature logic | Hard to scale | Keep shell infrastructure-only |
| No app-version compatibility plan | Old clients break | Add versioned contracts/gates |
| Flags as random strings | Runtime typo bugs | Typed flags |
| Accessibility only at QA end | Expensive rework | Build into components |
| Ignoring i18n until launch | Layout and copy break | Design for expansion early |
| No kill switch | Risky rollout | Add remote disable path |

---

## 10. Strong Interview Answer

Question:
How would you architect a React Native app for millions of users?

Strong answer:

```text
I would split the app into an app shell, design system, feature modules, core
services, and native wrappers. The app shell owns providers, navigation, auth
restore, feature flags, deep links, telemetry, and error boundaries. Features own
screens, hooks, components, and API mappers. Core services own API, storage,
logging, and session abstractions. I would design for old app versions, staged
rollouts, kill switches, accessibility, localization, offline states, and release
observability. Performance work would focus on startup, lists, images, JS/UI FPS,
and memory on realistic devices.
```

---

## 11. Revision Notes

- One-line summary: A production RN app is a mobile product platform with app shell, features, core services, and native wrappers.
- Three keywords: app shell, compatibility, feature flags.
- One interview trap: Web-style instant rollback assumptions do not hold for native apps.
- One memory trick: Mobile architecture must support old versions, weak networks, and real devices.

