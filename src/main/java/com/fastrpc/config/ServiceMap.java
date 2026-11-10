package com.fastrpc.config;

import java.util.HashMap;
import java.util.Map;

public class ServiceMap {
    private final Map<String, Object> services = new HashMap<>();

    public void register(String serviceName, Object implementation) {
        services.putIfAbsent(serviceName, implementation);
    }

    public Object get(String serviceName) {
        return services.get(serviceName);
    }
}
