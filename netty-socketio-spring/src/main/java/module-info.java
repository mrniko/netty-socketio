module netty.socketio.spring {
  exports com.corundumstudio.socketio.spring;

  requires netty.socketio.core;
  requires static spring.beans;
  requires static spring.core;
  requires org.slf4j;
}
