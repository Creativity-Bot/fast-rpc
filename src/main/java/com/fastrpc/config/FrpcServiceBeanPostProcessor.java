package com.fastrpc.config;

import com.fastrpc.annotation.FrpcService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class FrpcServiceBeanPostProcessor implements BeanPostProcessor {

    private final ServiceRegistry serviceRegistry;
    private final ServiceRateLimiter serviceRateLimiter;

    public FrpcServiceBeanPostProcessor(ServiceRegistry serviceRegistry, ServiceRateLimiter serviceRateLimiter) {
        this.serviceRegistry = serviceRegistry;
        this.serviceRateLimiter = serviceRateLimiter;
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        if (!type.isAnnotationPresent(FrpcService.class)) {
            return bean;
        }
        for (Class<?> iface : type.getInterfaces()) {
            serviceRegistry.register(iface.getName(), bean);
            serviceRateLimiter.register(iface.getName());
        }
        return bean;
    }
}
