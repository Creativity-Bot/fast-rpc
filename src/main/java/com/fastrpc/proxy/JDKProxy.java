package com.fastrpc.proxy;

import com.fastrpc.exception.ConnectException;
import com.fastrpc.exception.WriteFailureException;
import com.fastrpc.loadbalancer.LoadBalancer;
import com.fastrpc.transport.Transport;
import com.fastrpc.transport.impl.NettyClientTransport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class JDKProxy implements InvocationHandler {
    private static final int MAX_ATTEMPTS = 3;
    private static final int BACKOFF_PERIOD = 1;
    private static final int BACKOFF_MULTIPLIER = 2;

    private Class<?> target;
    private String ip;
    private int port;
    private LoadBalancer loadBalancer;
    private Transport transport;



    public JDKProxy(Class<?> target, String ip, int port) {
        this(target, ip, port, new NettyClientTransport());
    }

    public JDKProxy(Class<?> target, String ip, int port, Transport transport) {
        this(target, ip, port, null, transport);
    }

    public JDKProxy(Class<?> target, LoadBalancer loadBalancer, Transport transport) {
        this(target, null, -1, loadBalancer, transport);
    }

    public JDKProxy(Class<?> target, String ip, int port, LoadBalancer loadBalancer, Transport transport) {
        this.target = target;
        this.ip = ip;
        this.port = port;
        this.loadBalancer = loadBalancer;
        this.transport = transport;
    }

    public Object createProxy() {
        return Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target}, this);
    }




    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long backoffSeconds = BACKOFF_PERIOD;
        for (int attempt = 1; ; attempt++) {
            try {
                InetSocketAddress address = loadBalancer != null
                        ? loadBalancer.next(target.getName())
                        : new InetSocketAddress(ip, port);
                return transport.send(address, target, method, args);
            } catch (ConnectException | WriteFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                try {
                    TimeUnit.SECONDS.sleep(backoffSeconds);
                    backoffSeconds *= BACKOFF_MULTIPLIER;
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw interruptedException;
                }
            }
        }
    }
}
