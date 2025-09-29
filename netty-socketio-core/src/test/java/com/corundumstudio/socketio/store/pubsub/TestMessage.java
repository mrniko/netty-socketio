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
package com.corundumstudio.socketio.store.pubsub;

import java.io.Serializable;

/**
 * Test message for testing purposes
 * This class is created as a separate file to avoid module access restrictions
 */
public class TestMessage extends PubSubMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String content;

    public TestMessage() {
        // Default constructor required for serialization
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "TestMessage{content='" + content + "', nodeId=" + getNodeId() + "}";
    }
}
