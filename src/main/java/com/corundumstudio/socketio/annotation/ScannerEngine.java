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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.namespace.Namespace;

public class ScannerEngine {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final List<? extends AnnotationScanner> annotations =
                    Arrays.asList(new OnConnectScanner(), new OnDisconnectScanner(),
                            new OnEventScanner(), new OnJsonObjectScanner(), new OnMessageScanner());

    private Method findSimilarMethod(Class<?> objectClazz, Method method) {
        Method[] methods = objectClazz.getDeclaredMethods();
        for (Method m : methods) {
            if (equals(m, method)) {
                return m;
            }
        }
        return null;
    }

    public void scan(Namespace namespace, Object object, Class<?> clazz)
            throws IllegalArgumentException {
        Method[] methods = clazz.getDeclaredMethods();

        if (!clazz.isAssignableFrom(object.getClass())) {
            for (Method method : methods) {
                for (AnnotationScanner annotationScanner : annotations) {
                    Annotation ann = method.getAnnotation(annotationScanner.getScanAnnotation());
                    if (ann != null) {
                        annotationScanner.validate(method, clazz);

                        Method m = findSimilarMethod(object.getClass(), method);
                        if (m != null) {
                            annotationScanner.addListener(namespace, object, m, ann);
                        } else {
                            log.warn("Method similar to " + method.getName() + " can't be found in " + object.getClass());
                        }
                    }
                }
            }
        } else {
            for (Method method : methods) {
                for (AnnotationScanner annotationScanner : annotations) {
                    Annotation ann = method.getAnnotation(annotationScanner.getScanAnnotation());
                    if (ann != null) {
                        annotationScanner.validate(method, clazz);
                        makeAccessible(method);
                        annotationScanner.addListener(namespace, object, method, ann);
                    }
                }
            }

            if (clazz.getSuperclass() != null) {
                scan(namespace, object, clazz.getSuperclass());
            } else if (clazz.isInterface()) {
                for (Class<?> superIfc : clazz.getInterfaces()) {
                    scan(namespace, object, superIfc);
                }
            }
        }

    }

    private boolean equals(Method method1, Method method2) {
        if (!method1.getName().equals(method2.getName())
                || !method1.getReturnType().equals(method2.getReturnType())) {
            return false;
        }

        return Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes());
    }

    private void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
                && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

}
