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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

public class Encoder {

	private ObjectMapper objectMapper;
	
	public Encoder(ObjectMapper objectMapper) {
		super();
		this.objectMapper = objectMapper;
	}
	
	public CharSequence encodePayload(List<String> packets) {
		if (packets.size() == 1) {
			return packets.get(0);
		}
		StringBuilder result = new StringBuilder();
		for (String packet : packets) {
			result.append('\ufffd').append(packet.length()).append('\ufffd').append(packet);
		}
		return result;
	}

	public String encodePacket(Packet packet) throws IOException {
		int type = packet.getType().getValue();
		String id = packet.getId();
		String endpoint = packet.getEndpoint();
		String ack = packet.getAck();
		Object data = packet.getData();
		
		switch(packet.getType()) {
		
			case MESSAGE:
				if (packet.getData() != null) {
					data = packet.getData();
				}
				break;
		
			case EVENT:
				Event event = new Event(packet.getName(), packet.getArgs());
				data = objectMapper.writeValueAsString(event);
				break;
			
			case JSON:
				data = objectMapper.writeValueAsString(packet.getData());
				break;
				
			case CONNECT:
				data = packet.getQs();
				break;
				
			case ACK:
				String dataStr = packet.getAckId();
				if (packet.getArgs() != null) {
					dataStr += "+" + objectMapper.writeValueAsString(packet.getArgs());
				}
				data = dataStr;
				break;
			
			case ERROR:
				int reasonCode = -1;
				int adviceCode = -1;
				if (packet.getReason() != null) {
					reasonCode = packet.getReason().getValue();
				}
				if (packet.getAdvice() != null) {
					adviceCode = packet.getAdvice().getValue();
				}
				
				StringBuilder errorData = new StringBuilder();
				if (reasonCode != -1 || adviceCode != -1) {
					if (reasonCode != -1) {
						errorData.append(reasonCode);
					}
					if (adviceCode != -1) {
						errorData.append("+").append(adviceCode);
					}
				}
				data = errorData;
				break;
				
		}
		
		List<Object> params = new ArrayList<Object>(4);
		params.add(type);
		if ("data".equals(ack)) {
			params.add(id + "+");
		} else {
			params.add(id);
		}
		params.add(endpoint);
		if (data != null) {
			params.add(data);
		}
		
		return join(":", params);
	}
	
	private String join(String delimiter, List<Object> args) {
		StringBuilder result = new StringBuilder();
		for (Object arg : args) {
			result.append(arg).append(delimiter);
		}
		return result.substring(0, result.length()-1);
	}
	
}
