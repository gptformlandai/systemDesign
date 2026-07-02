# React Native App Store Compliance, Privacy, And Metadata - Gold Sheet

> Track Module - Group 6: Gold-Level Completeness
> Level: production release compliance and store-readiness

---

## 1. Interview Priority Meter

| Topic | Frequency | Why It Matters |
|---|---|---|
| Permission copy | Very high | Bad prompts cause denial and review risk |
| Apple privacy declarations | High | Required for store review and user trust |
| Google Play Data Safety | High | Required for Play publication |
| SDK inventory | High | Third-party SDKs collect data too |
| ATT/tracking consent | Medium-high | Ads/attribution compliance |
| Store rejection playbook | Medium-high | Senior release ownership |

MAANG signal:
You understand that mobile release is product, engineering, privacy, legal, and operations working together.

---

## 2. Mental Model

Store compliance maps actual app behavior to public declarations.

```text
Code and SDK behavior
  -> permissions and data collection
  -> privacy labels / Data Safety / manifests
  -> metadata and review notes
  -> app store review
  -> production monitoring and change control
```

Rule:
The store declaration must match what the app and included SDKs actually do.

---

## 3. Data Inventory

Create a table before release:

| Data | Collected? | Shared? | Purpose | Required? | SDK/Owner |
|---|---:|---:|---|---:|---|
| Email | Yes | No | account login | Yes | auth |
| Precise location | Optional | No | nearby tasks | No | maps/location |
| Crash logs | Yes | Yes | diagnostics | Yes | crash SDK |
| Analytics events | Yes | Yes | product analytics | Optional | analytics SDK |
| Payment token | Yes | Yes | checkout | Yes | payment SDK/backend |

Include:
- app code
- backend calls from app
- analytics SDKs
- crash SDKs
- ads/attribution SDKs
- payment SDKs
- embedded webviews controlled by the app

---

## 4. Permission Copy

Bad:

```text
Allow camera access.
```

Better:

```text
Allow camera access so you can scan receipts for expense reports.
```

Rules:
- Explain the user value.
- Ask at the moment of need.
- Use narrow permission if possible.
- Provide fallback if denied.
- Keep Info.plist/Android copy aligned with actual feature behavior.

---

## 5. Apple Privacy Areas

For iOS releases, track:
- App Store privacy nutrition labels.
- Privacy policy URL.
- App Tracking Transparency if tracking users across apps/sites.
- privacy manifests for app/SDK data practices where applicable.
- required reason API declarations when using covered APIs.
- permissions and entitlements matching real use.

Review checklist:

```text
1. Inventory app and third-party SDK data collection.
2. Confirm whether data is linked to user identity.
3. Confirm whether data is used for tracking.
4. Review required reason APIs.
5. Verify purpose strings.
6. Confirm privacy policy URL.
7. Test permission prompts on device.
```

---

## 6. Google Play Data Safety

For Android releases, track:
- data collected
- data shared
- whether collection is optional or required
- security practices such as encryption in transit
- user deletion request mechanism
- privacy policy
- third-party SDK data behavior
- permissions and sensitive API use

Important:
Even apps that collect no user data may still need to complete required store forms and provide policy information depending on distribution context.

---

## 7. SDK Inventory

Every native SDK should have an owner.

| SDK | Purpose | Data Collected | Store Declaration Needed | Owner |
|---|---|---|---|---|
| Crash SDK | diagnostics | device, crash, breadcrumbs | yes | platform |
| Analytics SDK | product metrics | events, user id | yes | product analytics |
| Payment SDK | checkout | payment token | yes | payments |
| Maps SDK | map/location | location/query | maybe | location team |

Release gate:
No new SDK enters production without data inventory, privacy review, version pinning, and rollback plan.

---

## 8. Metadata As Code

Metadata includes:
- app name/subtitle
- description
- keywords
- screenshots
- privacy policy URL
- support URL
- release notes
- review notes
- category
- age rating

For Expo/EAS teams, EAS Metadata can help manage app store metadata in source control.

Example concept:

```json
{
  "configVersion": 0,
  "apple": {
    "info": {
      "en-US": {
        "title": "MasteryApp",
        "subtitle": "Field work made reliable",
        "privacyPolicyUrl": "https://example.com/privacy"
      }
    }
  }
}
```

Do not store private credentials in metadata files.

---

## 9. Store Rejection Playbook

When rejected:

```text
1. Read the exact rejection reason.
2. Reproduce reviewer's path on a clean device/account.
3. Identify whether issue is code, metadata, permission, account, or policy.
4. Fix smallest compliant issue.
5. Add review notes/screenshots/test account if needed.
6. Resubmit through the correct track.
7. Add a checklist item so it does not repeat.
```

Common rejection causes:
- missing demo credentials
- permission purpose unclear
- broken login
- hidden features unavailable to reviewer
- privacy declaration mismatch
- payment policy violation
- background location justification weak

---

## 10. Release Compliance Checklist

Before production:

```text
[ ] Data inventory reviewed
[ ] Permission strings reviewed on real device
[ ] Privacy policy URL live
[ ] Apple privacy labels/manifests reviewed
[ ] Google Play Data Safety reviewed
[ ] SDK inventory reviewed
[ ] ATT/tracking decision documented
[ ] App metadata and screenshots updated
[ ] Review account prepared if needed
[ ] Store notes explain sensitive capabilities
[ ] Crash/source maps/symbols upload verified
```

---

## 11. Strong Interview Answer

```text
For mobile compliance I start with a data and SDK inventory. I map what the app
and third-party SDKs collect or share to App Store privacy declarations, Google
Play Data Safety, permission copy, privacy policy, and review notes. I ask for
permissions at the moment of need, keep copy tied to user value, and treat store
metadata as part of the release gate. If a rejection happens, I reproduce the
review path, fix the smallest compliant issue, and add it to the release checklist.
```

---

## 12. Revision Notes

- One-line summary: Store compliance is matching real app behavior to public declarations.
- Three keywords: inventory, permission copy, metadata.
- One interview trap: Ignoring data collected by third-party SDKs.
- Memory trick: Code behavior -> data inventory -> store declaration -> review.
