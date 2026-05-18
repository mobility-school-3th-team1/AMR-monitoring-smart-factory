package com.sfaas.amr_control_system.security;

public interface RefreshTokenStore {

    void register(String jti, String username);

    boolean isRegistered(String jti);

    void revoke(String jti);

    void revokeAllForUser(String username);
}
