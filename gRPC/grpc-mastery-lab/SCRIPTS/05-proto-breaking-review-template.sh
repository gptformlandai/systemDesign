#!/usr/bin/env bash
set -euo pipefail

cat <<'TEMPLATE'
# Proto Breaking Change Review

Package/service:
Change owner:
Client owners notified:

## Check
- [ ] No field numbers reused.
- [ ] Deleted fields reserve numbers and names.
- [ ] Field types and units are compatible.
- [ ] Enum zero remains UNSPECIFIED.
- [ ] New enum values are tolerated by clients.
- [ ] Method request/response shapes remain compatible.
- [ ] Generated artifacts were regenerated.
- [ ] Buf/proto breaking check passed.

## Risk

## Rollout Plan

## Rollback Plan
TEMPLATE