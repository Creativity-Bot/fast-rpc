package com.fastrpc.config;

import com.fastrpc.limiter.TokenBucket;

import java.util.HashMap;
import java.util.Map;

public class ServiceRateLimiter {
    private final Map<String, TokenBucket> services = new HashMap<>();

    public void register(String serviceName) {
        register(serviceName, 100, 100);
    }

    public void register(String serviceName, double permitsPerSecond, double capacity) {
        services.putIfAbsent(serviceName, new TokenBucket(permitsPerSecond, capacity));
    }

    public TokenBucket get(String serviceName) {
        return services.get(serviceName);
    }
}
