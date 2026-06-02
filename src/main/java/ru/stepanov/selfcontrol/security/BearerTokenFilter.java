package ru.stepanov.selfcontrol.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final TokenService tokenService;

    public BearerTokenFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String rawToken = h.substring(7).trim();
            tokenService.parse(rawToken).ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority(u.role().authority())))));
        }
        chain.doFilter(req, res);
    }
}
