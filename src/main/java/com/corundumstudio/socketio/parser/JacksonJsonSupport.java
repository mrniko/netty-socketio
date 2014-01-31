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

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.MultiTypeAckCallback;
import com.corundumstudio.socketio.misc.ConcurrentHashSet;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JacksonJsonSupport implements JsonSupport {

    private class JsonObjectDeserializer extends StdDeserializer<JsonObject> {

        private static final long serialVersionUID = 8025939196914136933L;

        final Set<Class<?>> classes = new ConcurrentHashSet<Class<?>>();

        protected JsonObjectDeserializer() {
            super(JsonObject.class);
        }

        @Override
        public JsonObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
                JsonProcessingException {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            JsonNode rootNode = mapper.readTree(jp);
            if (!rootNode.isObject()) {
                return null;
            }

            Object val = readObject(mapper, rootNode);
            return new JsonObject(val);
        }

        private Object readObject(ObjectMapper mapper, JsonNode rootNode) throws IOException,
                JsonParseException, JsonMappingException {
            Class<?> clazz = Object.class;
            ObjectNode root = (ObjectNode) rootNode;
            JsonNode node = root.remove(configuration.getJsonTypeFieldName());
            if (node != null) {
                try {
                    String typeName = node.asText();
                    if (configuration.getPackagePrefix() != null) {
                        typeName = configuration.getPackagePrefix() + "." + typeName;
                    }
                    Class<?> supportClazz = Class.forName(typeName);
                    if (classes.contains(supportClazz)) {
                        clazz = supportClazz;
                    }
                } catch (ClassNotFoundException e) {
                    // skip it
                }
            }
            Object val = mapper.treeToValue(root, clazz);
            return val;
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

                // TODO refactor it!
//                if (arg.isNumber()) {
//                    if (clazz.equals(Long.class)) {
//                        val = arg.longValue();
//                    }
//                    if (clazz.equals(BigDecimal.class)) {
//                        val = arg.bigIntegerValue();
//                    }
//                    if (clazz.equals(Double.class)) {
//                        val = arg.doubleValue();
//                    }
//                    if (clazz.equals(Integer.class)) {
//                        val = arg.intValue();
//                    }
//                    if (clazz.equals(Float.class)) {
//                        val = (float)arg.doubleValue();
//                    }
//                }
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
            ObjectNode root = (ObjectNode) mapper.readTree(jp);
            String eventName = root.get("name").asText();
            if (!eventMapping.containsKey(eventName)) {
                return new Event(eventName, Collections.emptyList());
            }

            List<Object> eventArgs = new ArrayList<Object>();
            Event event = new Event(eventName, eventArgs);
            JsonNode args = root.get("args");
            if (args != null) {
                Iterator<JsonNode> iterator = args.elements();
                List<Class<?>> eventClasses = eventMapping.get(eventName);
                int i = 0;
                while (iterator.hasNext()) {
                    JsonNode node = iterator.next();
                    if (i > eventClasses.size() - 1) {
                        log.debug("Event {} has more args than declared in handler: {}", eventName, args);
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
    private final Configuration configuration;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventDeserializer eventDeserializer = new EventDeserializer();
    private final JsonObjectDeserializer jsonObjectDeserializer = new JsonObjectDeserializer();
    private final AckArgsDeserializer ackArgsDeserializer = new AckArgsDeserializer();

    private final Logger log = LoggerFactory.getLogger(getClass());

    public JacksonJsonSupport(Configuration configuration) {
        this(configuration, null);
    }

    public JacksonJsonSupport(Configuration configuration, Module... modules) {
        this.configuration = configuration;

        if (modules != null && modules.length > 0) {
            objectMapper.registerModules(modules);
        }
        init(objectMapper);
    }

    protected void init(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Event.class, eventDeserializer);
        module.addDeserializer(JsonObject.class, jsonObjectDeserializer);
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
    public void addJsonClass(Class<?> clazz) {
        jsonObjectDeserializer.classes.add(clazz);
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
    public String writeValueAsString(Object value) throws IOException {
        return objectMapper.writeValueAsString(value);
    }

    @Override
    public <T> T readValue(String src, Class<T> valueType) throws IOException {
        return objectMapper.readValue(src, valueType);
    }


}
