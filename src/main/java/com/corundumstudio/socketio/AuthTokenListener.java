package com.corundumstudio.socketio;

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
