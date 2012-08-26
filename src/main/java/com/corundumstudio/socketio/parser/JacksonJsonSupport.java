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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.node.ObjectNode;

public class JacksonJsonSupport implements JsonSupport {

    private class EventDeserializer extends StdDeserializer<Event> {

        Map<String, Class<?>> eventMapping = new ConcurrentHashMap<String, Class<?>>();

        protected EventDeserializer() {
            super(Event.class);
        }

        @Override
        public Event deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            ObjectNode root = (ObjectNode) mapper.readTree(jp);
            String eventName = root.get("name").asText();

            List eventArgs = new ArrayList();
            Event event = new Event(eventName, eventArgs);
            JsonNode args = root.get("args");
            if (args != null) {
                Iterator<JsonNode> iterator = args.getElements();
                if (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    Class<?> eventClass = eventMapping.get(eventName);
                    Object arg = mapper.readValue(node, eventClass);
                    eventArgs.add(arg);
                    while (iterator.hasNext()) {
                        node = iterator.next();
                        arg = mapper.readValue(node, Object.class);
                        eventArgs.add(arg);
                    }
                }
            }
            return event;
        }

    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventDeserializer eventDeserializer = new EventDeserializer();

    public JacksonJsonSupport() {
        SimpleModule module = new SimpleModule("EventDeserializerModule", new Version(1, 0, 0, null));
        module.addDeserializer(Event.class, eventDeserializer);
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        objectMapper.registerModule(module);
    }

    public void addEventMapping(String eventName, Class<?> eventClass) {
        eventDeserializer.eventMapping.put(eventName, eventClass);
    }

    public void removeEventMapping(String eventName) {
        eventDeserializer.eventMapping.remove(eventName);
    }

    @Override
    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }

    @Override
    public void writeValue(OutputStream out, Object value) throws IOException {
        objectMapper.writeValue(out, value);

    }

    @Override
    public String writeValueAsString(Object value) throws IOException {
        return objectMapper.writeValueAsString(value);
    }

}
