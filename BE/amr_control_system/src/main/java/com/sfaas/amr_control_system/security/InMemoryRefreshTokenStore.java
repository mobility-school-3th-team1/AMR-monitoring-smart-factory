package com.sfaas.amr_control_system.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, String> jtiToUsername = new ConcurrentHashMap<>();

    @Override
    public void register(String jti, String username) {
        jtiToUsername.put(jti, username);
    }

    @Override
    public boolean isRegistered(String jti) {
        return jtiToUsername.containsKey(jti);
    }

    @Override
    public void revoke(String jti) {
        jtiToUsername.remove(jti);
    }

    @Override
    public void revokeAllForUser(String username) {
        jtiToUsername.entrySet().removeIf(entry -> username.equals(entry.getValue()));
    }
}
