package com.fastrpc.loadbalancer.impl;

import com.fastrpc.loadbalancer.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RoundRobinLoadBalancer implements LoadBalancer {

    private final Map<String, Endpoints> services = new ConcurrentHashMap<>();

    @Override
    public void accept(String serviceName, List<InetSocketAddress> addresses) {
        Endpoints endpoints = services.computeIfAbsent(serviceName, name -> new Endpoints());
        endpoints.addresses.set(addresses);
    }

    @Override
    public InetSocketAddress next(String serviceName) {
        Endpoints endpoints = services.get(serviceName);
        if (endpoints == null) {
            throw new IllegalStateException("no available address for " + serviceName);
        }
        List<InetSocketAddress> snapshot = endpoints.addresses.get();
        if (snapshot.isEmpty()) {
            throw new IllegalStateException("no available address for " + serviceName);
        }
        int index = endpoints.index.getAndIncrement() & Integer.MAX_VALUE;
        return snapshot.get(index % snapshot.size());
    }

    private static final class Endpoints {
        private final AtomicReference<List<InetSocketAddress>> addresses = new AtomicReference<>(List.of());
        private final AtomicInteger index = new AtomicInteger();
    }
}
