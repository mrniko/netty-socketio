/**
 * Copyright 2012 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.InetSocketAddress;

import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.junit.Before;
import org.junit.Test;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SessionID;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.scheduler.HashedWheelScheduler;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.transport.WebSocketTransport;

public class AuthorizeHandlerTest {

	private static final String TEST_SESSION_ID = "666";

    private static final String TEST_URI = "/?transport=" + WebSocketTransport.NAME;

	@Mocked
	private DisconnectableHub disconnectableHub;

	private EmbeddedChannel channel;

	private Configuration configuration;

    private ClientsBox clientsBox;

	@Before
	public void setUp() {
		configuration = new Configuration();
		HashedWheelScheduler scheduler = new HashedWheelScheduler();
		clientsBox = new ClientsBox();
        channel = new EmbeddedChannel(new AuthorizeHandler("/", scheduler,
				configuration, new NamespacesHub(configuration),
				new MemoryStoreFactory(), disconnectableHub, new AckManager(
						scheduler), clientsBox, new DefaultSessionIDFactory()));

		new NonStrictExpectations(channel) {
			{
				channel.remoteAddress();
				result = new InetSocketAddress(8080);
			}
		};
	}

	@Test
	public void shouldAuthorizeRequest() throws Exception {
		prepareAuthorizationListener(true, null);

		channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
				HttpMethod.GET, TEST_URI));
		// then
		Object in = channel.readInbound();
		assertTrue(in instanceof DefaultFullHttpRequest);
		DefaultFullHttpRequest req = (DefaultFullHttpRequest) in;
		assertEquals(1, req.refCnt());
		
	}

	@Test
	public void shouldNotAuthorizeRequest() throws Exception {
		prepareAuthorizationListener(false, null);

		channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
				HttpMethod.GET, TEST_URI));
		// then
		Object out = channel.readOutbound();
		assertTrue(out instanceof DefaultHttpResponse);
		DefaultHttpResponse res = (DefaultHttpResponse) out;
		assertEquals(HttpResponseStatus.UNAUTHORIZED, res.getStatus());
	}

	private void prepareAuthorizationListener(final boolean authorizeResponse, final SessionID sessionId) {
		configuration.setAuthorizationListener(new AuthorizationListener() {
			@Override
			public boolean isAuthorized(HandshakeData data) {
				if (sessionId != null) {
					data.setSessionId(sessionId);
				}
				return authorizeResponse;
			}
		});
	}
}
