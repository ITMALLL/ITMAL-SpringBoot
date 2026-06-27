package com.itmal.auth.service;

import com.itmal.auth.exception.TooManyRequestsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class EmailVerificationStore {

    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final ConcurrentHashMap<String, VerificationEntry> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastSentAt = new ConcurrentHashMap<>();

    private record VerificationEntry(String code, LocalDateTime expiredAt) {}

    public void checkCooldown(String email) {
        AtomicBoolean blocked = new AtomicBoolean(false);
        LocalDateTime now = LocalDateTime.now();

        lastSentAt.compute(email, (key, lastSent) -> {
            if (lastSent != null && now.isBefore(lastSent.plus(COOLDOWN))) {
                blocked.set(true);
                return lastSent;
            }
            return lastSent;
        });

        if (blocked.get()) {
            throw new TooManyRequestsException("이메일 발송은 60초에 한 번만 가능합니다.");
        }
    }

    public void recordSend(String email) {
        lastSentAt.put(email, LocalDateTime.now());
    }

    public void save(String email, String code, Duration ttl) {
        store.put(email, new VerificationEntry(code, LocalDateTime.now().plus(ttl)));
    }

    public boolean verify(String email, String code) {
        AtomicBoolean matched = new AtomicBoolean(false);
        LocalDateTime now = LocalDateTime.now();

        store.compute(email, (key, entry) -> {
            if (entry == null || now.isAfter(entry.expiredAt())) {
                return null;
            }
            if (!entry.code().equals(code)) {
                return entry;
            }
            matched.set(true);
            return null; // 성공 시 즉시 소비
        });

        return matched.get();
    }

    public void delete(String email) {
        store.remove(email);
    }

    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        LocalDateTime now = LocalDateTime.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiredAt()));
        lastSentAt.entrySet().removeIf(e -> now.isAfter(e.getValue().plus(COOLDOWN)));
    }
}
