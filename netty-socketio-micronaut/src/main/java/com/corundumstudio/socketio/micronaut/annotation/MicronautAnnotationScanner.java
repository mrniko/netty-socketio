/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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
package com.corundumstudio.socketio.micronaut.annotation;

import java.lang.reflect.Method;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import io.micronaut.context.BeanContext;
import io.micronaut.inject.BeanDefinition;

/**
 * Micronaut annotation scanner for Socket.IO event listeners.
 * This class scans for Micronaut beans annotated with Socket.IO event annotations
 * and registers them with the SocketIOServer.
 * <p>
 * Similar to Spring's SpringAnnotationScanner, this provides automatic discovery
 * and registration of event listeners in Micronaut applications.
 */
public class MicronautAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(MicronautAnnotationScanner.class);

    private final BeanContext beanContext;
    private final SocketIOServer socketIOServer;

    public MicronautAnnotationScanner(BeanContext beanContext, SocketIOServer socketIOServer) {
        this.beanContext = beanContext;
        this.socketIOServer = socketIOServer;
    }

    /**
     * Scans the Micronaut BeanContext for beans with Socket.IO event annotations
     * and registers them with the SocketIOServer.
     */
    public void scanAndRegister() {
        Collection<BeanDefinition<?>> allBeanDefinitions = beanContext.getAllBeanDefinitions();
        allBeanDefinitions.forEach(beanDefinition -> {
            Class<?> beanType = beanDefinition.getBeanType();
            if (hasSocketIOAnnotations(beanType)) {
                log.info("Found Socket.IO annotated bean: {}", beanType.getName());
                try {
                    Object bean = beanContext.getBean(beanType);
                    socketIOServer.addListeners(bean, beanType);
                    log.info("Added Socket.IO annotated bean: {}", beanType.getName());
                } catch (Exception e) {
                    log.error("Could not instantiate bean of type: {}", beanType.getName(), e);
                }
            }
        });
    }

    /**
     * Checks if the given class has any Socket.IO event listener annotations.
     *
     * @param beanClass the class to check for Socket.IO event listener annotations
     * @return {@code true} if any method in the class is annotated with {@link OnConnect}, {@link OnDisconnect}, or {@link OnEvent}; {@code false} otherwise
     */
    private boolean hasSocketIOAnnotations(Class<?> beanClass) {
        Method[] methods = beanClass.getDeclaredMethods();

        for (Method method : methods) {
            if (
                    method.isAnnotationPresent(OnConnect.class)
                            || method.isAnnotationPresent(OnDisconnect.class)
                            || method.isAnnotationPresent(OnEvent.class)
            ) {
                return true;
            }
        }

        return false;
    }
}