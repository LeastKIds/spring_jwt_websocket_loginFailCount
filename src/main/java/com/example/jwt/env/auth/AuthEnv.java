package com.example.jwt.env.auth;

public class AuthEnv {
    private long EXPIRE_LOGIN_FAIL;

    public AuthEnv() {
        this.EXPIRE_LOGIN_FAIL = 60L * 1000 * 30;
    }

    public Long getEXPIRE_LOGIN_FAIL() { return this.EXPIRE_LOGIN_FAIL; }
}
