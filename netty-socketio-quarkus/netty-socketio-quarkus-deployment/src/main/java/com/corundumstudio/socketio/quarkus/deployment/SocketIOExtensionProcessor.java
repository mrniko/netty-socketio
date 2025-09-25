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
package com.corundumstudio.socketio.quarkus.deployment;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.quarkus.config.DefaultSocketIOBeans;
import com.corundumstudio.socketio.quarkus.lifecycle.SocketIOServerLifecycle;
import com.corundumstudio.socketio.quarkus.recorder.NettySocketIOConfigRecorder;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * SocketIO Quarkus 扩展处理器
 */
class SocketIOExtensionProcessor {

    private static final String FEATURE = "socketio-auto-registration";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem defaultSocketIOBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        DefaultSocketIOBeans.class
                )
                .setUnremovable()
                .build();
    }

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        return ReflectiveClassBuildItem.builder(
                OnConnect.class,
                OnDisconnect.class,
                OnEvent.class
        ).methods().build();
    }


    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void createSocketIOServer(
            NettySocketIOConfigRecorder nettySocketIOConfigRecorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemProducer
    ) {
        RuntimeValue<SocketIOServer> socketIOServer = nettySocketIOConfigRecorder.createSocketIOServer();

        syntheticBeanBuildItemProducer.produce(
                SyntheticBeanBuildItem.configure(SocketIOServer.class)
                        .scope(ApplicationScoped.class)
                        .runtimeValue(socketIOServer)
                        .setRuntimeInit()
                        .done()
        );
    }

    @BuildStep
    AdditionalBeanBuildItem socketIOServerLifecycle() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(SocketIOServerLifecycle.class)
                .setUnremovable()
                .build();
    }
}