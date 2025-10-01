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
package com.corundumstudio.socketio.quarkus.config;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class DefaultSocketIOBeans {
    /**
     * Produce default ExceptionListener bean if none is provided by the user.
     * @return DefaultExceptionListener instance
     */
    @Produces
    @DefaultBean
    public ExceptionListener defaultExceptionListener() {
        return new DefaultExceptionListener();
    }

    /**
     * Produce default StoreFactory bean if none is provided by the user.
     * @return MemoryStoreFactory instance
     */
    @Produces
    @DefaultBean
    public StoreFactory defaultStoreFactory() {
        return new MemoryStoreFactory();
    }

    /**
     * Produce default JsonSupport bean if none is provided by the user.
     * @return JacksonJsonSupport instance
     */
    @Produces
    @DefaultBean
    public JsonSupport defaultJsonSupport() {
        return new JacksonJsonSupport();
    }

    /**
     * Produce default AuthorizationListener bean if none is provided by the user.
     * @return SuccessAuthorizationListener instance
     */
    @Produces
    @DefaultBean
    public AuthorizationListener defaultAuthorizationListener() {
        return new SuccessAuthorizationListener();
    }

}
