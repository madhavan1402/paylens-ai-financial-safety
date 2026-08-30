package com.paylens.backend.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance in-memory rate limiter for authentication endpoints.
 * Production multi-instance deployments should use distributed rate limiting (e.g. Redis).
 */
@Service
public class RateLimiterService {

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();
    private final int MAX_REQUESTS_PER_MINUTE = 10;

    private volatile boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean tryAcquire(String key) {
        if (!enabled) {
            return true;
        }
        long now = System.currentTimeMillis();
        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startTimeMs > 60000) {
                return new RateBucket(now, 1);
            }
            existing.count++;
            return existing;
        });
        return bucket.count <= MAX_REQUESTS_PER_MINUTE;
    }

    public void reset() {
        buckets.clear();
    }

    private static class RateBucket {
        long startTimeMs;
        int count;

        RateBucket(long startTimeMs, int count) {
            this.startTimeMs = startTimeMs;
            this.count = count;
        }
    }
}
