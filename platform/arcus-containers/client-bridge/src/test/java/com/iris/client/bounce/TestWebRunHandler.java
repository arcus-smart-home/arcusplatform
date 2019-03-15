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

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.client.http.HttpResourceTestCase;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;
import com.iris.security.handoff.AppHandoffToken;
import com.iris.test.Mocks;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.DefaultCookie;

/**
 *
 */
@Mocks({ 
	AppHandoffDao.class, 
	Authenticator.class,
	SessionAuth.class 
})
public class TestWebRunHandler extends HttpResourceTestCase {
	@Inject WebRunHandler handler;
	
	@Inject BounceConfig config;
	@Inject AppHandoffDao mockAppHandoffDao;
	@Inject Authenticator mockAuthenticator;
	
	UUID personId = UUID.randomUUID();
	String ip = "10.0.0.1";
	String token = "token";
	
	@Before
	public void initContext() {
		EasyMock.expect(mockContext().channel()).andReturn(mockChannel()).anyTimes();
	}
	
	@Before 
	public void initClient() {
		EasyMock.expect(mockClient().getPrincipalId()).andReturn(personId).anyTimes();
	}
	
	@Before 
	public void initChannel() {
		EasyMock.expect(mockChannel().remoteAddress()).andReturn(new InetSocketAddress(ip, 33213)).anyTimes();
	}
	
	@Test
	public void testValidHandoffToken() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run?token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndSucceed();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl());
		assertSessionCookieSet(response);
	}

	@Test
	public void testValidHandoffTokenWithExtraPath() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run/extra/context/here?token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndSucceed();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "/extra/context/here");
		assertSessionCookieSet(response);
	}
	
	@Test
	public void testValidHandoffTokenWithExtraQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run?random=text&param=assert&token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndSucceed();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "?random=text&param=assert");
		assertSessionCookieSet(response);
	}

	@Test
	public void testValidHandoffTokenWithExtraPathAndQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run/extra/context/here?random=text&param=assert&token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndSucceed();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "/extra/context/here?random=text&param=assert");
		assertSessionCookieSet(response);
	}
	
	@Test
	public void testMissingHandoffToken() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run");
		expectGetClient();
		replay();
		
		try {
			FullHttpResponse response = handler.respond(request, mockContext());
			fail("Expected exception but got " + response);
		}
		catch (HttpException e) {
			assertEquals(HttpResponseStatus.BAD_REQUEST.code(), e.getStatusCode());
		}
	}

	@Test
	public void testInvalidHandoffToken() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run?token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndFail();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl());
		assertSessionCookieNotSet(response);
	}

	@Test
	public void testInvalidHandoffTokenWithExtraPath() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run/extra/context/here?token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndFail();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "/extra/context/here");
		assertSessionCookieNotSet(response);
	}
	
	@Test
	public void testInvalidHandoffTokenWithExtraQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run?random=text&param=assert&token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndFail();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "?random=text&param=assert");
		assertSessionCookieNotSet(response);
	}

	@Test
	public void testInvalidHandoffTokenWithExtraPathAndQueryParams() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/web/run/extra/context/here?random=text&param=assert&token=token");
		expectGetClient();
		Capture<AppHandoffToken> handoff = captureLoginAndFail();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, config.getWebUrl() + "/extra/context/here?random=text&param=assert");
		assertSessionCookieNotSet(response);
	}
	
	protected Capture<AppHandoffToken> captureLoginAndSucceed() {
		Capture<AppHandoffToken> capture = Capture.newInstance();
		mockClient().login(EasyMock.capture(capture));
		EasyMock.expectLastCall().once();
		expectGenerateCookie();
		return capture;
	}
	
	protected Capture<AppHandoffToken> captureLoginAndFail() {
		Capture<AppHandoffToken> capture = Capture.newInstance();
		mockClient().login(EasyMock.capture(capture));
		EasyMock.expectLastCall().andThrow(new IncorrectCredentialsException()).once();
		return capture;
	}
	
	protected void expectGenerateCookie() {
		EasyMock.expect(mockClient().getSessionId()).andReturn("session-id").once();
		EasyMock.expect(mockAuthenticator.createCookie("session-id")).andReturn(new DefaultCookie("irisAuthToken", "session-id")).once();
	}
	
	protected void assertHandoff(AppHandoffToken handoff) {
		assertEquals(ip, handoff.getHost());
		assertEquals("token", handoff.getToken());
	}
	
	protected void assertSessionCookieSet(FullHttpResponse response) {
		String cookie = response.headers().get(HttpHeaders.Names.SET_COOKIE);
		assertFalse(StringUtils.isEmpty(cookie));
		// FIXME make more assertions about the validity of the cookie
	}

	protected void assertSessionCookieNotSet(FullHttpResponse response) {
		String cookie = response.headers().get(HttpHeaders.Names.SET_COOKIE);
		assertTrue(StringUtils.isEmpty(cookie));
	}

}

