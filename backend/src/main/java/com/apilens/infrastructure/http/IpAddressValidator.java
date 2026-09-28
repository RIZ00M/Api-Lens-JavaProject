package com.apilens.infrastructure.http;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Decides whether a *resolved* IP address is safe to connect to.
 *
 * This is deliberately checked against the resolved address, never against
 * the hostname string — a hostname is just a hint about where to look up
 * an address; the address is what actually gets connected to, and is the
 * only thing that can be trusted for this decision.
 */
final class IpAddressValidator {

    private IpAddressValidator() {
    }

    // Ranges not already covered by InetAddress's own isLoopback/isLinkLocal/
    // isSiteLocal/isMulticast/isAnyLocal checks below.
    private static final List<CidrRange> ADDITIONAL_BLOCKED_RANGES = List.of(
            CidrRange.of("0.0.0.0/8"),          // "this network"
            CidrRange.of("100.64.0.0/10"),      // carrier-grade NAT (RFC 6598)
            CidrRange.of("192.0.0.0/24"),       // IETF protocol assignments
            CidrRange.of("192.0.2.0/24"),       // TEST-NET-1
            CidrRange.of("198.18.0.0/15"),      // benchmarking
            CidrRange.of("198.51.100.0/24"),    // TEST-NET-2
            CidrRange.of("203.0.113.0/24"),     // TEST-NET-3
            CidrRange.of("240.0.0.0/4"),        // reserved
            CidrRange.of("255.255.255.255/32"), // limited broadcast
            CidrRange.of("fc00::/7")            // IPv6 unique local addresses
    );

    static boolean isBlocked(InetAddress address) {
        InetAddress unwrapped = unwrapIpv4MappedAddress(address);

        return unwrapped.isLoopbackAddress()       // 127.0.0.0/8, ::1
                || unwrapped.isLinkLocalAddress()   // 169.254.0.0/16 (covers cloud metadata endpoints), fe80::/10
                || unwrapped.isMulticastAddress()   // 224.0.0.0/4, ff00::/8
                || unwrapped.isSiteLocalAddress()   // RFC 1918 private IPv4 ranges
                || unwrapped.isAnyLocalAddress()    // 0.0.0.0, ::
                || ADDITIONAL_BLOCKED_RANGES.stream().anyMatch(range -> range.contains(unwrapped));
    }

    /**
     * An IPv4-mapped IPv6 address (::ffff:a.b.c.d) would otherwise slip past
     * the IPv4-specific checks above, since the JVM represents it as an
     * Inet6Address. Unwrap it to plain IPv4 first so it's checked the same
     * way a literal IPv4 address would be.
     */
    private static InetAddress unwrapIpv4MappedAddress(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return address;
        }
        byte[] bytes = address.getAddress();
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return address;
            }
        }
        if ((bytes[10] & 0xFF) != 0xFF || (bytes[11] & 0xFF) != 0xFF) {
            return address;
        }
        try {
            return InetAddress.getByAddress(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
        } catch (UnknownHostException e) {
            return address; // 4-byte array is always a valid literal; unreachable in practice
        }
    }
}
