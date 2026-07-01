# Proto Design Rules Cheatsheet

## Must Remember

- Field numbers are the wire contract.
- Never reuse deleted field numbers.
- Reserve deleted field numbers and names.
- Enum zero should be `UNSPECIFIED`.
- Add fields instead of changing existing meaning.
- Use versioned packages for major incompatible changes.
- Keep business-critical data typed, not buried in generic maps.

## Safe Change Examples

- add a new field number
- add a new message
- add a new RPC method
- reserve a deleted field

## Unsafe Change Examples

- reuse a field number
- change units without a new field
- remove field without reserve
- change request/response type incompatibly
- rename packages used by generated clients without migration