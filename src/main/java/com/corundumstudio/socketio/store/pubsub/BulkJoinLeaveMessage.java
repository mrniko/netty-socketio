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

import java.util.Set;
import java.util.UUID;

public class BulkJoinLeaveMessage extends PubSubMessage {

    private static final long serialVersionUID = 7506016762607624388L;

    private UUID sessionId;
    private String namespace;
    private Set<String> rooms;

    public BulkJoinLeaveMessage() {
    }

    public BulkJoinLeaveMessage(UUID id, Set<String> rooms, String namespace) {
        super();
        this.sessionId = id;
        this.rooms = rooms;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Set<String> getRooms() {
        return rooms;
    }

}
