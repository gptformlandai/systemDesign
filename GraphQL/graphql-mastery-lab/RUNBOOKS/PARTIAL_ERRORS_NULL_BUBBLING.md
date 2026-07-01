# Runbook: Partial Errors And Null Bubbling

## Symptoms

- response has both `data` and `errors`
- parent object unexpectedly null
- client UI loses a section after one field fails

## Evidence

- error path
- field nullability
- resolver exception
- upstream dependency status
- client error policy

## Mitigate

- restore failing dependency
- degrade optional field
- adjust resolver fallback
- revisit nullability only through schema governance
- update client handling for partial data

## Prevent

- stable error codes
- intentional nullability reviews
- resolver fallback policy
- client partial-data tests