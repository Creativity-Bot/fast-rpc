package com.fastrpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

@FunctionalInterface
public interface ServiceListener {

    void onAddresses(List<InetSocketAddress> addresses);
}
