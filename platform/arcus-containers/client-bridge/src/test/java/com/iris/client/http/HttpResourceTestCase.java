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
package com.iris.client.http;

import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.client.bounce.AbstractBounceHandler;
import com.iris.io.json.JSON;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.TypeMarker;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

@Mocks({ 
	Channel.class,
	ChannelHandlerContext.class,
	Client.class,
	ClientFactory.class
})
public class HttpResourceTestCase extends IrisMockTestCase {

	static {
		System.setProperty("bounce.android.url", "http://fakeurl");
		System.setProperty("bounce.apple.url", "http://fakeurl");
		System.setProperty("ios.app.ids", "foobar");
	}

	@Inject 
	private Channel mockChannel;
	@Inject 
	private ChannelHandlerContext mockContext;
	@Inject 
	private Client mockClient;
	@Inject 
	private ClientFactory mockFactory;
	
	@Override
	protected Set<String> configs() {
		Set<String> configs = super.configs();
		configs.add("src/test/resources/client-bridge.properties");
		return configs;
	}
	
	@Provides
	public BridgeMetrics metrics() {
		return new BridgeMetrics("test-case");
	}
	
	protected Channel mockChannel() { return mockChannel; }
	protected ChannelHandlerContext mockContext() { return mockContext; }
	protected Client mockClient() { return mockClient; }
	protected ClientFactory mockFactory() { return mockFactory; }

	protected void expectGetClient() {
		EasyMock
			.expect(mockFactory().get(mockChannel()))
			.andReturn(mockClient());
	}
	
	protected void assertRedirectTo(FullHttpResponse response, String url) {
		assertEquals(HttpResponseStatus.FOUND, response.getStatus());
		assertEquals(url, response.headers().get(HttpHeaders.Names.LOCATION));
		assertEquals(BridgeHeaders.CONTENT_TYPE_JSON_UTF8, response.headers().get(HttpHeaders.Names.CONTENT_TYPE));
		byte[] bytes = new byte[response.content().readableBytes()];
		response.content().readBytes(bytes);
		Map<String, String> contents = JSON.fromJson(new String(bytes, Charsets.UTF_8), TypeMarker.mapOf(String.class));
		assertEquals(url, contents.get(AbstractBounceHandler.ATTR_LOCATION));
	}

}

