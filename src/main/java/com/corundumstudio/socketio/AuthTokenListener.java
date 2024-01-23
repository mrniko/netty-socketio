package com.corundumstudio.socketio;

/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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

public interface AuthTokenListener {

  /** Socket.IO clients from version 4 can offer an auth token when connecting
   * to a namespace. This listener gets invoked if a token is found in the connect
   * packet
   * @param authToken the token as parsed by the JSON parser
   * @param client client that is connecting
   * @return authorization result
   */
  AuthTokenResult getAuthTokenResult(Object authToken, SocketIOClient client);

}
