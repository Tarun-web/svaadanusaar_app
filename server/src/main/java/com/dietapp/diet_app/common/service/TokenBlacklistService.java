package com.dietapp.diet_app.common.service;

public interface TokenBlacklistService {
    /**
     * Blacklist a token for the given TTL in milliseconds.
     */
    void blacklistToken(String token, long ttlMillis);

    /**
     * Returns true if the token is blacklisted (and not yet expired in blacklist).
     */
    boolean isBlacklisted(String token);
}

