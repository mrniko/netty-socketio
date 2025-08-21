/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.MultiTypeAckCallback;
import com.corundumstudio.socketio.namespace.Namespace;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.ArrayType;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.internal.PlatformDependent;

public class JacksonJsonSupport implements JsonSupport {

  protected class AckArgsDeserializer extends StdDeserializer<AckArgs> {

    private static final long serialVersionUID = 7810461017389946707L;

    protected AckArgsDeserializer() {
      super(AckArgs.class);
    }

    @Override
    public AckArgs deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
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

  public static class EventKey {

    private String namespaceName;
    private String eventName;

    public EventKey(String namespaceName, String eventName) {
      super();
      this.namespaceName = namespaceName;
      this.eventName = eventName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((eventName == null) ? 0 : eventName.hashCode());
      result = prime * result + ((namespaceName == null) ? 0 : namespaceName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      EventKey other = (EventKey) obj;
      if (eventName == null) {
        if (other.eventName != null) return false;
      } else if (!eventName.equals(other.eventName)) return false;
      if (namespaceName == null) {
        if (other.namespaceName != null) return false;
      } else if (!namespaceName.equals(other.namespaceName)) return false;
      return true;
    }
  }

  protected class EventDeserializer extends StdDeserializer<Event> {

    private static final long serialVersionUID = 8178797221017768689L;

    final Map<EventKey, List<Class<?>>> eventMapping = PlatformDependent.newConcurrentHashMap();

    protected EventDeserializer() {
      super(Event.class);
    }

    @Override
    public Event deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      ObjectMapper mapper = (ObjectMapper) jp.getCodec();
      String eventName = jp.nextTextValue();

      EventKey ek = new EventKey(namespaceClass.get(), eventName);
      if (!eventMapping.containsKey(ek)) {
        ek = new EventKey(Namespace.DEFAULT_NAME, eventName);
        if (!eventMapping.containsKey(ek)) {
          return new Event(eventName, Collections.emptyList());
        }
      }

      List<Object> eventArgs = new ArrayList<Object>();
      Event event = new Event(eventName, eventArgs);
      List<Class<?>> eventClasses = eventMapping.get(ek);
      int i = 0;
      while (true) {
        JsonToken token = jp.nextToken();
        if (token == JsonToken.END_ARRAY) {
          break;
        }
        if (i > eventClasses.size() - 1) {
          log.debug("Event {} has more args than declared in handler: {}", eventName, null);
          break;
        }
        Class<?> eventClass = eventClasses.get(i);
        Object arg = mapper.readValue(jp, eventClass);
        eventArgs.add(arg);
        i++;
      }
      return event;
    }
  }

  public static class ByteArraySerializer extends StdSerializer<byte[]> {

    private static final long serialVersionUID = 3420082888596468148L;

    private final ThreadLocal<List<byte[]>> arrays =
        new ThreadLocal<List<byte[]>>() {
          @Override
          protected List<byte[]> initialValue() {
            return new ArrayList<byte[]>();
          }
          ;
        };

    public ByteArraySerializer() {
      super(byte[].class);
    }

    @Override
    public boolean isEmpty(byte[] value) {
      return value == null || value.length == 0;
    }

    @Override
    public void serialize(byte[] value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("num", arrays.get().size());
      map.put("_placeholder", true);
      jgen.writeObject(map);
      arrays.get().add(value);
    }

    @Override
    public void serializeWithType(
        byte[] value, JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer)
        throws IOException, JsonGenerationException {
      serialize(value, jgen, provider);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
      ObjectNode o = createSchemaNode("array", true);
      ObjectNode itemSchema = createSchemaNode("string"); // binary values written as strings?
      return o.set("items", itemSchema);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
        throws JsonMappingException {
      if (visitor != null) {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
          v2.itemsFormat(JsonFormatTypes.STRING);
        }
      }
    }

    public List<byte[]> getArrays() {
      return arrays.get();
    }

    public void clear() {
      arrays.set(new ArrayList<byte[]>());
    }
  }

  protected static class ExBeanSerializerModifier extends BeanSerializerModifier {

    private final ByteArraySerializer serializer = new ByteArraySerializer();

    @Override
    public JsonSerializer<?> modifyArraySerializer(
        SerializationConfig config,
        ArrayType valueType,
        BeanDescription beanDesc,
        JsonSerializer<?> serializer) {
      if (valueType.getRawClass().equals(byte[].class)) {
        return this.serializer;
      }

      return super.modifyArraySerializer(config, valueType, beanDesc, serializer);
    }

    public ByteArraySerializer getSerializer() {
      return serializer;
    }
  }

  protected final ExBeanSerializerModifier modifier = new ExBeanSerializerModifier();
  protected final ThreadLocal<String> namespaceClass = new ThreadLocal<String>();
  protected final ThreadLocal<AckCallback<?>> currentAckClass = new ThreadLocal<AckCallback<?>>();
  protected final ObjectMapper objectMapper = new ObjectMapper();
  protected final EventDeserializer eventDeserializer = new EventDeserializer();
  protected final AckArgsDeserializer ackArgsDeserializer = new AckArgsDeserializer();

  protected static final Logger log = LoggerFactory.getLogger(JacksonJsonSupport.class);

  public JacksonJsonSupport() {
    this(new Module[] {});
  }

  public JacksonJsonSupport(Module... modules) {
    if (modules != null && modules.length > 0) {
      objectMapper.registerModules(modules);
    }
    init(objectMapper);
  }

  protected void init(ObjectMapper objectMapper) {
    SimpleModule module = new SimpleModule();
    module.setSerializerModifier(modifier);
    module.addDeserializer(Event.class, eventDeserializer);
    module.addDeserializer(AckArgs.class, ackArgsDeserializer);
    objectMapper.registerModule(module);

    objectMapper.setSerializationInclusion(Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
  }

  @Override
  public void addEventMapping(String namespaceName, String eventName, Class<?>... eventClass) {
    eventDeserializer.eventMapping.put(
        new EventKey(namespaceName, eventName), Arrays.asList(eventClass));
  }

  @Override
  public void removeEventMapping(String namespaceName, String eventName) {
    eventDeserializer.eventMapping.remove(new EventKey(namespaceName, eventName));
  }

  @Override
  public <T> T readValue(String namespaceName, ByteBufInputStream src, Class<T> valueType)
      throws IOException {
    namespaceClass.set(namespaceName);
    return objectMapper.readValue((InputStream) src, valueType);
  }

  @Override
  public AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws IOException {
    currentAckClass.set(callback);
    return objectMapper.readValue((InputStream) src, AckArgs.class);
  }

  @Override
  public void writeValue(ByteBufOutputStream out, Object value) throws IOException {
    modifier.getSerializer().clear();
    objectMapper.writeValue((OutputStream) out, value);
  }

  @Override
  public List<byte[]> getArrays() {
    return modifier.getSerializer().getArrays();
  }
}
