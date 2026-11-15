package com.fastrpc.loadbalancer.impl;

import com.fastrpc.loadbalancer.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
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
    public InetSocketAddress select(String serviceName, List<InetSocketAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            throw new IllegalStateException("all services are temporarily unavailable");
        }
        int index = services.get(serviceName).index.getAndIncrement() & Integer.MAX_VALUE;
        return addresses.get(index % addresses.size());
    }


    @Override
    public List<InetSocketAddress> getAddresses(String serviceName) {
        List<InetSocketAddress> addreList = services.get(serviceName).addresses.get();
        if(addreList.isEmpty()){
            throw new IllegalStateException("no available address from service registry");
        }
        // return copy of
        return new ArrayList<>(addreList);
    }


    private static final class Endpoints {
        private final AtomicReference<List<InetSocketAddress>> addresses = new AtomicReference<>(List.of());
        private final AtomicInteger index = new AtomicInteger();
    }
}
