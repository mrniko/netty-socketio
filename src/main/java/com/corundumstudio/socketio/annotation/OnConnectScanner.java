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
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.namespace.Namespace;

public class OnConnectScanner implements AnnotationScanner  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public Class<? extends Annotation> getScanAnnotation() {
        return OnConnect.class;
    }

    public void addListener(Namespace namespace, final Object object, final Class clazz, final Method method) {
        namespace.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                try {
                    method.invoke(object, client);
                } catch (Exception e) {
                    log.error("Can't invoke OnConnect listener on: " + clazz.getName() + ", method: " + method.getName(), e);
                }
            }
        });
    }

    public void validate(Method method, Class clazz) {
        if (method.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("Wrong OnConnect listener signature: " + clazz + "." + method.getName());
        }
        boolean valid = false;
        for (Class<?> eventType : method.getParameterTypes()) {
            if (eventType.equals(SocketIOClient.class)) {
                valid = true;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException("Wrong OnConnect listener signature: " + clazz + "." + method.getName());
        }
    }

}
