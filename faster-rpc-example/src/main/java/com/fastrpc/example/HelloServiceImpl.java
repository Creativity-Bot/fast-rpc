package com.fastrpc.example;

import com.fastrpc.annotation.FrpcService;

@FrpcService
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello " + name;
    }
}
