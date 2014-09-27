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

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.MultiTypeAckCallback;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JacksonJsonSupport implements JsonSupport {

    public static class HTMLCharacterEscapes extends CharacterEscapes
    {
        private static final long serialVersionUID = 6159367941232231133L;

        private final int[] asciiEscapes;

        public HTMLCharacterEscapes()
        {
            // start with set of characters known to require escaping (double-quote, backslash etc)
            int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
            // and force escaping of a few others:
            esc['"'] = CharacterEscapes.ESCAPE_CUSTOM;
            asciiEscapes = esc;
        }

        // this method gets called for character codes 0 - 127
        @Override
        public int[] getEscapeCodesForAscii() {
            return asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch) {
            if (ch == '"') {
                return new SerializedString("\\\\" + (char)ch);
            }
            return null;
        }
    }

    private class AckArgsDeserializer extends StdDeserializer<AckArgs> {

        private static final long serialVersionUID = 7810461017389946707L;

        protected AckArgsDeserializer() {
            super(AckArgs.class);
        }

        @Override
        public AckArgs deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            List<Object> args = new ArrayList<Object>();
            AckArgs result = new AckArgs(args);

            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            JsonNode root = mapper.readTree(jp);
            AckCallback<?> callback = currentAckClass.get();
            Iterator<JsonNode> iter = root.iterator();
            int i = 0;
            while (iter.hasNext()) {
                Object val;

                Class<?> clazz = callback.getResultClass();
                if (callback instanceof MultiTypeAckCallback) {
                    MultiTypeAckCallback multiTypeAckCallback = (MultiTypeAckCallback) callback;
                    clazz = multiTypeAckCallback.getResultClasses()[i];
                }

                JsonNode arg = iter.next();
                if (arg.isTextual() || arg.isBoolean()) {
                    clazz = Object.class;
                }

                val = mapper.treeToValue(arg, clazz);
                args.add(val);
                i++;
            }
            return result;
        }

    }

    private class EventDeserializer extends StdDeserializer<Event> {

        private static final long serialVersionUID = 8178797221017768689L;

        final Map<String, List<Class<?>>> eventMapping = new ConcurrentHashMap<String, List<Class<?>>>();

        protected EventDeserializer() {
            super(Event.class);
        }

        @Override
        public Event deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            ArrayNode root = (ArrayNode) mapper.readTree(jp);
            String eventName = root.get(0).asText();
            if (!eventMapping.containsKey(eventName)) {
                return new Event(eventName, Collections.emptyList());
            }

            List<Object> eventArgs = new ArrayList<Object>();
            Event event = new Event(eventName, eventArgs);
            if (root.size() > 1) {
                Iterator<JsonNode> iterator = root.elements();
                // skip 0 node
                iterator.next();
                List<Class<?>> eventClasses = eventMapping.get(eventName);
                int i = 0;
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (i > eventClasses.size() - 1) {
                        log.debug("Event {} has more args than declared in handler: {}", eventName, root);
                        break;
                    }
                    Class<?> eventClass = eventClasses.get(i);
                    Object arg = mapper.treeToValue(node, eventClass);
                    eventArgs.add(arg);
                    i++;
                }
            }
            return event;
        }

    }

    private final ThreadLocal<AckCallback<?>> currentAckClass = new ThreadLocal<AckCallback<?>>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper jsonpObjectMapper = new ObjectMapper();
    private final EventDeserializer eventDeserializer = new EventDeserializer();
    private final AckArgsDeserializer ackArgsDeserializer = new AckArgsDeserializer();

    private final Logger log = LoggerFactory.getLogger(getClass());

    public JacksonJsonSupport(Module... modules) {
        if (modules != null && modules.length > 0) {
            objectMapper.registerModules(modules);
            jsonpObjectMapper.registerModules(modules);
        }
        init(objectMapper);
        init(jsonpObjectMapper);
        jsonpObjectMapper.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
    }

    protected void init(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Event.class, eventDeserializer);
        module.addDeserializer(AckArgs.class, ackArgsDeserializer);
        objectMapper.registerModule(module);

        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);

//        TODO If jsonObjectDeserializer will be not enough
//        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL);
//        typer.init(JsonTypeInfo.Id.CLASS, null);
//        typer.inclusion(JsonTypeInfo.As.PROPERTY);
//        typer.typeProperty(configuration.getJsonTypeFieldName());
//        objectMapper.setDefaultTyping(typer);
    }

    @Override
    public void addEventMapping(String eventName, Class<?> ... eventClass) {
        eventDeserializer.eventMapping.put(eventName, Arrays.asList(eventClass));
    }

    @Override
    public void removeEventMapping(String eventName) {
        eventDeserializer.eventMapping.remove(eventName);
    }

    @Override
    public <T> T readValue(ByteBufInputStream src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }

    @Override
    public AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws IOException {
        currentAckClass.set(callback);
        return objectMapper.readValue(src, AckArgs.class);
    }

    @Override
    public void writeValue(ByteBufOutputStream out, Object value) throws IOException {
        objectMapper.writeValue(out, value);
    }

    @Override
    public void writeJsonpValue(ByteBufOutputStream out, Object value) throws IOException {
        jsonpObjectMapper.writeValue(out, value);
    }

    @Override
    public <T> T readValue(String src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }

    @Override
    public JsonSupport clone() {
        return new JacksonJsonSupport();
    }
}
