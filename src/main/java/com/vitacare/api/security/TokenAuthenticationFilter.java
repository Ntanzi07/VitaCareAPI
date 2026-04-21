package com.vitacare.api.security;

import com.vitacare.api.model.User;
import com.vitacare.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;

    public TokenAuthenticationFilter(UserRepository userRepository, AuthTokenService authTokenService) {
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(7).trim();
            authTokenService.resolveUserId(token)
                .flatMap(userRepository::findById)
                .ifPresent(this::authenticateUser);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(User user) {
        int roleMask = user.getRoleMask() == null ? RoleFlags.CLIENT : user.getRoleMask();
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), roleMask);
        var authorities = RoleFlags.toAuthorities(roleMask);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}