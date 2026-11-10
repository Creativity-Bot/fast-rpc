package com.fastrpc.config;

import com.fastrpc.loadbalancer.LoadBalancer;
import com.fastrpc.loadbalancer.impl.RoundRobinLoadBalancer;
import com.fastrpc.registry.Registry;
import com.fastrpc.registry.impl.ZKRegistry;
import com.fastrpc.server.impl.NettyServer;
import com.fastrpc.transport.Transport;
import com.fastrpc.transport.impl.NettyClientTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FrpcConfig {

    @Bean
    public ServiceMap serviceMap() {
        return new ServiceMap();
    }

    @Bean
    public ServiceRateLimiter serviceRateLimiter(){
        return new ServiceRateLimiter();
    }

    @Bean
    public Registry registry() {
        return new ZKRegistry("127.0.0.1:2181");
    }

    @Bean
    public Transport transport() {
        return new NettyClientTransport();
    }

    @Bean
    public LoadBalancer loadBalancer() {
        return new RoundRobinLoadBalancer();
    }

    @Bean
    public NettyServer nettyServer(ServiceMap serviceMap, ServiceRateLimiter serviceRateLimiter) {
        return new NettyServer(4, 16, 16, serviceMap, serviceRateLimiter);
    }


}
