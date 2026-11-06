package com.fastrpc.test;

import com.fastrpc.config.FrpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FrpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        log.debug("HelloServiceImpl invoked with parameter {}", name);
        return "hello, " + name;
    }
}
