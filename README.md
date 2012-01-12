This project is inspired by https://github.com/ibdknox/socket.io-netty work.
Supports 0.8.7+ version of socket.io-client https://github.com/LearnBoost/socket.io-client
Currently it supports only xhr-polling transport.

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
	server.setObjectMapper(createObjectMapper());
	server.setListener(handler);
	server.start();
