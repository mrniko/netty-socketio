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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;

public class Decoder {

	private final Pattern packetPattern = Pattern.compile("([^:]+):([0-9]+)?(\\+)?:([^:]+)?:?([\\s\\S]*)?");
	private final Pattern ackPattern = Pattern.compile("^([0-9]+)(\\+)?(.*)");
	
	private final ObjectMapper objectMapper;
	
	public Decoder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public List<Packet> decodePayload(String data) throws IOException {
		List<Packet> result = new ArrayList<Packet>();
		if (data.charAt(0) == Packet.DELIMITER) {
			// TODO optimize
			for (int i = 1; i < data.length(); i++) {
				String length = "";
				if (data.charAt(i) == Packet.DELIMITER) {
					String packetStr = data.substring(i + 1, Integer.valueOf(length));
					Packet packet = decodePacket(packetStr);
					result.add(packet);
					i += Integer.valueOf(length) + 1;
				} else {
					length += data.charAt(i);
				}
			}
		} else {
			result.add(decodePacket(data));
		}
		return result;
		
	}

	public Packet decodePacket(String msg) throws IOException {
		Matcher matcher = packetPattern.matcher(msg);
		if (!matcher.matches()) {
			return Packet.NULL_INSTANCE;
		}
		String id = extract(matcher, 2);
		String data = extract(matcher, 5);
		int typeId = Integer.valueOf(matcher.group(1));
		PacketType type = PacketType.valueOf(typeId);
		String endpoint = extract(matcher, 4);
		
		Packet packet = new Packet(type);
		packet.setEndpoint(endpoint);
		packet.setId(id);
		if (id != null) {
			String ackData = extract(matcher, 3);
			if (ackData != null) {
				packet.setAck("data");
			} else {
				packet.setAck(""); // TODO ack = true ?
			}
		}
		
		switch (type) {
			case ERROR:
				String[] pieces = data.split("\\+");
				if (pieces.length > 0 && pieces[0].trim().length() > 0) {
					ErrorReason reason = ErrorReason.valueOf(Integer.valueOf(pieces[0]));
					packet.setReason(reason);
					if (pieces.length > 1) {
						ErrorAdvice advice = ErrorAdvice.valueOf(Integer.valueOf(pieces[1]));
						packet.setAdvice(advice);
					}
				}
				break;

			case MESSAGE:
				if (data != null) {
					packet.setData(data);
				} else {
					packet.setData("");
				}
				break;
				
			case EVENT:
				Event event = objectMapper.readValue(data, Event.class);
				packet.setName(event.getName());
				if (event.getArgs() != null) {
					packet.setArgs(event.getArgs());
				}
				break;
				
			case JSON:
				Object obj = objectMapper.readValue(data, Object.class);
				packet.setData(obj);
				break;
				
			case CONNECT:
				packet.setQs(data);
				break;
				
			case ACK:
				if (data != null) {
					Matcher ackMatcher = ackPattern.matcher(data);
					if (ackMatcher.matches()) {
						packet.setAckId(ackMatcher.group(1));
						String ackArgsJSON = extract(ackMatcher, 3);
						if (ackArgsJSON != null && ackArgsJSON.trim().length() > 0) {
							List<Object> args = objectMapper.readValue(ackArgsJSON, List.class);
							packet.setArgs(args);
						}
					}
				}
		        break;
		        
			case DISCONNECT:
			case HEARTBEAT:
				break;
		}
		
		return packet;
	}
	
	private String extract(Matcher matcher, int index) {
		if (index > matcher.groupCount()) {
			return null;
		}
		return matcher.group(index);
	}
	
}
