package com.fastrpc.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRegistry {
    private final Map<String, Object> services = new ConcurrentHashMap<>();

    public void register(String serviceName, Object implementation) {
        services.put(serviceName, implementation);
    }

    public Object get(String serviceName) {
        return services.get(serviceName);
    }
}
