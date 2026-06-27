package com.itmal.auth.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailVerificationStore {

    private final ConcurrentHashMap<String, VerificationEntry> store = new ConcurrentHashMap<>();

    private record VerificationEntry(String code, LocalDateTime expiredAt) {}

    public void save(String email, String code, Duration ttl) {
        store.put(email, new VerificationEntry(code, LocalDateTime.now().plus(ttl)));
    }

    public boolean verify(String email, String code) {
        VerificationEntry entry = store.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiredAt())) {
            store.remove(email);
            return false;
        }
        return entry.code().equals(code);
    }

    public void delete(String email) {
        store.remove(email);
    }
}
