/**
 * Copyright 2012 Nikita Koksharov
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

import com.corundumstudio.socketio.SocketIOServer;

public class SpringAnnotationScanner implements BeanPostProcessor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<Class<? extends Annotation>> annotations =
                    Arrays.asList(OnConnect.class, OnDisconnect.class, OnEvent.class);

    private final SocketIOServer socketIOServer;

    private Class originalBeanClass;

    public SpringAnnotationScanner(SocketIOServer socketIOServer) {
        super();
        this.socketIOServer = socketIOServer;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (originalBeanClass != null) {
            socketIOServer.addListeners(bean, originalBeanClass);
            log.info("{} bean listeners added", beanName);
            originalBeanClass = null;
        }
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
        }
        return bean;
    }

}
