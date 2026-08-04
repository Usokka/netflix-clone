package com.netflixclone.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void ignoresForwardedHeaderWhenProxyTrustIsDisabled() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void usesAddressAppendedByTrustedProxyInsteadOfClientSuppliedValue() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");

        assertEquals("10.0.0.2", resolver.resolve(request));
    }

    @Test
    void rejectsMalformedForwardedAddress() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "attacker-controlled-value");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void rejectsNumericButInvalidForwardedAddress() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "999.999.999.999");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }
}
