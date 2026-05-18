package com.sfaas.amr_control_system.security;

public final class JwtConstants {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_JTI = "jti";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    private JwtConstants() {
    }
}
