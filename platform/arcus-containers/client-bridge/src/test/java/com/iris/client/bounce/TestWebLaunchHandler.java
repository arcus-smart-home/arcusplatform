/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.iris.client.bounce;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.client.http.HttpResourceTestCase;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;
import com.iris.test.Mocks;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 *
 */
@Mocks({ 
	AppHandoffDao.class, 
	SessionAuth.class 
})
public class TestWebLaunchHandler extends HttpResourceTestCase {
	@Inject WebLaunchHandler handler;
	
	@Inject BounceConfig config;
	@Inject AppHandoffDao mockAppHandoffDao;
	
	UUID personId = UUID.randomUUID();
	String username = "test3@gmail.com";
	String ip = "10.0.0.1";
	String token = "token";
	
	@Before
	public void initContext() {
		EasyMock.expect(mockContext().channel()).andReturn(mockChannel()).anyTimes();
	}
	
	@Before 
	public void initClient() {
		EasyMock.expect(mockClient().getPrincipalId()).andReturn(personId).anyTimes();
		EasyMock.expect(mockClient().getPrincipalName()).andReturn(username).anyTimes();
	}
	
	@Before 
	public void initChannel() {
		EasyMock.expect(mockChannel().remoteAddress()).andReturn(new InetSocketAddress(ip, 33213)).anyTimes();
	}
	
	@Test
	public void testWebLaunch() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getRedirectUrl() + "/web/run?token=token");
	}

	@Test
	public void testWebLaunchWithExtraPath() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch/extra/context/here");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getRedirectUrl() + "/web/run/extra/context/here?token=token");
	}
	
	@Test
	public void testWebLaunchWithQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch?random=text&param=assert");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getRedirectUrl() + "/web/run?random=text&param=assert&token=token");
	}

	@Test
	public void testWebLaunchWithExtraPathAndQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch/extra/context/here?random=text&param=assert");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getRedirectUrl() + "/web/run/extra/context/here?random=text&param=assert&token=token");
	}
	
	@Test
	public void testUnauthenticatedWebLaunch() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch");
		expectGetUnauthenticatedClient();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getWebUrl());
	}

	@Test
	public void testUnauthenticatedWebLaunchWithExtraPathAndQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/launch/extra/context/here?random=text&param=assert");
		expectGetUnauthenticatedClient();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getWebUrl() + "/extra/context/here?random=text&param=assert");
	}
	
	protected void expectGetUnauthenticatedClient() {
		expectGetClient();
		EasyMock.expect(mockClient().isAuthenticated()).andReturn(false);
	}

	protected Capture<SessionHandoff> captureNewToken() {
		EasyMock.expect(mockClient().isAuthenticated()).andReturn(true);
		Capture<SessionHandoff> capture = Capture.newInstance();
		EasyMock.expect(mockAppHandoffDao.newToken(EasyMock.capture(capture))).andReturn(token).once();
		return capture;
	}
	
	protected void assertHandoff(SessionHandoff handoff) {
		assertEquals(personId, handoff.getPersonId());
		assertEquals(ip, handoff.getIp());
	}
	
}

