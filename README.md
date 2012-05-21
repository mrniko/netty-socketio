#Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.
Required JDK 1.5 (or above).

Licensed under the Apache License 2.0.

### Features

* Supports 0.7+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to latest - 0.9.6
* Supports xhr-polling transport
* Supports websocket transport (Hixie-75/76/Hybi-00, Hybi-10..Hybi-13)

#Usage example

##Server

	SocketIOListener handler = new SocketIOListener() {

		@Override
		public void onEvent(SocketIOClient client, Packet packet) {
			...
		}

		@Override
		public void onMessage(SocketIOClient client, Packet packet) {
                        // get a message
                        packet.getData().toString();
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
		public void onJsonObject(SocketIOClient client, Packet packet) {
                        // get a json object
                        packet.getData();

			...
                        SampleObject obj = new SampleObject();
                        // send object to socket.io client
                        client.sendJsonObject(obj);
		}
	};

	Configuration config = new Configuration();
	config.setHostname("localhost");
	config.setPort(81);
	config.setListener(handler);

	SocketIOServer server = new SocketIOServer(config);
	server.start();
	...
	
	server.stop();

##Client

        <script type="text/javascript" src="socket.io.js" charset="utf-8"></script>

        <script type="text/javascript">

               var socket = io.connect('http://localhost:81', {
                 'reconnection delay' : 2000,
                 'force new connection' : true
               });

               socket.on('message', function(data) {
                    // here is your handler on messages from server
               });

	       socket.on('connect', function() {
		    // connection established, now we can send an objects

                    // send json-object to server
                    var obj = ...
                    socket.json.send(obj);
	       });

        </script>