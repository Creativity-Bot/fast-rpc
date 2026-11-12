package com.fastrpc.autoconfigure;

import com.fastrpc.config.ServiceMap;
import com.fastrpc.config.ServiceRateLimiter;
import com.fastrpc.loadbalancer.LoadBalancer;
import com.fastrpc.loadbalancer.impl.RoundRobinLoadBalancer;
import com.fastrpc.registry.Registry;
import com.fastrpc.registry.impl.ZKRegistry;
import com.fastrpc.server.impl.NettyServer;
import com.fastrpc.transport.Transport;
import com.fastrpc.transport.impl.NettyClientTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@AutoConfiguration
@ConditionalOnClass(NettyServer.class)
@EnableConfigurationProperties(FrpcProperties.class)
public class FrpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceMap serviceMap() {
        return new ServiceMap();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServiceRateLimiter serviceRateLimiter() {
        return new ServiceRateLimiter();
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public Registry registry(FrpcProperties properties) {
        return new ZKRegistry(properties.getRegistry().getAddress());
    }

    @Bean
    @ConditionalOnMissingBean
    public Transport transport() {
        return new NettyClientTransport();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer() {
        return new RoundRobinLoadBalancer();
    }

    @Bean
    @ConditionalOnMissingBean
    public FrpcClientBeanPostProcessor frpcClientBeanPostProcessor(Registry registry, Transport transport,
                                                                   LoadBalancer loadBalancer) {
        return new FrpcClientBeanPostProcessor(registry, transport, loadBalancer);
    }

    @Bean
    @ConditionalOnMissingBean
    public FrpcServiceBeanPostProcessor frpcServiceBeanPostProcessor(ServiceMap serviceMap,
                                                                     ServiceRateLimiter serviceRateLimiter,
                                                                     FrpcProperties properties) {
        FrpcProperties.RateLimit rateLimit = properties.getRateLimit();
        return new FrpcServiceBeanPostProcessor(serviceMap, serviceRateLimiter,
                rateLimit.getPermitsPerSecond(), rateLimit.getCapacity());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "fastrpc.server", name = "enabled", havingValue = "true")
    static class ServerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public NettyServer nettyServer(FrpcProperties properties, ServiceMap serviceMap,
                                       ServiceRateLimiter serviceRateLimiter, Registry registry) {
            FrpcProperties.Server server = properties.getServer();
            return new NettyServer(server.getWorkerThreads(), server.getHandlerThreads(), server.getServiceThreads(),
                    serviceMap, serviceRateLimiter, registry, server.getIp(), server.getPort(),
                    server.getReadIdleSeconds(), server.getWriteIdleSeconds(), TimeUnit.SECONDS);
        }

        @Bean
        public FrpcServerLifecycle frpcServerLifecycle(NettyServer nettyServer) {
            return new FrpcServerLifecycle(nettyServer);
        }
    }
}
