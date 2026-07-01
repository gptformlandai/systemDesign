# Runbook: GraphQL Authorization Data Leak

## Symptoms

- user sees another user's or tenant's data
- sensitive nested field is visible unexpectedly
- error response reveals object existence

## Evidence

- operation document and variables
- error/data path
- context user and tenant
- resolver path
- data-source filter
- loader/cache scope

## Mitigate

- disable exposed field if necessary
- patch field/object authorization
- enforce tenant filter near data access
- purge unsafe cache
- add regression test for nested selection

## Prevent

- field-level auth policy
- tenant-scoped loaders
- negative auth tests
- safe error contract