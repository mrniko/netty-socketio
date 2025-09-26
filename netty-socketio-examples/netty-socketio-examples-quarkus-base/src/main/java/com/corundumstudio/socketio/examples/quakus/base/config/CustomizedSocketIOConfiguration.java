package com.corundumstudio.socketio.examples.quakus.base.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ExceptionListener;

import io.netty.channel.ChannelHandlerContext;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;


@ApplicationScoped
public class CustomizedSocketIOConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CustomizedSocketIOConfiguration.class);

    AtomicReference<Throwable> lastException = new AtomicReference<>();

    public Throwable getLastException() {
        return lastException.get();
    }

    /**
     * Produce a custom ExceptionListener bean to handle exceptions in Socket.IO events.
     * replaces the default ExceptionListener.
     * @Unremovable ensures that this bean is not removed during build optimization.
     * @return
     */
    @Produces
    @Unremovable
    public ExceptionListener getExceptionListener() {
        return new ExceptionListener() {
            @Override
            public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
                lastException.set(e);
                log.error("onEventException, {}", e.getMessage());
            }

            @Override
            public void onDisconnectException(Exception e, SocketIOClient client) {
                lastException.set(e);
                log.error("onDisconnectException, {}", e.getMessage());
            }

            @Override
            public void onConnectException(Exception e, SocketIOClient client) {
                lastException.set(e);
                log.error("onConnectException, {}", e.getMessage());
            }

            @Override
            public void onPingException(Exception e, SocketIOClient client) {
                lastException.set(e);
                log.error("onPingException, {}", e.getMessage());
            }

            @Override
            public void onPongException(Exception e, SocketIOClient client) {
                lastException.set(e);
                log.error("onPongException, {}", e.getMessage());
            }

            @Override
            public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
                lastException.set(e);
                log.error("exceptionCaught, {}", e.getMessage());
                return false;
            }

            @Override
            public void onAuthException(Throwable e, SocketIOClient client) {
                lastException.set(e);
                log.error("onAuthException, {}", e.getMessage());
            }
        };
    }
}
