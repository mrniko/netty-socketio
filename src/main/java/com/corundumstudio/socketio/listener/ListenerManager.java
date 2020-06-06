package com.corundumstudio.socketio.listener;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.MultiTypeArgs;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.ScannerEngine;
import com.corundumstudio.socketio.namespace.EventEntry;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.transport.NamespaceClient;

import io.netty.util.internal.PlatformDependent;

public class ListenerManager implements ClientListeners {

	private final String namespaceName;
    private final JsonSupport jsonSupport;
    private final ExceptionListener exceptionListener;
    private final AckMode ackMode;
    private final Namespace namespace;
    
    private final ScannerEngine engine = new ScannerEngine();

    private final ConcurrentMap<String, EventEntry<?>> eventListeners = PlatformDependent.newConcurrentHashMap();
	
    private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
    private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();
    private final Queue<PingListener> pingListeners = new ConcurrentLinkedQueue<PingListener>();
    private final Queue<EventInterceptor> eventInterceptors = new ConcurrentLinkedQueue<EventInterceptor>();
	
    
    public ListenerManager(Namespace namespace, JsonSupport jsonSupport, String namespaceName, ExceptionListener exceptionListener, AckMode ackMode) {
    	this.namespace = namespace;
    	this.jsonSupport = jsonSupport;
    	this.namespaceName = namespaceName;
    	this.exceptionListener = exceptionListener;
    	this.ackMode = ackMode;
    }
    
    
	@Override
	public void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener, Class<?>... eventClass) {
		// TODO Auto-generated method stub
		 EventEntry entry = eventListeners.get(eventName);
	        if (entry == null) {
	            entry = new EventEntry();
	            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
	            if (oldEntry != null) {
	                entry = oldEntry;
	            }
	        }
	        entry.addListener(listener);
	   jsonSupport.addEventMapping(namespaceName, eventName, eventClass);
	}

	@Override
    @SuppressWarnings({"unchecked", "rawtypes"})
	public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
		// TODO Auto-generated method stub
		EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new EventEntry<T>();
            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.addListener(listener);
        jsonSupport.addEventMapping(namespaceName, eventName, eventClass);
	}

	@Override
	public void addEventInterceptor(EventInterceptor eventInterceptor) {
		// TODO Auto-generated method stub
        eventInterceptors.add(eventInterceptor);

	}

	@Override
	public void addDisconnectListener(DisconnectListener listener) {
		// TODO Auto-generated method stub
        disconnectListeners.add(listener);
	}

	@Override
	public void addConnectListener(ConnectListener listener) {
		// TODO Auto-generated method stub
		connectListeners.add(listener);
	}

	@Override
	public void addPingListener(PingListener listener) {
		// TODO Auto-generated method stub
		 pingListeners.add(listener);
	}

	@Override
	public void addListeners(Object listeners) {
		// TODO Auto-generated method stub
		addListeners(listeners, listeners.getClass());
	}

	@Override
	public void addListeners(Object listeners, Class<?> listenersClass) {
		// TODO Auto-generated method stub
        engine.scan(namespace, listeners, listenersClass);
	}

	@Override
	public void removeAllListeners(String eventName) {
		// TODO Auto-generated method stub
        EventEntry<?> entry = eventListeners.remove(eventName);
        if (entry != null) {
            jsonSupport.removeEventMapping(namespaceName, eventName);
        }
	}

	public EventEntry getEntry(String eventName) {
		return eventListeners.get(eventName);
	}
	
    private Object getEventData(List<Object> args, DataListener<?> dataListener) {
        if (dataListener instanceof MultiTypeEventListener) {
            return new MultiTypeArgs(args);
        } else {
            if (!args.isEmpty()) {
                return args.get(0);
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
	public boolean onEvent(NamespaceClient client, String eventName, List<Object> args, AckRequest ackRequest) {
		EventEntry entry = eventListeners.get(eventName);
   
        if (entry == null) {
            return false;
        }

        try {
            Queue<DataListener> listeners = entry.getListeners();
            for (DataListener dataListener : listeners) {
                Object data = getEventData(args, dataListener);
                dataListener.onData(client, data, ackRequest);
            }

            for (EventInterceptor eventInterceptor : eventInterceptors) {
                eventInterceptor.onEvent(client, eventName, args, ackRequest);
            }
        } catch (Exception e) {
        	exceptionListener.onEventException(e, args, client);
            if (ackMode == AckMode.AUTO_SUCCESS_ONLY) {
                return false;
            }
        }
		
        return true;
    }
    
    public void sendAck(AckRequest ackRequest) {
    	boolean isAckModeAuto = ackMode == AckMode.AUTO || ackMode == AckMode.AUTO_SUCCESS_ONLY;
        if (isAckModeAuto) {
            // send ack response if it not executed
            // during {@link DataListener#onData} invocation
            ackRequest.sendAckData(Collections.emptyList());
        }
    }
	
    public void onDisconnect(SocketIOClient client) {
        try {
            for (DisconnectListener listener : disconnectListeners) {
                listener.onDisconnect(client);
            }
        } catch (Exception e) {
            exceptionListener.onDisconnectException(e, client);
        }
    }
  
	public Queue<EventInterceptor> getEventInterceptors(){
		return eventInterceptors;
	}
	public ExceptionListener getExceptionListener(){
		return exceptionListener;
	}
	public Queue<DisconnectListener> getDisconnectListeners(){
		return disconnectListeners;
	}


	public void onConnect(SocketIOClient client) {
        try {
            for (ConnectListener listener : connectListeners) {
                listener.onConnect(client);
            }
        } catch (Exception e) {
            exceptionListener.onConnectException(e, client);
        }		
	}


	public void onPing(SocketIOClient client) {
		try {
            for (PingListener listener : pingListeners) {
                listener.onPing(client);
            }
        } catch (Exception e) {
            exceptionListener.onPingException(e, client);
        }		
	}


}
