package com.fastrpc.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {

    void accept(String serviceName, List<InetSocketAddress> addresses);

    InetSocketAddress next(String serviceName);
}
