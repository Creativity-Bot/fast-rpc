package com.fastrpc;

import com.fastrpc.server.impl.NettyServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class FrpcApplication {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext("com.fastrpc");
        context.getBean(NettyServer.class).start();
    }
}
