package com.fastrpc.config;

import com.fastrpc.proxy.JDKProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

@Component
public class FrpcClientBeanPostProcessor implements BeanPostProcessor {

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
                Object proxy = new JDKProxy(fieldType, frpcClient.ip(), frpcClient.port()).createProxy();
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
