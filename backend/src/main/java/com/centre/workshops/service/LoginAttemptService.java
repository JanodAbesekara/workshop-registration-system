package com.centre.workshops.service;

import com.centre.workshops.exception.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final class Attempt {
        int failures;
        Instant windowStart = Instant.now();
        Instant lockedUntil;
    }

    private final Map<String, Attempt> attempts = new HashMap<>();

    public synchronized void checkAllowed(String key) {
        Attempt a = attempts.get(key);
        if (a != null && a.lockedUntil != null) {
            if (Instant.now().isBefore(a.lockedUntil)) {
                throw ApiException.tooManyRequests("Too many failed sign-in attempts. Try again in a few minutes.");
            }
            attempts.remove(key);
        }
    }

    public synchronized void recordFailure(String key) {
        Instant now = Instant.now();
        Attempt a = attempts.computeIfAbsent(key, k -> new Attempt());
        if (now.isAfter(a.windowStart.plus(WINDOW))) {
            a.failures = 0;
            a.windowStart = now;
        }
        a.failures++;
        if (a.failures >= MAX_FAILURES) {
            a.lockedUntil = now.plus(WINDOW);
        }
    }

    public synchronized void recordSuccess(String key) {
        attempts.remove(key);
    }
}
