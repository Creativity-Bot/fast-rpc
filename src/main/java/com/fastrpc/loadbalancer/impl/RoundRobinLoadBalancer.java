package com.fastrpc.loadbalancer.impl;

import com.fastrpc.loadbalancer.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;

public class RoundRobinLoadBalancer implements LoadBalancer {



    @Override
    public InetSocketAddress next(List<InetSocketAddress> hostAndPorts) {
        return null;
    }
}
