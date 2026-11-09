package com.fastrpc.config;

import com.fastrpc.server.impl.NettyServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FrpcConfig {

    @Bean
    public ServiceRegistry serviceRegistry() {
        return new ServiceRegistry();
    }

    @Bean
    public ServiceRateLimiter serviceRateLimiter(){
        return new ServiceRateLimiter();
    }

    @Bean
    public NettyServer nettyServer(ServiceRegistry serviceRegistry, ServiceRateLimiter serviceRateLimiter) {
        return new NettyServer(4, 16, 16, serviceRegistry, serviceRateLimiter);
    }


}
