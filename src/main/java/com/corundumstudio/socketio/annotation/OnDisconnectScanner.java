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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.namespace.Namespace;

public class OnDisconnectScanner implements AnnotationScanner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Class<? extends Annotation> getScanAnnotation() {
        return OnDisconnect.class;
    }

    @Override
    public void addListener(Namespace namespace, final Object object, final Class clazz, final Method method) {
        namespace.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                try {
                    method.invoke(object, client);
                } catch (Exception e) {
                    log.error("Can't invoke OnDisconnect listener on: " + clazz.getName() + ", method: " + method.getName(), e);
                }
            }
        });
    }

    @Override
    public void validate(Method method, Class clazz) {
        if (method.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("Wrong OnDisconnect listener signature: " + clazz + "." + method.getName());
        }
        boolean valid = false;
        for (Class<?> eventType : method.getParameterTypes()) {
            if (eventType.equals(SocketIOClient.class)) {
                valid = true;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Wrong OnDisconnect listener signature: " + clazz + "." + method.getName());
        }
    }

}
