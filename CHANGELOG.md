# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog
and this project adheres to Semantic Versioning.

---

## [0.1.0] - 2026-05-04

### Added
- Initial release of `supabase-java`
- Fluent, immutable PostgREST query builder
- Type-safe deserialization using Jackson
- Support for:
  - SELECT (single + list)
  - Filters: eq, neq, gt, gte, lt, lte, like, ilike, in, is, not, or
  - Ordering, pagination (limit, offset, range)
  - INSERT (single + bulk)
  - UPSERT
  - UPDATE (with safety guard)
  - DELETE (with safety guard)
  - RPC (single + list)
- Structured error handling via `SupabaseException`
- Custom `OkHttpClient` support
- Thread-safe client design

### Notes
- Auth (GoTrue), Storage, Edge Functions, and Realtime are not yet implemented
- Requires anon/service key (no session management yet)

## [0.2.0] - 2026-05-25

### Added
- Full **Auth (GoTrue) implementation**
    - Sign up, sign in, sign out
    - Token refresh
- `AuthClient` module exposed via `SupabaseClient.auth()`
- Support for **authenticated PostgREST requests**
    - Automatic `Authorization: Bearer <access_token>` header injection
- `SupabaseClient.withAccessToken(token)` for request-scoped authentication
- Improved SupabaseClient design to support stateless authenticated instances

### Changed
- `SupabaseClient` now supports optional access token context
- PostgREST requests now conditionally include auth headers when token is present
- Internal HTTP layer updated to support dynamic header injection

### Notes
- Auth is now fully supported, but session persistence (storage/refresh automation) is left to the consuming application
- Storage, Edge Functions, and Realtime remain planned

### Testing
- Added initial unit tests for Auth and PostgREST integration