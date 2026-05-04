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