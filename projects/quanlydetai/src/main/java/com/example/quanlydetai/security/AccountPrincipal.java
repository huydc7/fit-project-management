package com.example.quanlydetai.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public final class AccountPrincipal extends User {
    private final String accountId;
    private final String maGV;
    private final String maSV;
    private final String credentialVersion;

    public AccountPrincipal(
            String accountId,
            String username,
            String password,
            boolean active,
            String maGV,
            String maSV,
            Collection<? extends GrantedAuthority> roles) {
        super(username, password, active, true, true, active, roles);
        this.accountId = accountId;
        this.maGV = maGV;
        this.maSV = maSV;
        try {
            this.credentialVersion =
                    java.util.HexFormat.of()
                            .formatHex(
                                    java.security.MessageDigest.getInstance("SHA-256")
                                            .digest(
                                                    password.getBytes(
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String getAccountId() {
        return accountId;
    }

    public String getMaGV() {
        return maGV;
    }

    public String getMaSV() {
        return maSV;
    }

    public String getCredentialVersion() {
        return credentialVersion;
    }
}
