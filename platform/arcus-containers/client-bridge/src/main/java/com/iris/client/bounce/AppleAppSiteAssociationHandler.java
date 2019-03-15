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

import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.core.template.TemplateService;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

@Singleton
@HttpGet("/apple-app-site-association")
public class AppleAppSiteAssociationHandler extends HttpResource {
	private final TemplateService service;
	private final Map<String, Object> templateContext;

	@Inject
	public AppleAppSiteAssociationHandler(
			AlwaysAllow authorizer, 
			BridgeMetrics metrics,
			BounceConfig config,
			TemplateService service
	) {
		super(authorizer, new HttpSender(AppleAppSiteAssociationHandler.class, metrics));
		this.service = service;
		this.templateContext = ImmutableMap.of("appIDs", config.getAppleIdList());
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		// FIXME this should be highly cache-able
		String contents = service.render("apple-app-site-association", templateContext);
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
		response.content().writeBytes(contents.getBytes(Charsets.UTF_8));
		return response;
	}
}

