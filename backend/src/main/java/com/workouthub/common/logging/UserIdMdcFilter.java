package com.workouthub.common.logging;

import com.workouthub.common.security.AppUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * After Spring Security has populated the SecurityContext (i.e. AFTER
 * JwtAuthenticationFilter), pushes the authenticated user id into MDC
 * under key {@code user_id}. Removed in {@code finally} so the value
 * never leaks into unrelated threads in a thread-pool runtime.
 *
 * <p>Not profile-gated: MDC propagation is useful in dev/test logs too;
 * only the JSON output and secret masking are prod-only.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class UserIdMdcFilter extends OncePerRequestFilter {

    static final String MDC_KEY = "user_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean pushed = false;
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            MDC.put(MDC_KEY, principal.userId().toString());
            pushed = true;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (pushed) {
                MDC.remove(MDC_KEY);
            }
        }
    }
}
