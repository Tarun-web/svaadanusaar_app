package com.dietapp.diet_app.common.service;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final ConcurrentMap<String, Long> blacklist = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("token-blacklist-cleaner");
        return t;
    });

    @PostConstruct
    public void init() {
        // run cleanup every minute
        cleaner.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        cleaner.shutdownNow();
    }

    @Override
    public void blacklistToken(String token, long ttlMillis) {
        long expiry = System.currentTimeMillis() + ttlMillis;
        blacklist.put(token, expiry);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : blacklist.entrySet()) {
            if (e.getValue() < now) {
                blacklist.remove(e.getKey(), e.getValue());
            }
        }
    }
}


