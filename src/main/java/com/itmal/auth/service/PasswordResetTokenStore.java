package com.itmal.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordResetTokenStore {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final ConcurrentHashMap<String, TokenEntry> store = new ConcurrentHashMap<>();

    private record TokenEntry(String email, LocalDateTime expiredAt) {}

    public String save(String email) {
        String token = java.util.UUID.randomUUID().toString();
        store.put(token, new TokenEntry(email, LocalDateTime.now().plus(TOKEN_TTL)));
        return token;
    }

    public Optional<String> findEmail(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null || LocalDateTime.now().isAfter(entry.expiredAt())) {
            store.remove(token);
            return Optional.empty();
        }
        return Optional.of(entry.email());
    }

    public void delete(String token) {
        store.remove(token);
    }

    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiredAt()));
    }
}
