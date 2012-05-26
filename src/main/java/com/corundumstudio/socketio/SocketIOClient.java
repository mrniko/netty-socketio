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
package com.corundumstudio.socketio;

import java.net.SocketAddress;
import java.util.UUID;

public interface SocketIOClient extends ClientOperations {

    /**
     * Client session id, uses {@link UUID} object
     *
     * @return - session id
     */
    UUID getSessionId();

    /**
     * Get client remote address
     *
     * @return remote address
     */
    SocketAddress getRemoteAddress();

}
