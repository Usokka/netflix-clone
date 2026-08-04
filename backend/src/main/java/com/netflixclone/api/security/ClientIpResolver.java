package com.netflixclone.api.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    private static final Pattern SAFE_IP_VALUE = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");
    private final boolean trustForwardedHeaders;

    public ClientIpResolver(
            @Value("${app.security.trust-forwarded-headers:false}") boolean trustForwardedHeaders
    ) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolve(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String[] addresses = forwardedFor.split(",");
                String proxyAppendedAddress = addresses[addresses.length - 1].trim();
                if (isValidIpAddress(proxyAppendedAddress)) {
                    return proxyAppendedAddress;
                }
            }
        }

        return request.getRemoteAddr();
    }

    private boolean isValidIpAddress(String value) {
        if (!SAFE_IP_VALUE.matcher(value).matches()) {
            return false;
        }

        try {
            InetAddress.getByName(value);
            return value.contains(":") || hasValidIpv4Segments(value);
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private boolean hasValidIpv4Segments(String value) {
        String[] segments = value.split("\\.", -1);
        if (segments.length != 4) return false;

        for (String segment : segments) {
            try {
                if (segment.isEmpty() || Integer.parseInt(segment) > 255) return false;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }
}
