/**
 * Copyright (c) 2012-2019 Nikita Koksharov
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

import io.netty.buffer.ByteBuf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.corundumstudio.socketio.namespace.Namespace;

public class Packet implements Serializable {

    private static final long serialVersionUID = 4560159536486711426L;

    private PacketType type;
    private EngineIOVersion engineIOVersion;
    private PacketType subType;
    private Long ackId;
    private String name;
    private String nsp = Namespace.DEFAULT_NAME;
    private Object data;

    private ByteBuf dataSource;
    private int attachmentsCount;
    private List<ByteBuf> attachments = Collections.emptyList();

    protected Packet() {
    }

    //only for tests
    public Packet(PacketType type) {
        super();
        this.type = type;
    }
    public Packet(PacketType type, EngineIOVersion engineIOVersion) {
        this(type);
        this.engineIOVersion = engineIOVersion;
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
     * 
     * @param <T> the type data
     * 
     * <pre>
     * @return <b>json object</b> for PacketType.JSON type
     * <b>message</b> for PacketType.MESSAGE type
     * </pre>
     */
    public <T> T getData() {
        return (T)data;
    }

    /**
     * Creates a copy of #{@link Packet} with new namespace set
     * if it differs from current namespace.
     * Otherwise, returns original object unchanged
     *
     * @param namespace
     * @param engineIOVersion
     * @return packet
     */
    public Packet withNsp(String namespace, EngineIOVersion engineIOVersion) {
        if (this.nsp.equalsIgnoreCase(namespace)) {
            return this;
        } else {
            Packet newPacket = new Packet(this.type, engineIOVersion);
            newPacket.setAckId(this.ackId);
            newPacket.setData(this.data);
            newPacket.setDataSource(this.dataSource);
            newPacket.setName(this.name);
            newPacket.setSubType(this.subType);
            newPacket.setNsp(namespace);
            newPacket.attachments = this.attachments;
            newPacket.attachmentsCount = this.attachmentsCount;
            return newPacket;
        }
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

    public boolean isAckRequested() {
        return getAckId() != null;
    }

    public void initAttachments(int attachmentsCount) {
        this.attachmentsCount = attachmentsCount;
        this.attachments = new ArrayList<ByteBuf>(attachmentsCount);
    }
    public void addAttachment(ByteBuf attachment) {
        if (this.attachments.size() < attachmentsCount) {
            this.attachments.add(attachment);
        }
    }
    public List<ByteBuf> getAttachments() {
        return attachments;
    }
    public boolean hasAttachments() {
        return attachmentsCount != 0;
    }
    public boolean isAttachmentsLoaded() {
        return this.attachments.size() == attachmentsCount;
    }

    public ByteBuf getDataSource() {
        return dataSource;
    }
    public void setDataSource(ByteBuf dataSource) {
        this.dataSource = dataSource;
    }

    public EngineIOVersion getEngineIOVersion() {
        return engineIOVersion;
    }

    public void setEngineIOVersion(EngineIOVersion engineIOVersion) {
        this.engineIOVersion = engineIOVersion;
    }

    @Override
    public String toString() {
        return "Packet [type=" + type + ", ackId=" + ackId + "]";
    }

}
