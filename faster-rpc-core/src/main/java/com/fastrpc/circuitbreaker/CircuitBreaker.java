package com.fastrpc.circuitbreaker;

import java.net.InetSocketAddress;

public interface CircuitBreaker {


    boolean allowRequest(InetSocketAddress address);

    void update(InetSocketAddress address, boolean isSuccess, Permit permit);

    Permit tryAcquire(InetSocketAddress address);

    interface Permit{
    }
}
