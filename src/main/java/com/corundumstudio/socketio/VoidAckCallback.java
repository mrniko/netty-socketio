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
package com.corundumstudio.socketio;

/** Base ack callback with {@link Void} class as type. */
public abstract class VoidAckCallback extends AckCallback<Void> {

  public VoidAckCallback() {
    super(Void.class);
  }

  public VoidAckCallback(int timeout) {
    super(Void.class, timeout);
  }

  @Override
  public final void onSuccess(Void result) {
    onSuccess();
  }

  protected abstract void onSuccess();
}
