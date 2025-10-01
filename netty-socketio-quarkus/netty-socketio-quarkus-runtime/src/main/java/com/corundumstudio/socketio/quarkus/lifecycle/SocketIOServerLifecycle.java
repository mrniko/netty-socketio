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
package com.corundumstudio.socketio.quarkus.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;

/**
 * Manages the lifecycle of the SocketIOServer within a Quarkus application.
 * It starts the server on application startup and stops it on shutdown.
 * It also scans for beans with Socket.IO event listener annotations and registers them with the server.
 */
@ApplicationScoped
public class SocketIOServerLifecycle {
    private static final Logger log = Logger.getLogger(SocketIOServerLifecycle.class);

    private static final List<Class<? extends Annotation>> ALL_NETTY_SOCKET_IO_ANNOTATIONS =
            Arrays.asList(OnConnect.class, OnDisconnect.class, OnEvent.class);

    @Inject
    SocketIOServer socketIOServer;

    void onStart(@Observes StartupEvent ev) {
        BeanManager beanManager = CDI.current().getBeanManager();
        Set<Bean<?>> beans = CDI.current().getBeanManager().getBeans(Object.class);
        beans.forEach(bean -> {
            try {
                if (hasSocketIOAnnotations(bean.getBeanClass())) {
                    Object instance = beanManager.getReference(bean, Object.class,
                            beanManager.createCreationalContext(bean));
                    socketIOServer.addListeners(instance, instance.getClass());
                    log.info("SocketIO listeners registered for: " + instance.getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.warn("Failed to process bean: " + bean.getBeanClass().getName(), e);
            }
        });

        log.info("Starting SocketIO server...");
        socketIOServer.startAsync().addListener(future -> {
            if (future.isSuccess()) {
                log.info("SocketIO server started successfully on port: " + socketIOServer.getConfiguration().getPort());
            } else {
                log.error("Failed to start SocketIO server", future.cause());
            }
        });
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping SocketIO server...");
        socketIOServer.stop();
        log.info("SocketIO server stopped");
    }

    private boolean hasSocketIOAnnotations(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            for (Class<? extends Annotation> annotationClass : ALL_NETTY_SOCKET_IO_ANNOTATIONS) {
                if (method.isAnnotationPresent(annotationClass)) {
                    return true;
                }
            }
        }
        return false;
    }
}