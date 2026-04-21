package com.vitacare.api.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public final class RoleFlags {
    public static final int CLIENT = 1;
    public static final int NUTRITIONIST = 2;
    public static final int ADMIN = 4;

    private RoleFlags() {
    }

    public static int parseRoles(List<String> roles, int fallback) {
        if (roles == null || roles.isEmpty()) {
            return fallback;
        }

        int mask = 0;
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid role");
            }
            mask |= parseRole(role);
        }
        return mask;
    }

    public static int parseRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "CLIENT" -> CLIENT;
            case "NUTRITIONIST" -> NUTRITIONIST;
            case "ADMIN" -> ADMIN;
            default -> throw new ResponseStatusException(BAD_REQUEST, "Invalid role");
        };
    }

    public static boolean hasRole(int mask, int roleFlag) {
        return (mask & roleFlag) == roleFlag;
    }

    public static List<String> toNames(int mask) {
        Set<String> names = new LinkedHashSet<>();
        if (hasRole(mask, CLIENT)) {
            names.add("CLIENT");
        }
        if (hasRole(mask, NUTRITIONIST)) {
            names.add("NUTRITIONIST");
        }
        if (hasRole(mask, ADMIN)) {
            names.add("ADMIN");
        }
        return List.copyOf(names);
    }

    public static List<SimpleGrantedAuthority> toAuthorities(int mask) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (hasRole(mask, CLIENT)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        if (hasRole(mask, NUTRITIONIST)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_NUTRITIONIST"));
        }
        if (hasRole(mask, ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return authorities;
    }
}