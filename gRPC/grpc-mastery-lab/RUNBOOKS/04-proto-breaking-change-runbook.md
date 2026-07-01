# Runbook: Proto Breaking Change

## Symptoms

- clients receive wrong data
- generated clients fail to compile
- `UNIMPLEMENTED` after package/method change
- business metrics regress with `OK` status

## Checks

1. Compare proto diff against last release.
2. Look for field number reuse.
3. Look for deleted fields without reserve.
4. Check enum changes.
5. Compile old client against new server behavior if possible.
6. Review Buf/proto breaking check results.
7. Confirm generated artifact versions.

## Mitigations

- roll back schema/server release
- restore old field semantics
- add compatibility shim
- publish fixed generated artifacts
- notify client owners

## Prevention

Breaking-change gates, schema owners, golden compatibility tests, and reserved fields.