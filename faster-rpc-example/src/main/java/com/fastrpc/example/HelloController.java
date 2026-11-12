package com.fastrpc.example;

import com.fastrpc.annotation.FrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @FrpcClient
    private HelloService helloService;

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "faster-rpc") String name) {
        return helloService.hello(name);
    }
}
