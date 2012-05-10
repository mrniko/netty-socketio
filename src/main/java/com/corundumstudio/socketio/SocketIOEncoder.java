package com.corundumstudio.socketio;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.Transport;
import com.corundumstudio.socketio.transport.XHRPollingClient;

public class SocketIOEncoder extends OneToOneEncoder {

    class ClientEntry {

        final AtomicInteger lastChannelId = new AtomicInteger();
        final Queue<Packet> packets = new ConcurrentLinkedQueue<Packet>();

        public void addPacket(Packet packet) {
            packets.add(packet);
        }

        public Packet getPacket() {
            return packets.poll();
        }

        public boolean tryToWrite(Channel channel) {
            int prevVal = lastChannelId.get();
            return lastChannelId.compareAndSet(prevVal, channel.getId());
        }

    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ObjectMapper objectMapper;
    private Encoder encoder;

    private Transport webSocketTransport;
    private Transport xhrPoolingTransport;

    private ConcurrentMap<UUID, ClientEntry> sessionId2ActiveChannelId = new ConcurrentHashMap<UUID, ClientEntry>();

    public SocketIOEncoder(ObjectMapper objectMapper, Encoder encoder,
            Transport webSocketTransport, Transport xhrPoolingTransport) {
        super();
        this.objectMapper = objectMapper;
        this.encoder = encoder;
        this.webSocketTransport = webSocketTransport;
        this.xhrPoolingTransport = xhrPoolingTransport;
    }

    private ClientEntry getClientEntry(Channel channel, UUID sessionId) {
        ClientEntry clientEntry = sessionId2ActiveChannelId.get(sessionId);
        if (clientEntry == null) {
            clientEntry = new ClientEntry();
            ClientEntry old = sessionId2ActiveChannelId.putIfAbsent(sessionId, clientEntry);
            if (old != null) {
                clientEntry = old;
            }
        }
        return clientEntry;
    }

    private void sendPostResponse(Channel channel, String origin) {
        HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);
        if (origin != null) {
            res.addHeader("Access-Control-Allow-Origin", origin);
            res.addHeader("Access-Control-Allow-Credentials", "true");
        }

        ChannelFuture f = channel.write(res);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    private void write(XHRPollingClient client, ClientEntry clientEntry,
            Channel channel) throws IOException {
        if (!channel.isConnected()) {
            log.trace("Connection closed!");
            return;
        }

        List<Packet> packets = new ArrayList<Packet>();
        while (true) {
            Packet packet = clientEntry.getPacket();
            if (packet != null) {
                packets.add(packet);
            } else {
                break;
            }
        }
        if (packets.isEmpty()) {
            return;
        }
        String message = encoder.encodePackets(packets);

        sendMessage(client.getOrigin(), client.getSessionId(), channel, message);
    }

    private void sendMessage(String origin, UUID sessionId, Channel channel,
            String message) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1,
                HttpResponseStatus.OK);
        addHeaders(origin, res);
        res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
        HttpHeaders.setContentLength(res, res.getContent().readableBytes());

        log.trace("Out message: {}, sessionId: {}, channelId: {}", new Object[] { message,
                sessionId, channel.getId() });
        ChannelFuture f = channel.write(res);
        f.addListener(ChannelFutureListener.CLOSE);
    }

    private void addHeaders(String origin, HttpResponse res) {
        res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.addHeader(CONNECTION, KEEP_ALIVE);
        if (origin != null) {
            res.addHeader("Access-Control-Allow-Origin", origin);
            res.addHeader("Access-Control-Allow-Credentials", "true");
        }
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof AuthorizeMessage) {
            AuthorizeMessage authMsg = (AuthorizeMessage) msg;
            String message = authMsg.getMsg();
            if (authMsg.getJsonpParam() != null) {
                message = "io.j[" + authMsg.getJsonpParam() + "]("
                        + objectMapper.writeValueAsString(message) + ");";
            }
            sendMessage(authMsg.getOrigin(), authMsg.getSessionId(), channel, message);
            return ChannelBuffers.EMPTY_BUFFER;
        } else if (msg instanceof ErrorMessage) {
            ErrorMessage errorMsg = (ErrorMessage) msg;
            if (errorMsg.getType() == ErrorMessage.Type.XHR) {
                String message = encoder.encodePacket(errorMsg.getPacket());
                sendMessage(errorMsg.getOrigin(), null, channel, message);
            }
            return ChannelBuffers.EMPTY_BUFFER;
        } else if (msg instanceof XHRPostMessage) {
            XHRPostMessage message = (XHRPostMessage) msg;
            sendPostResponse(channel, message.getOrigin());
            return ChannelBuffers.EMPTY_BUFFER;
        } else if ((msg instanceof Packet) || (msg instanceof XHRNewChannelMessage)) {
            if (webSocketTransport.getClient(channel) != null) {
                SocketIOClient client = webSocketTransport.getClient(channel);
                Packet packet = (Packet) msg;
                String message = encoder.encodePacket(packet);
                WebSocketFrame res = new TextWebSocketFrame(message.toString());
                log.trace("Out message: {} sessionId: {}", new Object[] {
                        message, client.getSessionId() });
                channel.write(res);
                return ChannelBuffers.EMPTY_BUFFER;
            }
            if (xhrPoolingTransport.getClient(channel) != null) {
                XHRPollingClient client = (XHRPollingClient) xhrPoolingTransport.getClient(channel);
                ClientEntry clientEntry = getClientEntry(channel, client.getSessionId());

                if (msg instanceof Packet) {
                    Packet packet = (Packet) msg;
                    clientEntry.addPacket(packet);
                }
                if (clientEntry.tryToWrite(channel)) {
                    write(client, clientEntry, channel);
                    return ChannelBuffers.EMPTY_BUFFER;
                }
            }
        }
        return msg;
    }

}
