package com.vitacare.api.security;

import com.vitacare.api.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {

    private final ConcurrentHashMap<String, UUID> tokens = new ConcurrentHashMap<>();

    public String issueToken(User user) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, user.getId());
        return token;
    }

    public Optional<UUID> resolveUserId(String token) {
        return Optional.ofNullable(tokens.get(token));
    }
}