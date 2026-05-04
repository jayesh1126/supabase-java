# Contributing

Thanks for your interest in contributing to `supabase-java`.

## Getting Started

```bash
git clone https://github.com/jayesh1126/supabase-java.git
cd supabase-java
mvn install
```

## Development Guidelines

- Java 17+
- Follow existing code style and structure
- All builder methods must remain immutable
- Public APIs should be type-safe and fluent
- Avoid introducing unnecessary abstractions or interfaces

## Code Standards
- Use SupabaseException for HTTP/network errors
- Use IllegalArgumentException for invalid inputs
- Use IllegalStateException for invalid builder states
- Add Javadoc for all public methods

## Tests

## Pull Requests
- Open an issue before large changes
- Keep PRs focused and small
- Include tests where applicable
- Ensure build passes before submitting

## Roadmap Focus Areas
- Auth (GoTrue)
- Storage API
- Realtime (WebSocket)
- Async API (CompletableFuture)
- Improved error handling

Thanks for contributing !