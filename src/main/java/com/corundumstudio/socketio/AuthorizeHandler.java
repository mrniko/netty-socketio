package com.corundumstudio.socketio;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.transport.XHRPollingClient;

public class AuthorizeHandler extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 'UUID' to 'timestamp' mapping
    // this map will be always smaller than 'connectedSessionIds'
    private final Map<UUID, Long> authorizedSessionIds = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> connectedSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private final String connectPath;

    private final ObjectMapper objectMapper;
    private final Encoder encoder;

    private final Configuration configuration;
    private final SocketIOListener socketIOListener;

    public AuthorizeHandler(String connectPath, ObjectMapper objectMapper, Encoder encoder,
            SocketIOListener socketIOListener, Configuration configuration) {
        super();
        this.connectPath = connectPath;
        this.objectMapper = objectMapper;
        this.socketIOListener = socketIOListener;
        this.encoder = encoder;
        this.configuration = configuration;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            Channel channel = ctx.getChannel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            if (HttpMethod.GET.equals(req.getMethod()) && queryDecoder.getPath().equals(connectPath)) {
                authorize(channel, req, queryDecoder.getParameters());
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private void authorize(Channel channel, HttpRequest msg, Map<String, List<String>> params)
            throws IOException {
        removeStaleAuthorizedIds();
        // TODO use common client
        final UUID sessionId = UUID.randomUUID();
        XHRPollingClient client = new XHRPollingClient(encoder, this, null);
        authorizedSessionIds.put(sessionId, System.currentTimeMillis());

        //String transports = "xhr-polling,websocket";
        String transports = "websocket";
        String heartbeatTimeoutVal = String.valueOf(configuration.getHeartbeatTimeout());
        if (configuration.getHeartbeatTimeout() == 0) {
            heartbeatTimeoutVal = "";
        }

        String hs = sessionId + ":" + heartbeatTimeoutVal + ":" + configuration.getCloseTimeout() + ":" + transports;

        List<String> jsonpParam = params.get("jsonp");
        if (jsonpParam != null) {
            String jsonpResponse = "io.j[" + jsonpParam.get(0) + "](" + objectMapper.writeValueAsString(hs) + ");";
            client.sendJsonp(jsonpResponse);
        } else {
            client.sendUnencoded(hs);
        }
        client.doReconnect(channel, msg);
        log.debug("New sessionId: {} authorized", sessionId);
    }

    public boolean isSessionAuthorized(UUID sessionId) {
        return connectedSessionIds.contains(sessionId)
                    || authorizedSessionIds.containsKey(sessionId);
    }

    /**
     * Remove stale authorized client ids which
     * has not connected during some timeout
     */
    private void removeStaleAuthorizedIds() {
        for (Iterator<Entry<UUID, Long>> iterator = authorizedSessionIds.entrySet().iterator(); iterator.hasNext();) {
            Entry<UUID, Long> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue() > 60*1000) {
                iterator.remove();
                log.debug("Authorized sessionId: {} cleared due to connection timeout", entry.getKey());
            }
        }
    }

    public void connect(SocketIOClient client) {
        authorizedSessionIds.remove(client.getSessionId());
        connectedSessionIds.add(client.getSessionId());

        client.send(new Packet(PacketType.CONNECT));
        socketIOListener.onConnect(client);
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        connectedSessionIds.remove(client.getSessionId());
    }

}
