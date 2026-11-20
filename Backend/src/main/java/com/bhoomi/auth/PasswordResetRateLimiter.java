package com.bhoomi.auth;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PasswordResetRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_NANOS = Duration.ofMinutes(15).toNanos();
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Window> clients = new HashMap<>();

    public synchronized boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        long now = System.nanoTime();
        Window existing = clients.get(key);
        if (existing == null) {
            if (clients.size() >= MAX_TRACKED_CLIENTS) {
                removeExpiredWindows(now);
                if (clients.size() >= MAX_TRACKED_CLIENTS) {
                    return false;
                }
            }
            clients.put(key, new Window(now, 1));
            return true;
        }
        Window window = now - existing.startedAtNanos >= WINDOW_NANOS
                ? new Window(now, 1)
                : new Window(existing.startedAtNanos, existing.requests + 1);
        clients.put(key, window);
        return window.requests <= MAX_REQUESTS;
    }

    private void removeExpiredWindows(long now) {
        Iterator<Window> windows = clients.values().iterator();
        while (windows.hasNext()) {
            if (now - windows.next().startedAtNanos >= WINDOW_NANOS) {
                windows.remove();
            }
        }
    }

    private record Window(long startedAtNanos, int requests) {
    }
}
