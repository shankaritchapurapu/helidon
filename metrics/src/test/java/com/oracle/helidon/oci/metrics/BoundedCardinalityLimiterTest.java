/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 */

package com.oracle.helidon.oci.metrics;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class BoundedCardinalityLimiterTest {

    @Test
    void admitsRelatedKeysAtomically() {
        BoundedCardinalityLimiter<String> limiter = new BoundedCardinalityLimiter<>(3);

        assertThat(limiter.tryAdmit(List.of("a", "b")), is(true));
        assertThat(limiter.tryAdmit(List.of("b", "c")), is(true));
        assertThat(limiter.tryAdmit(List.of("d", "e")), is(false));
        assertThat(limiter.size(), is(3));
    }

    @Test
    void neverExceedsLimitUnderConcurrency() throws Exception {
        int limit = 50;
        BoundedCardinalityLimiter<String> limiter = new BoundedCardinalityLimiter<>(limit);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(16)) {
            var futures = IntStream.range(0, 500)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await();
                        return limiter.tryAdmit(List.of("key-" + index));
                    }))
                    .toList();
            start.countDown();
            for (var future : futures) {
                future.get();
            }
        }

        assertThat(limiter.size(), is(limit));
    }
}
