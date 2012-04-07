#Overview

This project is an open-source Java implementation of [Socket.IO](http://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/) server. Based on [Netty](http://netty.io/) server framework.
Licensed under the MIT license.

### Features

* Supports 0.7+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to latest - 0.9.5
* Supports xhr-polling transport

#Usage example

	SocketIOListener handler = new SocketIOListener() {

		@Override
		public void onMessage(SocketIOClient client, String message) {
			System.out.println("onMessage: " + message);
		}
	
		@Override
		public void onDisconnect(SocketIOClient client) {
			System.out.println("onDisconnect: " + client.getSessionId());
		}
	
		@Override
		public void onConnect(final SocketIOClient client) {
			System.out.println("onConnect: " + client.getSessionId());
		}

		@Override
		public void onJsonObject(SocketIOClient client, Object obj) {
			System.out.println("onJsonObject: " + obj);
		}
	};

	SocketIOServer server = new SocketIOServer();
	server.setHostname("localhost");
	server.setPort(81);
	server.setHeartbeatThreadPoolSize(8);
	server.setWorkerThreadPoolSize(8);
	server.setBossThreadPoolSize(4);
	server.setListener(handler);
	server.start();

	...
	
	server.stop();