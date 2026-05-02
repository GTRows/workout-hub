package com.workouthub.common.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Holds a parsed list of CIDR ranges (or single IPs) and answers whether a
 * given remote address falls inside any of them. Used by ForwardAuthFilter
 * to gate trust of X-Forwarded-* identity headers per
 * docs/SELF_HOSTED_CONTRACT.md section 7.2.
 *
 * <p>An empty CSV (or null) means "trust nothing" - the safer default than
 * trusting all callers.
 */
public final class TrustedProxyMatcher {

    private final List<IpAddressMatcher> matchers;

    public TrustedProxyMatcher(String csv) {
        this.matchers = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String cidr : csv.split(",")) {
            String trimmed = cidr.trim();
            if (!trimmed.isEmpty()) {
                matchers.add(new IpAddressMatcher(trimmed));
            }
        }
    }

    public boolean isTrusted(String remoteAddr) {
        if (remoteAddr == null || matchers.isEmpty()) {
            return false;
        }
        for (IpAddressMatcher m : matchers) {
            if (m.matches(remoteAddr)) {
                return true;
            }
        }
        return false;
    }
}
