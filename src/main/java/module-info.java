module netty.socketio {
  exports com.zizzle.socketio;
  exports com.zizzle.socketio.ack;
  exports com.zizzle.socketio.annotation;
  exports com.zizzle.socketio.handler;
  exports com.zizzle.socketio.listener;
  exports com.zizzle.socketio.namespace;
  exports com.zizzle.socketio.misc;
  exports com.zizzle.socketio.messages;
  exports com.zizzle.socketio.protocol;

  requires static spring.beans;
  requires static spring.core;

  requires com.fasterxml.jackson.core;
  requires com.fasterxml.jackson.annotation;
  requires com.fasterxml.jackson.databind;

  requires static com.hazelcast.core;

  requires static redisson;

  requires static io.netty.transport.classes.epoll;
  requires io.netty.codec;
  requires io.netty.handler;
  requires io.netty.codec.http;
    requires io.netty.transport.classes.io_uring;
    requires io.netty.transport;
    requires io.netty.buffer;
    requires io.netty.common;
    requires org.slf4j;
}
