package com.fastrpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

public interface Registry {

    String SERVICE_PREFIX = "/frpc";
    String SERVICE_SUFFIX = "/addresses";

    void registerService(String targetInterfaceName, InetSocketAddress hostAndPort);

    List<InetSocketAddress> pullService(String targetInterfaceName);

    void subscribeService(String targetInterfaceName, ServiceListener listener);

}
