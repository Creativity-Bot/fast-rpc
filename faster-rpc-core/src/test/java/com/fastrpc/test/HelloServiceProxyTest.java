package com.fastrpc.test;

import com.fastrpc.proxy.JDKProxy;

public class HelloServiceProxyTest {
    public static void main(String[] args) throws Throwable {
        int port = 9527;
        JDKProxy jdkProxy = new JDKProxy(HelloService.class, "localhost", port);
        HelloService helloService = (HelloService) jdkProxy.createProxy();
        String result = helloService.hello("faster-rpc");
        System.out.println(result);
    }
}
