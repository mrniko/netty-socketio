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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.handler.SocketIOException;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.namespace.Namespace;

public class OnJsonObjectScanner implements AnnotationScanner {

    @Override
    public Class<? extends Annotation> getScanAnnotation() {
        return OnJsonObject.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addListener(Namespace namespace, final Object object, final Method method, Annotation annot) {
        final int socketIOClientIndex = paramIndex(method, SocketIOClient.class);
        final int ackRequestIndex = paramIndex(method, AckRequest.class);
        final int dataIndex = dataIndex(method);
        Class objectType = method.getParameterTypes()[dataIndex];
        namespace.addJsonObjectListener(objectType, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                try {
                    Object[] args = new Object[method.getParameterTypes().length];
                    if (socketIOClientIndex != -1) {
                        args[socketIOClientIndex] = client;
                    }
                    if (ackRequestIndex != -1) {
                        args[ackRequestIndex] = ackSender;
                    }
                    if (dataIndex != -1) {
                        args[dataIndex] = data;
                    }
                    method.invoke(object, args);
                } catch (InvocationTargetException e) {
                    throw new SocketIOException(e.getCause());
                } catch (Exception e) {
                    throw new SocketIOException(e);
                }
            }
        });
    }

    private int dataIndex(Method method) {
        int index = 0;
        for (Class type : method.getParameterTypes()) {
            if (!type.equals(AckRequest.class) && !type.equals(SocketIOClient.class)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private int paramIndex(Method method, Class clazz) {
        int index = 0;
        for (Class type : method.getParameterTypes()) {
            if (type.equals(clazz)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Override
    public void validate(Method method, Class clazz) {
        int paramsCount = 3;
        final int socketIOClientIndex = paramIndex(method, SocketIOClient.class);
        final int ackRequestIndex = paramIndex(method, AckRequest.class);
        final int dataIndex = dataIndex(method);
        if (dataIndex == -1) {
            throw new IllegalArgumentException("Wrong OnJsonObject listener signature: " + clazz + "." + method.getName() + " Data parameter is mandatory.");
        }
        if (socketIOClientIndex == -1) {
            paramsCount--;
        }
        if (ackRequestIndex == -1) {
            paramsCount--;
        }
        if (paramsCount != method.getParameterTypes().length) {
            throw new IllegalArgumentException("Wrong OnJsonObject listener signature: " + clazz + "." + method.getName());
        }
    }

}
