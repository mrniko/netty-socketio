#Netty-socketio Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.  

Checkout [Demo project](https://github.com/mrniko/netty-socketio-demo)

Licensed under the Apache License 2.0.


Features
================================
* Supports 0.7+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to latest
* Supports xhr-polling transport
* Supports flashsocket transport
* Supports websocket transport: HyBi 00 (which is the same as Hixie 76), HyBi 8-10 and HyBi 13-17 (17 is the same as IETF 6455).
* Supports namespaces and rooms
* Supports ack (acknowledgment of received data)
* Supports SSL
* Supports client store (Memory, Redis, Hazelcast)
* Supports distributed broadcast across netty-socketio nodes (Redis, Hazelcast)
* Supports OSGi
* Lock-free and thread-safe implementation
* Declarative handler configuration via annotations


Performance
================================

CentOS, 1 CPU, 4GB RAM runned on VM (based on customer report, tested in 2012): 

CPU 10%, Memory 15%  
6000 xhr-long polling sessions or 15000 websockets sessions  
4000 messages per second  

Recent Releases
================================
####Please Note: trunk is current development branch.

####19-Dec-2013 - version 1.6.0 released (JDK 1.6+ compatible, Netty 4.0.13)
Fixed - XHR-pooling transport regression  
Fixed - Websocket transport regression  
Fixed - namespace NPE in PacketHandler  
Fixed - shutdown executors during server stop added  
Feature - client store (Memory, Redis, Hazelcast) support  
Feature - distributed broadcast across netty-socketio nodes (Redis, Hazelcast) support  
Feature - OSGi support (thanks to rdevera)  
Improvement - XHR-pooling optimization  
Improvement - SocketIOClient.getAllRooms method added

####07-Dec-2013 - version 1.5.4 released (JDK 1.6+ compatible, Netty 4.0.13)
Fixed - flash policy "request leak" after page reload (thanks to ntrp)  
Fixed - websocket swf loading (thanks to ntrp)  
Fixed - wrong urls causes a potential DDoS  
Fixed - Event.class package visibility changed to avoid direct usage  
Improvement - Simplified Jackson modules registration

####24-Oct-2013 - version 1.5.2 released (JDK 1.6+ compatible, Netty 4.0.11)
Fixed - NPE during shutdown  
Improvement - isEmpty method added to Namespace

####13-Oct-2013 - version 1.5.1 released (JDK 1.6+ compatible, Netty 4.0.9)
Fixed - wrong ack timeout callback invocation  
Fixed - bigdecimal serialization for JSON  
Fixed - infinity loop during packet handling exception  
Fixed - 'client not found' handling  

####27-Aug-2013 - version 1.5.0 released (JDK 1.6+ compatible, Netty 4.0.7)
Improvement - encoding buffers allocation optimization.  
Improvement - encoding buffers now pooled in memory to reduce GC pressure (netty 4.x feature).  

####03-Aug-2013 - version 1.0.1 released (JDK 1.5+ compatible)
Fixed - error on unknown property during deserialization.  
Fixed - memory leak in long polling transport.  
Improvement - logging error info with inbound data.
 
####07-Jun-2013 - version 1.0.0 released (JDK 1.5+ compatible)
First stable release.


### Maven 

Include the following to your dependency list:

    <dependency>
     <groupId>com.corundumstudio.socketio</groupId>
     <artifactId>netty-socketio</artifactId>
     <version>1.6.0</version>
    </dependency>


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
