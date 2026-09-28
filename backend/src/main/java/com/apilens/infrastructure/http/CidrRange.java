package com.apilens.infrastructure.http;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A CIDR block (e.g. "100.64.0.0/10"), used to block IP ranges that
 * {@link InetAddress}'s own isLoopback/isLinkLocal/isSiteLocal/isMulticast
 * helpers don't cover (carrier-grade NAT, IANA special-purpose ranges,
 * IPv6 unique-local addresses, etc).
 */
final class CidrRange {

    private final byte[] base;
    private final int prefixLength;

    private CidrRange(byte[] base, int prefixLength) {
        this.base = base;
        this.prefixLength = prefixLength;
    }

    static CidrRange of(String cidr) {
        String[] parts = cidr.split("/");
        try {
            byte[] base = InetAddress.getByName(parts[0]).getAddress();
            return new CidrRange(base, Integer.parseInt(parts[1]));
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid CIDR literal: " + cidr, e);
        }
    }

    boolean contains(InetAddress address) {
        byte[] candidate = address.getAddress();
        if (candidate.length != base.length) {
            return false; // IPv4 range can't contain an IPv6 address and vice versa
        }

        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (candidate[i] != base[i]) {
                return false;
            }
        }
        if (remainingBits > 0) {
            int mask = 0xFF << (8 - remainingBits);
            if ((candidate[fullBytes] & mask) != (base[fullBytes] & mask)) {
                return false;
            }
        }
        return true;
    }
}
