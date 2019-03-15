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
public class TestAppLaunchHandler extends HttpResourceTestCase {
	@Inject AppLaunchHandler handler;
	
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
	public void testAndroidOsQueryParam() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch?os=android");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/android/run?token=token");
	}

	@Test
	public void testAppleOsQueryParam() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch?os=ios");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/ios/run?token=token");
	}
	
	@Test
	public void testInvalidOsQueryParam() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch?os=linux");
		expectGetClient();
		replay();
		
		try {
			handler.respond(request, mockContext());
			fail("Allowed a bad request through");
		}
		catch(HttpException e) {
			assertEquals(HttpResponseStatus.BAD_REQUEST.code(), e.getStatusCode());
		}
	}

	@Test
	public void testAndroidUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (Linux; Android 7.0; SAMSUNG SM-G935P Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/5.4 Chrome/51.0.2704.106 Mobile Safari/537.36");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/android/run?token=token");
	}

	@Test
	public void testIPhoneUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) Version/10.0 Mobile/14G60 Safari/602.1");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/ios/run?token=token");
	}

	@Test
	public void testIPadUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (iPad; CPU OS 11_0_2 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A421 Safari/604.1");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/ios/run?token=token");
	}

	@Test
	public void testMacUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/other/run?token=token");
	}

	@Test
	public void testWindowsUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/other/run?token=token");
	}

	@Test
	public void testLinuxUserAgent() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "https://app.arcus.com/app/launch");
		request.headers().add(HttpHeaders.Names.USER_AGENT, "Mozilla/5.0 (X11; Linux armv7l) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.89 Safari/537.36");
		expectGetClient();
		Capture<SessionHandoff> handoff = captureNewToken();
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertHandoff(handoff.getValue());
		assertRedirectTo(response, "https://dev-app.arcus.com/other/run?token=token");
	}
	
	protected Capture<SessionHandoff> captureNewToken() {
		Capture<SessionHandoff> capture = Capture.newInstance();
		EasyMock.expect(mockAppHandoffDao.newToken(EasyMock.capture(capture))).andReturn(token).once();
		return capture;
	}
	
	protected void assertHandoff(SessionHandoff handoff) {
		assertEquals(personId, handoff.getPersonId());
		assertEquals(ip, handoff.getIp());
	}
	
}

