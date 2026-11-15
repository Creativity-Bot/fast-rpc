package com.fastrpc.loadbalancer;

import java.net.InetSocketAddress;
import java.util.List;

public interface LoadBalancer {

    void accept(String serviceName, List<InetSocketAddress> addresses);

    InetSocketAddress select(String serviceName, List<InetSocketAddress> addresses);

    List<InetSocketAddress> getAddresses(String serviceName);
}
