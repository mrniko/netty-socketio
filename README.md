#Netty-socketio Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.  

Checkout [Demo project](https://github.com/mrniko/netty-socketio-demo)

Licensed under the Apache License 2.0.


Features
================================
* Supports __0.7__...__0.9.16__ (netty-socketio 1.6.5) and __1.0+__ (netty-socketio latest version) version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client)  
* Supports xhr-polling transport  
* Supports flashsocket transport  
* Supports websocket transport  
* Supports namespaces and rooms  
* Supports ack (acknowledgment of received data)  
* Supports SSL  
* Supports client store (Memory, [Redisson](https://github.com/mrniko/redisson), [Hazelcast](http://www.hazelcast.com/))  
* Supports distributed broadcast across netty-socketio nodes ([Redisson](https://github.com/mrniko/redisson), [Hazelcast](http://www.hazelcast.com/))  
* Supports OSGi  
* Supports Spring  
* Lock-free and thread-safe implementation  
* Declarative handler configuration via annotations  


Performance
================================

Customer feedback in __2012__:  
CentOS, 1 CPU, 4GB RAM runned on VM: 
CPU 10%, Memory 15%  
6000 xhr-long polling sessions or 15000 websockets sessions  
4000 messages per second  


Customer feedback in __2014__:  
"To stress test the solution we run 30 000 simultaneous websocket clients and managed to peak at total of about 140 000 messages per second with less than 1 second average delay." (c) Viktor Endersz - Kambi Sports Solutions

Projects using netty-socketio
================================
AVOS Cloud: [avoscloud.com](https://avoscloud.com/)  
Bingo Crack: [bingocrack.com](http://bingocrack.com/)  
Kambi Sports Solutions: [kambi.com](http://kambi.com/)  
ARSnova: [arsnova.eu](https://arsnova.eu)  
Zipwhip: [zipwhip.com](https://zipwhip.com/)

Recent Releases
================================
####Please Note: trunk is current development branch.

####22-Aug-2014 - version 1.7.2 released (SocketIO 1.0+ protocol)  
Fixed - wrong outgoing message encoding using websocket transport  
Fixed - NPE in websocket transport  
Fixed - multiple packet decoding in polling transport  
Fixed - buffer leak  

####07-Jul-2014 - version 1.7.1 released (SocketIO 1.0+ protocol)  
Feature - ability to set custom `Access-Control-Allow-Origin` via Configuration.origin  
Fixed - connection via CLI socket.io-client  

####28-Jun-2014 - version 1.7.0 released (SocketIO 1.0+ protocol)
Feature - Socket.IO 1.0 protocol support. Thanks to the new protocol decoding/encoding has speedup  
__Dropped__ - `SocketIOClient.sendMessage`, `SocketIOClient.sendJsonObject` methods and corresponding listeners  
__Dropped__ - Flashsocket transport support  
__Dropped__ - protocol version 0.7 ... 0.9.16  

####13-May-2014 - version 1.6.5 released (JDK 1.6+ compatible)
Improvement - single packet encoding optimized, used mostly in WebSocket transport. Encoding time reduced up to 40% (thanks to Viktor Endersz)  
Improvement - rooms handling optimized  
Improvement - ExceptionListener.exceptionCaught method added  
__Breaking api change__ - Configuration.autoAck replaced with ackMode  
Feature - trustStore setting added  
Feature - maxFramePayloadLength setting added  
Feature - getAllClients and getClient methods added to SocketIONamespace  
Fixed - SocketIOServer.getAllClients returns wrong clients amount  

####25-Mar-2014 - version 1.6.4 released (JDK 1.6+ compatible, Netty 4.0.17)
Fixed - message release problem  
Fixed - problem with exception listener configuration redefinition  
__Breaking api change__ - DataListener.onData now throws Exception  
Improvement - data parameter added to exception listener  
Improvement - ability to setup socket configuration  
Improvement - Configuration.autoAck parameter added  

####06-Mar-2014 - version 1.6.3 released (JDK 1.6+ compatible, Netty 4.0.17)
Fixed - AckCallback handling during client disconnect  
Fixed - unauthorized handshake HTTP code changed to 401  
__Breaking api change__ - Configuration.heartbeatThreadPoolSize setting removed  
Feature - annotated Spring beans support via _SpringAnnotationScanner_  
Feature - common exception listener  
Improvement - _ScheduledExecutorService_ replaced with _HashedWheelTimer_  

####08-Feb-2014 - version 1.6.2 released (JDK 1.6+ compatible, Netty 4.0.15)
Fixed - wrong namespace client disconnect handling  
Fixed - exception in onConnect/onDisconnect/isAuthorized methods leads to server hang  
__Breaking api change__ - SocketIOClient.sendEvent methods signature changed  
Improvement - multi type events support via _MultiTypeEventListener_ and _OnEvent_ annotation  
Improvement - multi type events ack support via _MultiTypeAckCallback_  
Improvement - SocketIOClient.getHandshakeData method added  
Improvement - Jedis replaced with [Redisson](https://github.com/mrniko/redisson)  

####14-Jan-2014 - version 1.6.1 released (JDK 1.6+ compatible, Netty 4.0.14)
Fixed - JDK 1.6+ compatibility  
Feature - authorization support  

####19-Dec-2013 - version 1.6.0 released (JDK 1.6+ compatible, Netty 4.0.13)
Fixed - XHR-pooling transport regression  
Fixed - Websocket transport regression  
Fixed - namespace NPE in PacketHandler  
Fixed - executors shutdown during server stop  
Feature - client store (Memory, [Redis](http://redis.io/), [Hazelcast](http://www.hazelcast.com/)) support  
Feature - distributed broadcast across netty-socketio nodes ([Redis](http://redis.io/), [Hazelcast](http://www.hazelcast.com/)) support  
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
     <version>1.6.5</version>
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
