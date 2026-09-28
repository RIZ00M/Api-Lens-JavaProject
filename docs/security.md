# API Lens — Security: outbound HTTP (SSRF protections)

API Lens fetches URLs supplied by users (an API's base URL, an explicit
OpenAPI URL) and URLs derived from those during discovery (e.g. appending
`/openapi.json`). This is a textbook Server-Side Request Forgery (SSRF)
surface: without protection, a malicious "API base URL" could point at
`http://169.254.169.254/` (cloud metadata), `http://localhost:6379/`
(an internal Redis instance), or similar.

## The chokepoint: `SecureHttpClient`

Every outbound request the backend makes to an external, user-influenced
URL goes through `com.apilens.infrastructure.http.SecureHttpClient` — no
other class is permitted to hold a `WebClient` pointed at an arbitrary
external host. This is enforced as an architectural rule (see
`docs/architecture.md`), not just documented as a preference.

## What is enforced, and where in the code

| Protection | Enforced by |
|---|---|
| HTTPS only (by default) | `SecureHttpClient.fetch` — scheme check against `apilens.http.https-only` |
| Reject localhost / 127.0.0.0/8 | `IpAddressValidator.isBlocked` — `isLoopbackAddress()` |
| Reject RFC1918 private IPv4 | `IpAddressValidator.isBlocked` — `isSiteLocalAddress()` |
| Reject link-local (incl. cloud metadata, 169.254.0.0/16) | `IpAddressValidator.isBlocked` — `isLinkLocalAddress()` |
| Reject multicast | `IpAddressValidator.isBlocked` — `isMulticastAddress()` |
| Reject IPv6 loopback / private / unique-local | `IpAddressValidator.isBlocked` — `isLoopbackAddress()`, `fc00::/7` CIDR check |
| Reject other reserved ranges (CGNAT, TEST-NET, etc.) | `IpAddressValidator.ADDITIONAL_BLOCKED_RANGES` |
| IPv4-mapped IPv6 addresses can't bypass IPv4 checks | `IpAddressValidator.unwrapIpv4MappedAddress` |
| Redirects are validated, not blindly followed | `reactorHttpClient.followRedirect(false)` + `SecureHttpClient.followRedirect` recurses through the full validation path |
| Connection timeout | `ChannelOption.CONNECT_TIMEOUT_MILLIS`, `apilens.http.connect-timeout-ms` |
| Read timeout | `HttpClient.responseTimeout`, `apilens.http.read-timeout-ms` |
| Maximum response size | `ExchangeStrategies` `maxInMemorySize`, `apilens.http.max-response-size-bytes` |
| Maximum redirect count | `apilens.http.max-redirects`, checked in `followRedirect` |
| Content-type allow-list | `SecureHttpClient.isContentTypeAllowed`, `apilens.http.allowed-content-type-prefixes` |
| No credentials/inbound headers forwarded | Each outbound request is built from scratch (only `User-Agent`/`Accept` are set) — nothing from the caller's inbound request is copied |
| Blocked requests are logged | `SecureHttpClient.blockedAndLogged` logs `SSRF_REQUEST_BLOCKED url=... reason=...` |
| Safe error messages | `SecureFetchResult` carries a short `message`; raw exceptions are never returned to callers |

## DNS rebinding: known limitation

`SecureHttpClient` resolves the target hostname and validates every
resolved address *before* making the request. It does **not** currently
pin the HTTP connection to that specific validated IP address — the
underlying HTTP client re-resolves the hostname when it actually connects.
This leaves a narrow window in which an attacker controlling DNS for the
target hostname could, in principle, change the DNS answer between our
validation step and the client's connection step (classic "DNS rebinding").

This is called out explicitly (see `docs/adr/ADR-004-ssrf-protection-strategy.md`)
rather than silently left unhandled. The pragmatic mitigations already in
place — short-lived requests, no credential exposure even on a successful
rebind, and discovery only fetching a small set of well-known spec paths
rather than acting as a general-purpose proxy — substantially limit the
impact even if this window were exploited. Full protection (pinning the
literal socket connection to the pre-validated IP while still presenting
the correct hostname for TLS SNI/certificate validation) is tracked as a
follow-up hardening task rather than implemented now with an unverified
approach.

## Explicit non-goals

API Lens analyses API contracts from documentation and specifications. It
does not, and will not:
- brute-force or guess undocumented endpoint paths beyond the small,
  configured list of standard OpenAPI/Swagger locations,
- bypass authentication on a target API,
- attempt credential attacks,
- scan arbitrary internal networks,
- perform vulnerability exploitation.
