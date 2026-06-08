package com.qindongyun.portfolio.chat;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private final Map<String, Deque<Instant>> requests = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public synchronized boolean tryAcquire(String key) {
        Instant cutoff = clock.instant().minus(1, ChronoUnit.MINUTES);
        Deque<Instant> timestamps = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.removeFirst();
        }
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            return false;
        }
        timestamps.addLast(clock.instant());
        return true;
    }
}

