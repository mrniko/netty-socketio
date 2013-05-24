#Netty-socketio Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.
Required JDK 1.5 (or above).

Checkout [Demo project](https://github.com/mrniko/netty-socketio-demo)

Licensed under the Apache License 2.0.

Features
================================
* Supports 0.7+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to latest - 0.9.11
* Supports xhr-polling transport
* Supports flashsocket transport
* Supports websocket transport (Hixie-75/76/Hybi-00, Hybi-10..Hybi-13)
* Supports namespaces
* Supports ack (acknowledgment of received data)
* Supports SSL
* Lock-free implementation
* Declarative handler configuration via annotations


Usage example
================================
##Server

Base configuration. More details about Configuration object is [here](https://github.com/mrniko/netty-socketio/wiki/Configuration-details).

        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(81);

        SocketIOServer server = new SocketIOServer(config);
        
Programmatic handlers binding:
        
        server.addMessageListener(new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String message, AckRequest ackRequest) {
                ...
            }
        });

        server.addEventListener("someevent", SomeClass.class, new DataListener<SomeClass>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackRequest) {
                ...
            }
        });

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                ...
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                ...
            }
        });


        // Don't forget to include type field on javascript side,
        // it named '@class' by default and should equals to full class name.
        //
        // TIP: you can customize type field name via Configuration.jsonTypeFieldName property.

        server.addJsonObjectListener(SomeClass.class, new DataListener<SomeClass>() {
            @Override
            public void onData(SocketIOClient client, SomeClass data, AckRequest ackRequest) {

                ...

                // send object to socket.io client
                SampleObject obj = new SampleObject();
                client.sendJsonObject(obj);
            }
        });
        
Declarative handlers binding. Handlers could be bound via annotations on any object:

        pubic class SomeBusinessService {
        
             ...
             // some stuff code
             ...

             // SocketIOClient, AckRequest and Data could be ommited
             @OnEvent('someevent')
             public void onSomeEventHandler(SocketIOClient client, SomeClass data, AckRequest ackRequest) {
                 ...
             }
             
             @OnConnect
             public void onConnectHandler(SocketIOClient client) {
                 ...
             }

             @OnDisconnect
             public void onDisconnectHandler(SocketIOClient client) {
                 ...
             }

             // only data object is required in arguments, 
             // SocketIOClient and AckRequest could be ommited
             @OnJsonObject
             public void onSomeEventHandler(SocketIOClient client, SomeClass data, AckRequest ackRequest) {
                 ...
             }

             // only data object is required in arguments, 
             // SocketIOClient and AckRequest could be ommited
             @OnMessage
             public void onSomeEventHandler(SocketIOClient client, String data, AckRequest ackRequest) {
                 ...
             }

        }
        
        SomeBusinessService someService = new SomeBusinessService();
        server.addListeners(someService);


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
                    // '@class' property should be defined and should 
                    // equals to full class name.
                    var obj = { '@class' : 'com.sample.SomeClass',
                                 ...
                              };
                    socket.json.send(obj);



                    // send event-object to server
                    // '@class' property is NOT necessary in this case
                    var event = { 
                                 ...
                              };
                    socket.emit('someevent', event);

	       });

        </script>
