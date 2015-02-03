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
package com.corundumstudio.socketio;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.protocol.AckArgs;
import com.corundumstudio.socketio.protocol.JsonSupport;

class JsonSupportWrapper implements JsonSupport {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JsonSupport delegate;

    JsonSupportWrapper(JsonSupport delegate) {
        this.delegate = delegate;
    }

    public AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback) throws IOException {
        try {
            return delegate.readAckArgs(src, callback);
        } catch (Exception e) {
            src.reset();
            log.error("Can't read ack args: " + src.readLine() + " for type: " + callback.getResultClass(), e);
            return null;
        }
    }

    public <T> T readValue(String namespaceName, ByteBufInputStream src, Class<T> valueType) throws IOException {
        try {
            return delegate.readValue(namespaceName, src, valueType);
        } catch (Exception e) {
            src.reset();
            log.error("Can't read value: " + src.readLine() + " for type: " + valueType, e);
            return null;
        }
    }

    public void writeValue(ByteBufOutputStream out, Object value) throws IOException {
        try {
            delegate.writeValue(out, value);
        } catch (Exception e) {
            log.error("Can't write value: " + value, e);
        }
    }

    public void addEventMapping(String namespaceName, String eventName, Class<?> ... eventClass) {
        delegate.addEventMapping(namespaceName, eventName, eventClass);
    }

    public void removeEventMapping(String namespaceName, String eventName) {
        delegate.removeEventMapping(namespaceName, eventName);
    }

    @Override
    public List<byte[]> getArrays() {
        return delegate.getArrays();
    }

}
