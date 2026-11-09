package com.fastrpc.limiter;

import java.util.concurrent.atomic.AtomicReference;

public final class TokenBucket {
    private final double capacity;
    private final double refillPerNanos;
    private final AtomicReference<State> state;

    public TokenBucket(double permitsPerSecond, double capacity) {
        this.capacity = capacity;
        this.refillPerNanos = permitsPerSecond / 1_000_000_000d;
        long now = System.nanoTime();
        this.state = new AtomicReference<>(new State(capacity, now));
    }

    public boolean tryAcquire() {
        while (true) {
            State old = state.get();
            long now = System.nanoTime();
            double elapsedNanos = Math.max(0d, now - old.lastRefillNanos);
            double refilled = Math.min(capacity, old.tokens + elapsedNanos * refillPerNanos);
            if (refilled < 1d) {
                return false;
            }
            State next = new State(refilled - 1d, now);
            if (state.compareAndSet(old, next)) {
                return true;
            }
        }
    }

    private static final class State {
        private final double tokens;
        private final long lastRefillNanos;

        private State(double tokens, long lastRefillNanos) {
            this.tokens = tokens;
            this.lastRefillNanos = lastRefillNanos;
        }
    }
}
