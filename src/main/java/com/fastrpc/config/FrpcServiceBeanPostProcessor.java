package com.fastrpc.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class FrpcServiceBeanPostProcessor implements BeanPostProcessor {

    private final ServiceRegistry serviceRegistry;

    public FrpcServiceBeanPostProcessor(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        if (!type.isAnnotationPresent(FrpcService.class)) {
            return bean;
        }
        for (Class<?> iface : type.getInterfaces()) {
            serviceRegistry.register(iface.getName(), bean);
        }
        return bean;
    }
}
