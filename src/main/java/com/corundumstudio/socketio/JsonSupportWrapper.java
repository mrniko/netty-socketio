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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.AckArgs;
import com.corundumstudio.socketio.parser.JsonSupport;

class JsonSupportWrapper implements JsonSupport {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final JsonSupport delegate;

    JsonSupportWrapper(JsonSupport delegate) {
        this.delegate = delegate;
    }

    public AckArgs readAckArgs(ByteBufInputStream src, Class<?> argType) throws IOException {
        try {
            return delegate.readAckArgs(src, argType);
        } catch (IOException e) {
            src.reset();
            log.error("Can't read ack args: " + src.readLine() + " for type: " + argType, e);
            return null;
        } catch (RuntimeException e) {
            src.reset();
            log.error("Can't read ack args: " + src.readLine() + " for type: " + argType, e);
            return null;
        }
    }

    public <T> T readValue(ByteBufInputStream src, Class<T> valueType) throws IOException {
        try {
            return delegate.readValue(src, valueType);
        } catch (IOException e) {
            src.reset();
            log.error("Can't read value 1: " + src.readLine() + " for type: " + valueType, e);
            return null;
        } catch (RuntimeException e) {
            src.reset();
            log.error("Can't read value 2: " + src.readLine() + " for type: " + valueType, e);
            return null;
        }
    }

    public void writeValue(ByteBufOutputStream out, Object value) throws IOException {
        try {
            delegate.writeValue(out, value);
        } catch (IOException e) {
            log.error("Can't write value: " + value, e);
        } catch (RuntimeException e) {
            log.error("Can't write value: " + value, e);
        }
    }

    public String writeValueAsString(Object value) throws IOException {
        try {
            return delegate.writeValueAsString(value);
        } catch (IOException e) {
            log.error("Can't convert value to string: " + value, e);
            return null;
        } catch (RuntimeException e) {
            log.error("Can't convert value to string: " + value, e);
            return null;
        }
    }

    public void addEventMapping(String eventName, Class<?> eventClass) {
        delegate.addEventMapping(eventName, eventClass);
    }

    public void addJsonClass(Class<?> clazz) {
        delegate.addJsonClass(clazz);
    }

    public void removeEventMapping(String eventName) {
        delegate.removeEventMapping(eventName);
    }



}
