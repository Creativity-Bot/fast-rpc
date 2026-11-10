package com.fastrpc.config;

import com.fastrpc.annotation.FrpcClient;
import com.fastrpc.loadbalancer.LoadBalancer;
import com.fastrpc.proxy.JDKProxy;
import com.fastrpc.registry.Registry;
import com.fastrpc.transport.Transport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Component
public class FrpcClientBeanPostProcessor implements BeanPostProcessor {

    private final Registry registry;
    private final Transport transport;
    private final LoadBalancer loadBalancer;
    private final Set<String> subscribedServices = new HashSet<>();

    public FrpcClientBeanPostProcessor(Registry registry, Transport transport, LoadBalancer loadBalancer) {
        this.registry = registry;
        this.transport = transport;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                FrpcClient frpcClient = field.getAnnotation(FrpcClient.class);
                if (frpcClient == null) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (!fieldType.isInterface()) {
                    throw new BeanCreationException(beanName, fieldType.getName() + " must be an interface");
                }
                String serviceName = fieldType.getName();
                if(!subscribedServices.contains(serviceName)){
                    registry.subscribeService(serviceName, addresses -> loadBalancer.accept(serviceName, addresses));
                    subscribedServices.add(serviceName);
                }
                Object proxy = new JDKProxy(fieldType, loadBalancer, transport).createProxy();
                ReflectionUtils.makeAccessible(field);
                try {
                    field.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    throw new BeanCreationException(beanName, "failed to inject " + field.getName(), e);
                }
            }
            type = type.getSuperclass();
        }
        return bean;
    }
}
