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
package com.corundumstudio.socketio.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.MultiTypeArgs;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.handler.SocketIOException;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.MultiTypeEventListener;
import com.corundumstudio.socketio.namespace.Namespace;

public class OnEventScanner implements AnnotationScanner {

    @Override
    public Class<? extends Annotation> getScanAnnotation() {
        return OnEvent.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addListener(Namespace namespace, final Object object, final Method method, Annotation annot) {
        OnEvent annotation = (OnEvent) annot;
        if (annotation.value() == null || annotation.value().trim().length() == 0) {
            throw new IllegalArgumentException("OnEvent \"value\" parameter is required");
        }
        final int socketIOClientIndex = paramIndex(method, SocketIOClient.class);
        final int ackRequestIndex = paramIndex(method, AckRequest.class);
        final List<Integer> dataIndexes = dataIndexes(method);

        if (dataIndexes.size() > 1) {
            List<Class<?>> classes = new ArrayList<Class<?>>();
            for (int index : dataIndexes) {
                Class<?> param = method.getParameterTypes()[index];
                classes.add(param);
            }

            namespace.addMultiTypeEventListener(annotation.value(), new MultiTypeEventListener() {
                @Override
                public void onData(SocketIOClient client, MultiTypeArgs data, AckRequest ackSender) {
                    try {
                        Object[] args = new Object[method.getParameterTypes().length];
                        if (socketIOClientIndex != -1) {
                            args[socketIOClientIndex] = client;
                        }
                        if (ackRequestIndex != -1) {
                            args[ackRequestIndex] = ackSender;
                        }
                        int i = 0;
                        for (int index : dataIndexes) {
                            args[index] = data.get(i);
                            i++;
                        }
                        method.invoke(object, args);
                    } catch (InvocationTargetException e) {
                        throw new SocketIOException(e.getCause());
                    } catch (Exception e) {
                        throw new SocketIOException(e);
                    }
                }
            }, classes.toArray(new Class[0]));
        } else {
            Class objectType = Void.class;
            if (!dataIndexes.isEmpty()) {
                objectType = method.getParameterTypes()[dataIndexes.iterator().next()];
            }

            namespace.addEventListener(annotation.value(), objectType, new DataListener<Object>() {
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
                        if (!dataIndexes.isEmpty()) {
                            int dataIndex = dataIndexes.iterator().next();
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
    }

    private List<Integer> dataIndexes(Method method) {
        List<Integer> result = new ArrayList<Integer>();
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (!type.equals(AckRequest.class) && !type.equals(SocketIOClient.class)) {
                result.add(index);
            }
            index++;
        }
        return result;
    }

    private int paramIndex(Method method, Class<?> clazz) {
        int index = 0;
        for (Class<?> type : method.getParameterTypes()) {
            if (type.equals(clazz)) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Override
    public void validate(Method method, Class<?> clazz) {
        int paramsCount = method.getParameterTypes().length;
        final int socketIOClientIndex = paramIndex(method, SocketIOClient.class);
        final int ackRequestIndex = paramIndex(method, AckRequest.class);
        List<Integer> dataIndexes = dataIndexes(method);
        paramsCount -= dataIndexes.size();
        if (socketIOClientIndex != -1) {
            paramsCount--;
        }
        if (ackRequestIndex != -1) {
            paramsCount--;
        }
        if (paramsCount != 0) {
            throw new IllegalArgumentException("Wrong OnEvent listener signature: " + clazz + "." + method.getName());
        }
    }

}
