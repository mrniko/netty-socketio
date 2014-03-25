#Netty-socketio Overview

This project is an open-source Java implementation of [Socket.IO](http://socket.io/) server. Based on [Netty](http://netty.io/) server framework.  

Checkout [Demo project](https://github.com/mrniko/netty-socketio-demo)

Licensed under the Apache License 2.0.


Features
================================
* Supports 0.7+ version of [Socket.IO-client](https://github.com/LearnBoost/socket.io-client) up to 0.9.16  
* Supports xhr-polling transport  
* Supports flashsocket transport  
* Supports websocket transport: HyBi 00 (which is the same as Hixie 76), HyBi 8-10 and HyBi 13-17 (17 is the same as IETF 6455).  
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

CentOS, 1 CPU, 4GB RAM runned on VM (based on customer report, tested in 2012): 

CPU 10%, Memory 15%  
6000 xhr-long polling sessions or 15000 websockets sessions  
4000 messages per second  

Projects using netty-socketio
================================
ARSnova: [arsnova.eu](https://arsnova.eu)  
Zipwhip: [zipwhip.com](https://zipwhip.com/)

Recent Releases
================================
####Please Note: trunk is current development branch.

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
Improvement - _ScheduledExecutorService_ replaced with _HashedWheelT
