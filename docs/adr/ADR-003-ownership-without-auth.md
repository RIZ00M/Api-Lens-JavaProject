# ADR-003: Model API ownership before implementing authentication

## Status
Accepted

## Context
The product spec (section 26) explicitly defers full authentication
(JWT/OAuth2) past the MVP, but also requires that "every API should belong
to a user" so ownership doesn't need to be retrofitted later — and so
authorization checks (a user can only see/modify their own APIs) are
exercised from Phase 2 onward instead of being bolted on afterward.

## Decision
Introduce a minimal `users` table and a real `owner_id` foreign key on
`apis` in Phase 2. Seed a single fixed "system user"
(`00000000-0000-0000-0000-000000000001`) via Flyway migration `V2`, and
have `ApiManagementService.currentUser()` resolve to that user for every
request. All read/write operations already filter and check ownership by
this id.

## Consequences
- Positive: the ownership check (`ApiManagementService.getApi`) exists and
  is tested now, so switching from the fixed system user to a real
  authenticated principal is a one-method change, not a schema migration
  plus a rewrite of every query.
- Positive: `apis.owner_id` is `NOT NULL` from the start, avoiding a
  nullable-then-backfilled-then-NOT-NULL migration sequence later.
- Trade-off: until real authentication exists, every API in a given
  deployment is effectively visible to whoever can reach the backend —
  acceptable for local/portfolio use, but called out explicitly in the
  README so it isn't mistaken for a finished security model.
