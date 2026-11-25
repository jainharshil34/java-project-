package com.civicheat.security;

import com.civicheat.config.CivicHeatProperties;
import com.civicheat.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding window in-memory rate limiter protecting complaint submission endpoints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final CivicHeatProperties properties;
    private final Map<String, WindowBucket> clientBuckets = new ConcurrentHashMap<>();

    /**
     * Verifies if the client key is within permitted rate limits, or throws RateLimitExceededException.
     *
     * @param clientKey IP address or authenticated user identifier
     */
    public void checkLimit(String clientKey) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }

        int maxPerMinute = properties.getRateLimit().getMaxRequestsPerMinute();
        long currentMinute = Instant.now().getEpochSecond() / 60;

        WindowBucket bucket = clientBuckets.compute(clientKey, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowBucket(currentMinute, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (bucket.counter.get() > maxPerMinute) {
            log.warn("Rate limit exceeded for client: {} ({}/min)", clientKey, bucket.counter.get());
            throw new RateLimitExceededException(
                    "Rate limit exceeded: You have submitted too many requests. Please try again in a few moments."
            );
        }

        // Periodic cleanup of stale window entries (if map grows > 10,000 keys)
        if (clientBuckets.size() > 10000) {
            clientBuckets.entrySet().removeIf(e -> e.getValue().minute < currentMinute - 1);
        }
    }

    private record WindowBucket(long minute, AtomicInteger counter) {}
}
