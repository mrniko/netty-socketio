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
package com.corundumstudio.socketio.parser;

import com.corundumstudio.socketio.namespace.NamespacesHub;
import org.junit.Before;

import mockit.Mocked;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.protocol.PacketDecoder;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;


public class DecoderBaseTest {

    @Mocked
    protected AckManager ackManager;

    protected PacketDecoder decoder;

    @Before
    public void before() {
        decoder = new PacketDecoder(new JacksonJsonSupport(), ackManager);
    }

}
