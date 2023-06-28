
Netty-socketio Overview
===
this patch  V4 socket.io   websocket  when  conect  is conneting  ,still time out
                           polling    loop   open new url   
![image](https://github.com/mrniko/netty-socketio/assets/7028443/70c91fc9-210d-4001-beba-c4408b0e0ed4)![image](https://github.com/mrniko/netty-socketio/assets/7028443/78467ab1-0a90-40ea-a6ea-06ef08bc8b5d)

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.  

Checkout [Demo project](https://github.com/mrniko/netty-socketio-demo)

Licensed under the Apache License 2.0.


Features
================================
* __1.x - 4.x__ version of [Socket.IO-client](https://github.com/socketio/socket.io-client) is supported
* Supports xhr-polling transport  
* Supports websocket transport  
* Supports namespaces and rooms  
* Supports ack (acknowledgment of received data)  
* Supports SSL  
* Supports client store (Memory, [Redisson](https://redisson.org), [Hazelcast](https://www.hazelcast.com/))  
* Supports distributed broadcast across netty-socketio nodes ([Redisson](https://redisson.org), [Hazelcast](https://www.hazelcast.com/))  
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
#### Please Note: trunk is current development branch.

#### 17-May-2023 - version 2.0.2 released
Fixed - NPE in WebSocketTransport and PollingTransport (thanks to @Hunterk95)

#### 14-May-2023 - version 2.0.1 released 
JDK 8 is minimum version required for usage. 

Fixed - message larger than maxFramePayloadLength causes memory leak  

#### 14-May-2023 - version 2.0.0 released 
Feature - SocketIO 4.x protocol support (thanks to @HashZhang)  

Fixed - add all the socketio specific classes to bundles (thanks to @rishabhgup)  

#### 17-Apr-2023 - version 1.7.25 released  
Fixed - io.netty.channel.ChannelPipelineException error

#### 14-Apr-2023 - version 1.7.24 released  
Feature - SocketIOClient.isWritable() method added (thanks @xuwenqing04)  
Feature - Namespace.addListeners(Iterable<L> listeners) method added (thanks @damonxue)  

Fixed - disconnect event packet should be a subtype of MESSAGE (thanks @MasterShi)  
Fixed - BaseStoreFactory throws NPE  

#### 02-Feb-2023 - version 1.7.23 released  
Feature - Added support for bulk join and leave (thanks @ksahu750)

Fixed - auto disconnected (regression since 1.7.22)

#### 22-Sep-2022 - version 1.7.22 released  
Feature - ping/pong support for socketio v4 protocol (thanks to @HashZhang)

#### 06-Aug-2022 - version 1.7.21 released  
Fixed - transport name should be checked in uppercase (regression since 1.7.20)

#### 26-Jul-2022 - version 1.7.20 released  
Feature - needClientAuth setting added (thanks to @huws)

Fixed - ContinuationWebSocketFrame should be used if payload > maxFramePayloadLength (thanks to 俞伟搏)  
Fixed - event listener src class and dest bean is no match (thanks to @cifaz)  
Fixed - illegal transport parameter (thanks to @mirus36)  

#### 29-Apr-2021 - version 1.7.19 released  
Feature - writeBufferWaterMarkLow and writeBufferWaterMarkHigh settings added (thanks to @xuminwlt)  
Feature - allowHeaders setting added (thanks to @HuangHuan)  
Feature - getCurrentRoomSize() method added (thanks to @sunxiaoyu3)  

Fixed - namespace bug (thanks to @@johntyty912)  
Fixed - multiple nodes with redisson receive same message (thanks to 梁嘉祺)  
Fixed - multiple nodes receive only one LEAVE msg on client disconnect (thanks to @GaryLeung922)  

#### 17-Jan-2020 - version 1.7.18 released  
Feature - support for event interceptors which catch all events (thanks to @yosiat)  
Fixed - namespace event broadcasting (thanks to Volodymyr Masliy)  

#### 11-Jan-2019 - version 1.7.17 released  
Feature - randomSession setting added to Config object (thanks to @yuanxiangz)  
Fixed - NPE in WebSocketTransport  
Fixed - NPE & memory leak (thanks to zhaolianwang)  
Fixed - namespace parsing (thanks to Redliver)  
Fixed - Redisson 3.9+ compatibility  

#### 06-Jul-2018 - version 1.7.16 released  
Fixed - non thread-safe ACK handling (thanks to @dawnbreaks)  
Fixed - inactive long-polling channels cause memory leak (thanks to @dawnbreaks)  
Fixed - websocket CloseFrame processing (thanks to @hangsu.cho)  
Fixed - WebSocketTransport NPE  

#### 15-May-2018 - version 1.7.15 released  

Fixed - Session ID is not unique anymore  
Fixed - fixed underlying connection not closing on ping timeout  
Fixed - the "fin_close" problem  

#### 26-Feb-2018 - version 1.7.14 released  
Feature - added local socket address for the connection (thanks to @SergeyGrigorev)  
Feature - `addPingListener` method added (thanks to @lovebing)  
Feature - add ThreadFactory for HashedWheelTimer (thanks to @hand515)  
Fixed - changed SO_LINGER to be handled as child channel (not server channel) option (thanks to @robymus)  
Fixed - ByteBuf leak if binary attachments are used  
Fixed - restore session from Cookie (thanks to @wuxudong)  
Fixed - NumberFormatException when b64 is bool value (thanks to @vonway)  
Fixed - data encoding for polling transport  

#### 20-Sep-2017 - version 1.7.13 released  
Feature - Added option to change the SSL KeyFactoryAlgorithm using Configuration (thanks to @robymus)  
Improvement - Binary ack handling improvements (thanks to Sergey Bushik)  
Fixed - Failed to mark a promise as success because it has succeeded already (thanks to @robymus)

#### 27-Aug-2016 - version 1.7.12 released  
Feature - `SocketIOServer.removeAllListeners` method added  
Feature - `BroadcastOperations.sendEvent` method with `excludedClient` param added  
Improvement - Redisson updated to 2.4.0  
Fixed - memory leak in Namespace object (thanks to @CrazyIvan007)  


#### 13-Jul-2016 - version 1.7.11 released  
Fixed - Throw error if transport not supported  
Fixed - Client disconnecting when using Polling - IndexOutOfBoundsException  

#### 4-Mar-2016 - version 1.7.10 released  
Fixed - netty updated to 4.1.0.CR3 version  
Fixed - binary packet parsing (thanks to Winston Li)  

#### 6-Feb-2016 - version 1.7.9 released  
Feature - Compression support  
Fixed - DotNET client request handling  
Fixed - Packet length format parsing  
Fixed - skipping 'd=' in packet  
Fixed - Polling clients sporadically get prematurely disconnected (thanks to lpage30)  
Fixed - connections stay open forever if server sent `close` packet  
Fixed - compatibility with Redisson latest version  

#### 30-Nov-2015 - version 1.7.8 released  
Improvement - `WebSocketServerHandshaker.allowExtensions` is `true` now  
Improvement - SessionID cookie implementation (thanks to @ryandietrich)  
Fixed - clientRooms leak (thanks to @andreaspalm)  
Fixed - ExceptionListener not used for errors in JSON parsing  
Fixed - "silent channel" attack    

#### 26-Mar-2015 - version 1.6.7 released  
Improvement - `useStrictOrdering` param added for websocket packets strict ordering  
Improvement - `FAIL_ON_EMPTY_BEANS = false` option setted in json decoder  

#### 18-Feb-2015 - version 1.7.7 released  
Improvement - no need to add jackson lib if you use own JsonSupport impl    
Fixed - SocketIO client 1.3.x support  
Fixed - Charset encoding handling (thanks to  alim-akbashev)  

#### 17-Jan-2015 - version 1.7.6 released  
Improvement - `SocketIONamespace.getName()` added  
Fixed - WebSocket frames aggregation  
Fixed - WebSocket buffer release  
Fixed - `Unexpected end-of-input in VALUE_STRING` error  
Fixed - Access-Control-Allow-Credentials is TRUE for requests with origin header  

#### 05-Dec-2014 - version 1.7.5 released  
Feature - `Configuration.sslProtocol` param added  
Fixed - BinaryEvent ack handling  
Fixed - BinaryEvent non b64 encoding/decoding  
Fixed - buffer leak during packet encoding  

#### 15-Nov-2014 - version 1.7.4 released  
Fixed - packet encoding  
Fixed - BinaryEvent encoding/decoding  
Fixed - unchallenged connections handling  

#### 29-Sep-2014 - version 1.6.6 released  
Feature - `origin` setting added  
Feature - `crossDomainPolicy` setting added  
Feature - `SocketIOServer.startAsync` method added  

#### 24-Sep-2014 - version 1.7.3 released  
Feature - Epoll support  
Improvement - BinaryEvent support  
Fixed - SocketIOClient disconnect handling  
Fixed - broadcast callback  
Fixed - NPE then no transport defined during auth  
Fixed - ping timeout for polling transport  
Fixed - buffer leak in PacketEncoder  

#### 22-Aug-2014 - version 1.7.2 released  
Fixed - wrong outgoing message encoding using websocket transport  
Fixed - NPE in websocket transport  
Fixed - multiple packet decoding in polling transport  
Fixed - buffer leak  

#### 07-Jul-2014 - version 1.7.1 released  
Feature - ability to set custom `Access-Control-Allow-Origin` via Configuration.origin  
Fixed - connection via CLI socket.io-client  

#### 28-Jun-2014 - version 1.7.0 released
Feature - Socket.IO 1.0 protocol support. Thanks to the new protocol decoding/encoding has speedup  
__Dropped__ - `SocketIOClient.sendMessage`, `SocketIOClient.sendJsonObject` methods and corresponding listeners  
__Dropped__ - Flashsocket transport support  
__Dropped__ - protocol version 0.7 ... 0.9.16  

#### 13-May-2014 - version 1.6.5 released
Improvement - single packet encoding optimized, used mostly in WebSocket transport. Encoding time reduced up to 40% (thanks to Viktor Endersz)  
Improvement - rooms handling optimized  
Improvement - ExceptionListener.exceptionCaught method added  
__Breaking api change__ - Configuration.autoAck replaced with ackMode  
Feature - trustStore setting added  
Feature - maxFramePayloadLength setting added  
Feature - getAllClients and getClient methods added to SocketIONamespace  
Fixed - SocketIOServer.getAllClients returns wrong clients amount  

#### 25-Mar-2014 - version 1.6.4 released
Fixed - message release problem  
Fixed - problem with exception listener configuration redefinition  
__Breaking api change__ - DataListener.onData now throws Exception  
Improvement - data parameter added to exception listener  
Improvement - ability to setup socket configuration  
Improvement - Configuration.autoAck parameter added  

#### 06-Mar-2014 - version 1.6.3 released
Fixed - AckCallback handling during client disconnect  
Fixed - unauthorized handshake HTTP code changed to 401  
__Breaking api change__ - Configuration.heartbeatThreadPoolSize setting removed  
Feature - annotated Spring beans support via _SpringAnnotationScanner_  
Feature - common exception listener  
Improvement - _ScheduledExecutorService_ replaced with _HashedWheelTimer_  

#### 08-Feb-2014 - version 1.6.2 released
Fixed - wrong namespace client disconnect handling  
Fixed - exception in onConnect/onDisconnect/isAuthorized methods leads to server hang  
__Breaking api change__ - SocketIOClient.sendEvent methods signature changed  
Improvement - multi type events support via _MultiTypeEventListener_ and _OnEvent_ annotation  
Improvement - multi type events ack support via _MultiTypeAckCallback_  
Improvement - SocketIOClient.getHandshakeData method added  
Improvement - Jedis replaced with [Redisson](https://github.com/mrniko/redisson)  

#### 14-Jan-2014 - version 1.6.1 released
Fixed - JDK 1.6+ compatibility  
Feature - authorization support  

#### 19-Dec-2013 - version 1.6.0 released
Fixed - XHR-pooling transport regression  
Fixed - Websocket transport regression  
Fixed - namespace NPE in PacketHandler  
Fixed - executors shutdown during server stop  
Feature - client store (Memory, [Redis](http://redis.io/), [Hazelcast](http://www.hazelcast.com/)) support  
Feature - distributed broadcast across netty-socketio nodes ([Redis](http://redis.io/), [Hazelcast](http://www.hazelcast.com/)) support  
Feature - OSGi support (thanks to rdevera)  
Improvement - XHR-pooling optimization  
Improvement - SocketIOClient.getAllRooms method added

#### 07-Dec-2013 - version 1.5.4 released
Fixed - flash policy "request leak" after page reload (thanks to ntrp)  
Fixed - websocket swf loading (thanks to ntrp)  
Fixed - wrong urls causes a potential DDoS  
Fixed - Event.class package visibility changed to avoid direct usage  
Improvement - Simplified Jackson modules registration

#### 24-Oct-2013 - version 1.5.2 released
Fixed - NPE during shutdown  
Improvement - isEmpty method added to Namespace

#### 13-Oct-2013 - version 1.5.1 released
Fixed - wrong ack timeout callback invocation  
Fixed - bigdecimal serialization for JSON  
Fixed - infinity loop during packet handling exception  
Fixed - 'client not found' handling  

#### 27-Aug-2013 - version 1.5.0 released
Improvement - encoding buffers allocation optimization.  
Improvement - encoding buffers now pooled in memory to reduce GC pressure (netty 4.x feature).  

#### 03-Aug-2013 - version 1.0.1 released
Fixed - error on unknown property during deserialization.  
Fixed - memory leak in long polling transport.  
Improvement - logging error info with inbound data.
 
#### 07-Jun-2013 - version 1.0.0 released
First stable release.


### Maven 

Include the following to your dependency list:

    <dependency>
     <groupId>com.corundumstudio.socketio</groupId>
     <artifactId>netty-socketio</artifactId>
     <version>1.7.19</version>
    </dependency>
    
### Supported by

YourKit is kindly supporting this open source project with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
<a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a> and
<a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>.
