package com.corundumstudio.socketio.store.pubsub;

import java.io.Serializable;

/**
 * Test message for testing purposes This class is created as a separate file to avoid module access
 * restrictions
 */
public class TestMessage extends PubSubMessage implements Serializable {
  private static final long serialVersionUID = 1L;

  private String content;

  public TestMessage() {
    // Default constructor required for serialization
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "TestMessage{content='" + content + "', nodeId=" + getNodeId() + "}";
  }
}
