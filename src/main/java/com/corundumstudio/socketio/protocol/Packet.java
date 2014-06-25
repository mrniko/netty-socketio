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

import java.io.Serializable;

import com.corundumstudio.socketio.namespace.Namespace;

public class Packet implements Serializable {

    private static final long serialVersionUID = 4560159536486711426L;

    private PacketType type;
    private PacketType subType;
    private Long ackId;
    private String name;
    private String nsp = Namespace.DEFAULT_NAME;
    private Object data;

    private ErrorReason reason;
    private ErrorAdvice advice;

    protected Packet() {
    }

    public Packet(PacketType type) {
        super();
        this.type = type;
    }

    public PacketType getSubType() {
        return subType;
    }

    public void setSubType(PacketType subType) {
        this.subType = subType;
    }

    public PacketType getType() {
        return type;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Get packet data
     * <pre>
     * @return <b>json object</b> for {@link PacketType.JSON} type
     * <b>message</b> for {@link PacketType.MESSAGE} type
     * </pre>
     */
    public <T> T getData() {
        return (T)data;
    }

    public void setNsp(String endpoint) {
        this.nsp = endpoint;
    }

    public String getNsp() {
        return nsp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAckId() {
        return ackId;
    }

    public void setAckId(Long ackId) {
        this.ackId = ackId;
    }

    public ErrorReason getReason() {
        return reason;
    }

    public void setReason(ErrorReason reason) {
        this.reason = reason;
    }

    public ErrorAdvice getAdvice() {
        return advice;
    }

    public void setAdvice(ErrorAdvice advice) {
        this.advice = advice;
    }

    public boolean isAckRequested() {
        return getAckId() != null && getSubType().equals(PacketType.EVENT);
    }

    @Override
    public String toString() {
        return "Packet [type=" + type + ", ackId=" + ackId + "]";
    }

}
