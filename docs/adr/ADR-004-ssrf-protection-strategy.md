# ADR-004: SSRF protection strategy for outbound discovery requests

## Status
Accepted (with one documented follow-up)

## Context
API Lens must fetch URLs it does not control (a user-supplied API base
URL, and paths derived from it during discovery). This is a direct SSRF
risk: without protection, the backend itself becomes a tool for reaching
internal services, cloud metadata endpoints, or loopback-bound services
that the caller couldn't otherwise reach.

## Decision
1. Concentrate all such fetching behind one component, `SecureHttpClient`,
   and make it an architectural rule (not just a convention) that no
   controller or service holds its own `WebClient`/HTTP client pointed at
   an external, user-influenced host.
2. Validate the *resolved IP address*, not the hostname string, against a
   blocklist covering loopback, RFC1918 private ranges, link-local
   (which covers 169.254.169.254-style cloud metadata endpoints),
   multicast, IPv6 unique-local, and several additional reserved ranges
   (carrier-grade NAT, TEST-NET, etc.) not covered by `InetAddress`'s
   built-in helpers.
3. Never auto-follow redirects at the HTTP-client level; instead, treat
   each redirect target as a brand new request that goes through the same
   validation path, capped at a configurable maximum.
4. Enforce connect/read timeouts and a maximum response size via the HTTP
   client's own configuration (codec `maxInMemorySize`), rather than
   trusting `Content-Length`.
5. Explicitly do **not** implement full DNS-rebinding protection (pinning
   the literal validated IP for the actual socket connection while still
   presenting the correct hostname for TLS SNI) in this pass. The
   correct way to do this depends on precise, version-specific behavior
   of the underlying Reactor Netty client that I was not able to verify
   with confidence in this environment; shipping a plausible-looking but
   unverified implementation would be worse than being explicit about the
   gap. See `docs/security.md` for the residual risk assessment.

## Consequences
- Positive: the primary SSRF vector (an attacker-supplied URL resolving
  directly to an internal/metadata address) is closed and has direct unit
  test coverage (`IpAddressValidatorTest`, `SecureHttpClientTest`).
- Positive: because validation is IP-based rather than hostname-string-based,
  obvious bypass attempts (decimal/octal IP encodings, IPv4-mapped IPv6
  literals) are also blocked — `InetAddress.getByName` and the unwrap step
  normalize these before the check runs.
- Trade-off: a DNS-rebinding window remains (see above). This is recorded
  as a known limitation with a suggested direction (custom Reactor Netty
  `AddressResolverGroup` or a hand-rolled connection using a pre-resolved
  `InetSocketAddress` with explicit SNI configuration) for a follow-up
  phase, rather than left undocumented.
