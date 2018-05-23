/*
 * @(#)WebSocketTransportTest.java 2018. 5. 23.
 *
 * Copyright 2018 NAVER Corp. All rights Reserved. NAVER PROPRIETARY/CONFIDENTIAL. Use is subject to
 * license terms.
 */
package com.corundumstudio.socketio.transport;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

/**
 * @author hangsu.cho@navercorp.com
 *
 */
public class WebSocketTransportTest {

  /**
   * Test method for {@link com.corundumstudio.socketio.transport.WebSocketTransport#channelRead()}.
   */
  @Test
  public void testCloseFrame() {
    EmbeddedChannel channel = createChannel();

    channel.writeInbound(new CloseWebSocketFrame());
    Object msg = channel.readOutbound();

    // https://tools.ietf.org/html/rfc6455#section-5.5.1
    // If an endpoint receives a Close frame and did not previously send a Close frame, the endpoint
    // MUST send a Close frame in response.
    assertTrue(msg instanceof CloseWebSocketFrame);
  }

  private EmbeddedChannel createChannel() {
    return new EmbeddedChannel(new WebSocketTransport(false, null, null, null, null) {
      /*
       * (non-Javadoc)
       * 
       * @see
       * com.corundumstudio.socketio.transport.WebSocketTransport#channelInactive(io.netty.channel.
       * ChannelHandlerContext)
       */
      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {}
    });
  }

}
