module netty.socketio {
  exports com.corundumstudio.socketio;
  exports com.corundumstudio.socketio.ack;
  exports com.corundumstudio.socketio.annotation;
  exports com.corundumstudio.socketio.handler;
  exports com.corundumstudio.socketio.listener;
  exports com.corundumstudio.socketio.namespace;
  exports com.corundumstudio.socketio.misc;
  exports com.corundumstudio.socketio.messages;
  exports com.corundumstudio.socketio.protocol;

  requires static spring.beans;
  requires static spring.core;

  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.databind;

  requires static com.hazelcast.core;
  requires static com.hazelcast.client;

  requires static redisson;

  requires static io.netty.transport.classes.epoll;
  requires io.netty.codec;
  requires io.netty.transport;
  requires io.netty.buffer;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.codec.http;
  requires org.slf4j;
}
