package com.fastrpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

public class JDKProxy implements InvocationHandler {
    private Class<?> target;
    private String ip;
    private int port;


    public JDKProxy(Class<?> target, String ip, int port) {
        this.target = target;
        this.ip = ip;
        this.port = port;
    }

    public Object createProxy() {

        return Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target}, this);
    }







    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 1. 需要创建对应服务的连接，
        // 2. 对同一个服务只创建一个连接。
        // 3. 记录服务名字和连接
        return null;
    }
}
