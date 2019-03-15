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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.http.impl.matcher.RegexMatcher;
import com.iris.common.rule.filter.MatcherFilter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.apache.commons.lang3.StringUtils;

/**
 * If this request comes through, it means the app isn't installed
 * or isn't at a new enough version, redirect to the store.
 * 
 * If we can't tell which store would be appropriate, redirect
 * to a help page.
 * 
 * @author ted
 *
 */
@Singleton
public class AppFallbackHandler extends AbstractBounceHandler {
	private static final RequestMatcher MATCHER = new RegexMatcher("/(android|ios|other)/run(/.*)?(\\?.*)?", HttpMethod.GET);
	
	private final String androidStoreUrl;
	private final String appleStoreUrl;
	private final String helpUrl;
	
	@Inject
	public AppFallbackHandler(
			AlwaysAllow authorizer, 
			BridgeMetrics metrics,
			BounceConfig config
	) {
		super(authorizer, new HttpSender(WebRunHandler.class, metrics));
		this.androidStoreUrl = config.getAndroidStoreUrl();
		this.appleStoreUrl = config.getAppleStoreUrl();
		this.helpUrl = config.getHelpUrl();
	}

	@Override
	public boolean matches(FullHttpRequest req) {
		return MATCHER.matches(req);
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		String path = new QueryStringDecoder(req.getUri()).path();
		if(path.startsWith("/ios/")) {
			return redirect(appleStoreUrl);
		}
		if(path.startsWith("/android/")) {
			RequestInfo info = parseUrl(req, "");

			if(!StringUtils.isEmpty(info.getToken()) && androidStoreUrl.contains("<token>"))
					return redirect(androidStoreUrl.replace("<token>", info.getToken()));
			else
				return redirect(androidStoreUrl);
		}
		return redirect(helpUrl);
	}

	
}

