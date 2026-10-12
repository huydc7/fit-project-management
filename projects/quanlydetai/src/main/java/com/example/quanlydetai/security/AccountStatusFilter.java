package com.example.quanlydetai.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Refresh permissions so account locks/revoked dean assignments affect existing sessions. */
public final class AccountStatusFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService users;

    public AccountStatusFilter(CustomUserDetailsService users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AccountPrincipal previous) {
            try {
                var current = users.loadById(previous.getAccountId());
                if (!current.isEnabled()
                        || !current.isAccountNonLocked()
                        || !current.getCredentialVersion()
                                .equals(previous.getCredentialVersion())) {
                    expire(request, response);
                    return;
                }
                current.eraseCredentials();
                var refreshed =
                        UsernamePasswordAuthenticationToken.authenticated(
                                current, null, current.getAuthorities());
                refreshed.setDetails(authentication.getDetails());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(refreshed);
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException ex) {
                expire(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void expire(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
        if (request.getRequestURI()
                .substring(request.getContextPath().length())
                .startsWith("/api/")) {
            ApiSecurityResponse.write(response, 401);
        } else {
            response.sendRedirect(request.getContextPath() + "/login?expired");
        }
    }
}
