package com.fastrpc.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {
    InetSocketAddress next(List<InetSocketAddress> hostAndPorts);
}
