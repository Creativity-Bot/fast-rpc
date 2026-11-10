package com.fastrpc.registry;

import java.net.InetSocketAddress;

public interface Registry {

    String SERVICE_PREFIX = "/frpc";
    String SERVICE_SUFFIX = "/addresses";

    void registerService(String targetInterfaceName, InetSocketAddress hostAndPort);

    void subscribeService(String targetInterfaceName, ServiceListener listener);

}
