/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.corundumstudio.socketio.listener.ClientListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.corundumstudio.socketio.SocketIOServer;

public class SpringAnnotationScanner implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SpringAnnotationScanner.class);

    private final List<Class<? extends Annotation>> annotations =
                    Arrays.asList(OnConnect.class, OnDisconnect.class, OnEvent.class);

    private final SocketIOServer socketIOServer;

    private Class originalBeanClass;
    private Namespace namespaceAnnotation;

    public SpringAnnotationScanner(SocketIOServer socketIOServer) {
        super();
        this.socketIOServer = socketIOServer;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (originalBeanClass == null) {
            return bean;
        }

        ClientListeners listeners;
        if (namespaceAnnotation == null) {
            listeners = socketIOServer;
        } else {
            ClientListeners namespace = socketIOServer.getNamespace(namespaceAnnotation.value());
            if (namespace == null) {
                namespace = socketIOServer.addNamespace(namespaceAnnotation.value());
                log.info("{} namespace created", namespaceAnnotation.value());
            }
            listeners = namespace;
            namespaceAnnotation = null;
        }

        listeners.addListeners(bean, originalBeanClass);
        log.info("{} bean listeners added", beanName);
        originalBeanClass = null;

        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        final AtomicBoolean add = new AtomicBoolean();
        ReflectionUtils.doWithMethods(bean.getClass(),
                new MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException,
            IllegalAccessException {
                add.set(true);
            }
        },
        new MethodFilter() {
            @Override
            public boolean matches(Method method) {
                for (Class<? extends Annotation> annotationClass : annotations) {
                    if (method.isAnnotationPresent(annotationClass)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if (add.get()) {
            originalBeanClass = bean.getClass();
            if (bean.getClass().isAnnotationPresent(Namespace.class)) {
                namespaceAnnotation = bean.getClass().getAnnotation(Namespace.class);
            }
        }

        return bean;
    }

}
