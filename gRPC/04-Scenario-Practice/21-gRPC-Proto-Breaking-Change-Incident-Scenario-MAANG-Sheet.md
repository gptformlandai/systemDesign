# 21. Scenario: Proto Breaking Change Incident

## Incident

New server release causes older clients to misread account status. No transport errors appear; RPC status is `OK`.

---

## Likely Root Cause

A protobuf field number, enum value, or semantic meaning changed incompatibly. Because protobuf decoding still succeeds, the application sees wrong data instead of a clean RPC failure.

---

## Evidence Path

| Evidence | What To Check |
|---|---|
| proto diff | field number reuse, type change, enum changes |
| generated code diff | changed generated package or field mapping |
| old client fixture | decode new response with old client |
| Buf breaking output | whether CI gate missed or was bypassed |
| release timeline | when server and clients changed |
| metrics | client errors or business anomaly after release |

---

## Dangerous Change Examples

```proto
// Bad: old clients may interpret field 3 as the old meaning.
message Account {
  string account_id = 1;
  string display_name = 2;
  string risk_tier = 3;
}
```

If field `3` used to mean `account_status`, this is unsafe. Delete and reserve old fields, then add a new number.

---

## Mitigation

- roll back server contract change
- add compatibility shim if rollback is impossible
- restore old field semantics
- publish fixed proto and generated artifacts
- notify client owners
- add regression fixture proving old client compatibility

---

## Prevention

- require breaking-change checks in CI
- reserve deleted field numbers and names
- review enum changes carefully
- keep golden binary compatibility fixtures for critical messages
- document schema ownership
- block manual bypass of proto gates

---

## Interview Sound Bite

Proto breaking changes can be worse than obvious failures because RPC status can remain `OK` while old clients decode wrong meaning. I inspect field numbers and enum evolution, reproduce with an old client fixture, roll back or shim, and enforce automated breaking checks.