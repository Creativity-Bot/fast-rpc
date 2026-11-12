package com.fastrpc.autoconfigure;

import com.fastrpc.annotation.FrpcService;
import com.fastrpc.config.ServiceMap;
import com.fastrpc.config.ServiceRateLimiter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

public class FrpcServiceBeanPostProcessor implements BeanPostProcessor {

    private final ServiceMap serviceMap;
    private final ServiceRateLimiter serviceRateLimiter;
    private final double permitsPerSecond;
    private final double capacity;

    public FrpcServiceBeanPostProcessor(ServiceMap serviceMap, ServiceRateLimiter serviceRateLimiter,
                                        double permitsPerSecond, double capacity) {
        this.serviceMap = serviceMap;
        this.serviceRateLimiter = serviceRateLimiter;
        this.permitsPerSecond = permitsPerSecond;
        this.capacity = capacity;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        if (!type.isAnnotationPresent(FrpcService.class)) {
            return bean;
        }
        for (Class<?> iface : type.getInterfaces()) {
            serviceMap.register(iface.getName(), bean);
            serviceRateLimiter.register(iface.getName(), permitsPerSecond, capacity);
        }
        return bean;
    }
}
