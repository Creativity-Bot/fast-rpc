package com.fastrpc.transport;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public interface Transport {
    Object send(InetSocketAddress address, Class<?> target, Method method, Object[] args) throws Throwable;
}
