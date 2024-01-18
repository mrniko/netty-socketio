package com.corundumstudio.socketio;

public class AuthTokenResult {


  public final static AuthTokenResult AuthTokenResultSuccess = new AuthTokenResult(true, null);
  private final boolean success;
  private final Object errorData;

  public AuthTokenResult(final boolean success, final Object errorData) {
    this.success = success;
    this.errorData = errorData;
  }

  public boolean isSuccess() {
    return success;
  }

  public Object getErrorData() {
    return errorData;
  }
}
