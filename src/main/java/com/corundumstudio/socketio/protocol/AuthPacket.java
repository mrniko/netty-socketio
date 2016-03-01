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
package com.corundumstudio.socketio.protocol;

public class AuthPacket {

    private final Long sid;
    private final String[] upgrades;
    private final int pingInterval;
    private final int pingTimeout;

    public AuthPacket(Long sid, String[] upgrades, int pingInterval, int pingTimeout) {
        super();
        this.sid = sid;
        this.upgrades = upgrades;
        this.pingInterval = pingInterval;
        this.pingTimeout = pingTimeout;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public String getSid() {
        return Long.toString(sid);
    }

    public String[] getUpgrades() {
        return upgrades;
    }

}
