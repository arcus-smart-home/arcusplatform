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
package com.iris.client.bounce;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.client.http.HttpResourceTestCase;

import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class TestAppFallbackHandler extends HttpResourceTestCase {
	@Inject AppFallbackHandler handler;
	@Inject BounceConfig config;
	
	@Test
	public void testIOsRedirect() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/ios/run");
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getAppleStoreUrl());
	}
	
	@Test
	public void testAndroidRedirect() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/android/run");
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getAndroidStoreUrl());
	}
	
	@Test
	public void testOtherRedirect() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/other/run");
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getHelpUrl());
	}
	
	@Test
	public void testRandomRedirect() throws Exception {
		FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/windows/run");
		replay();
		
		FullHttpResponse response = handler.respond(request, mockContext());
		assertRedirectTo(response, config.getHelpUrl());
	}
}

