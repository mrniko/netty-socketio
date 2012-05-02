#Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.
Required JDK 1.5 (or above).

Licensed under the Apache License 2.0.

### Features

* Supports 0.8+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to latest - 0.9.5
* Supports xhr-polling transport

#Usage example

##Server

	SocketIOListener handler = new SocketIOListener() {

		@Override
		public void onMessage(SocketIOClient client, String message) {
			...
		}
	
		@Override
		public void onDisconnect(SocketIOClient client) {
			...
		}
	
		@Override
		public void onConnect(final SocketIOClient client) {
			...
		}

		@Override
		public void onJsonObject(SocketIOClient client, Object obj) {
			...
                        SampleObject obj = new SampleObject();
                        // send object to socket.io client
                        client.sendJsonObject(obj);
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

##Client

        <script type="text/javascript" src="socket.io.js" charset="utf-8"></script>

        <script type="text/javascript">
               var socket = io.connect('http://localhost:81', {
                 'transports' : [ 'xhr-polling' ],
                 'reconnection delay' : 2000,
                 'force new connection' : true
               });
               socket.on('message', function(data) {
                    // here is your handler on messages from server
               });

               // send json-object to server
               var obj = ...
               socket.json.send(obj);
        </script>