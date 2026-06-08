package com.qindongyun.portfolio.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    @Test
    void rejectsEleventhRequestWithinOneMinute() {
        RequestRateLimiter limiter = new RequestRateLimiter(
                Clock.fixed(Instant.parse("2026-06-02T00:00:00Z"), ZoneOffset.UTC));

        for (int index = 0; index < 10; index++) {
            assertThat(limiter.tryAcquire("visitor")).isTrue();
        }

        assertThat(limiter.tryAcquire("visitor")).isFalse();
    }
}

