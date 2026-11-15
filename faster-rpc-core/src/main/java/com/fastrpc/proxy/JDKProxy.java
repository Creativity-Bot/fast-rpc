package com.fastrpc.proxy;

import com.fastrpc.circuitbreaker.CircuitBreaker;
import com.fastrpc.exception.ConnectException;
import com.fastrpc.exception.TimeoutException;
import com.fastrpc.exception.WriteFailureException;
import com.fastrpc.loadbalancer.LoadBalancer;
import com.fastrpc.transport.Transport;
import com.fastrpc.transport.impl.NettyClientTransport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
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
    private CircuitBreaker circuitBreaker;



    public JDKProxy(Class<?> target, String ip, int port) {
        this(target, ip, port, new NettyClientTransport());
    }

    public JDKProxy(Class<?> target, String ip, int port, Transport transport) {
        this(target, ip, port, null, transport, null);
    }

    public JDKProxy(Class<?> target, LoadBalancer loadBalancer, Transport transport, CircuitBreaker circuitBreaker) {
        this(target, null, -1, loadBalancer, transport, circuitBreaker);
    }

    public JDKProxy(Class<?> target, String ip, int port, LoadBalancer loadBalancer, Transport transport, CircuitBreaker circuitBreaker) {
        this.target = target;
        this.ip = ip;
        this.port = port;
        this.loadBalancer = loadBalancer;
        this.transport = transport;
        this.circuitBreaker = circuitBreaker;
    }

    public Object createProxy() {
        return Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target}, this);
    }




    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        long backoffSeconds = BACKOFF_PERIOD;

        // keep record of used nodes
        List<InetSocketAddress> invoked = new ArrayList<>(MAX_ATTEMPTS);

        for (int attempt = 1; ;) {

            // 1. get the latest endpoints from service registry
            List<InetSocketAddress> publishedList = loadBalancer.getAddresses(target.getName());

            // 2. filter out those have been tried
            List<InetSocketAddress> filteredList = publishedList.stream().filter(address -> !invoked.contains(address)).toList();

            // 3. filter addresses those circuit breaker is close or half opened
            List<InetSocketAddress> allowedList = filteredList.stream().filter(address -> circuitBreaker.allowRequest(address)).toList();

            // 4. address to send request
            InetSocketAddress address = loadBalancer.select(target.getName(), allowedList);

            CircuitBreaker.Permit permit = circuitBreaker.tryAcquire(address);

            if(permit == null){
                // keep searching
                continue;
            }

            try {
                // 4. send request
                Object result = transport.send(address, target, method, args);

                // 5. update circuit breaker via response
                circuitBreaker.update(address, true, permit);
                /*InetSocketAddress address = loadBalancer != null
                        ? loadBalancer.next(target.getName())
                        : new InetSocketAddress(ip, port);*/
                return result;
            } catch (ConnectException | WriteFailureException | TimeoutException e) {
                circuitBreaker.update(address, false, permit);
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
            } catch (Exception e) {
                // if business exception
                circuitBreaker.update(address, true, permit);
                throw e;
            } finally {
                invoked.add(address);
                attempt++;
            }
        }
    }
}
