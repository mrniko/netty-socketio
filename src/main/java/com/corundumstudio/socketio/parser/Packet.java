/**
 * Copyright (c) 2012 Nikita Koksharov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.corundumstudio.socketio.parser;

import java.util.List;

public class Packet {

	public static final Packet NULL_INSTANCE = new Packet(null);
	
	private final PacketType type;
	private List<String> args = null;
	private String qs;
	private String ack;
	private String ackId;
	private String name;
	private String id = "";
	private String endpoint = "";
	private Object data;
	
	private ErrorReason reason;
	private ErrorAdvice advice;
	
	public Packet(PacketType type) {
		super();
		this.type = type;
	}
	
	public PacketType getType() {
		return type;
	}
	
	public void setData(Object data) {
		this.data = data;
	}
	public Object getData() {
		return data;
	}
	
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getEndpoint() {
		return endpoint;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public void setAck(String ack) {
		this.ack = ack;
	}
	public String getAck() {
		return ack;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public List<String> getArgs() {
		return args;
	}
	public void setArgs(List<String> args) {
		this.args = args;
	}

	public String getQs() {
		return qs;
	}
	public void setQs(String qs) {
		this.qs = qs;
	}

	public String getAckId() {
		return ackId;
	}
	public void setAckId(String ackId) {
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
	
}
