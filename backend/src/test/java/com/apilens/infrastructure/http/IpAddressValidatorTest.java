package com.apilens.infrastructure.http;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

/** Same package as IpAddressValidator, so its package-private isBlocked() is called directly. */
class IpAddressValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",          // loopback
            "127.5.5.5",          // loopback range
            "10.0.0.5",           // RFC1918 private
            "172.16.0.1",         // RFC1918 private
            "192.168.1.1",        // RFC1918 private
            "169.254.169.254",    // cloud metadata (link-local range)
            "169.254.1.1",        // link-local
            "0.0.0.0",            // any-local
            "224.0.0.1",          // multicast
            "100.64.0.1",         // carrier-grade NAT
            "192.0.2.1",          // TEST-NET-1
            "255.255.255.255",    // limited broadcast
            "::1",                 // IPv6 loopback
            "fe80::1",             // IPv6 link-local
            "fc00::1",             // IPv6 unique local
            "::ffff:127.0.0.1",    // IPv4-mapped loopback
            "::ffff:10.0.0.1"      // IPv4-mapped private
    })
    void blocksUnsafeAddresses(String literal) throws Exception {
        assertThat(IpAddressValidator.isBlocked(InetAddress.getByName(literal)))
                .as("expected %s to be blocked", literal)
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "8.8.8.8",             // public
            "1.1.1.1",             // public
            "93.184.216.34"        // public (example.com, historical)
    })
    void allowsPublicAddresses(String literal) throws Exception {
        assertThat(IpAddressValidator.isBlocked(InetAddress.getByName(literal)))
                .as("expected %s to be allowed", literal)
                .isFalse();
    }
}
