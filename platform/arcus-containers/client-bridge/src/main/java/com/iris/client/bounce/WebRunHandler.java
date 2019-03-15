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

import java.net.InetSocketAddress;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.security.handoff.AppHandoffToken;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

@HttpGet(WebRunHandler.PATH)
@HttpGet(WebRunHandler.PATH + "?*")
@HttpGet(WebRunHandler.PATH + "/*")
@Singleton
public class WebRunHandler extends AbstractBounceHandler {
	private static final Logger logger = LoggerFactory.getLogger(WebRunHandler.class);
	
	public static final String PATH = "/web/run";
	
	private final String webUrl;
	private final Authenticator authenticator;
	private final ClientFactory factory;
	
	@Inject
	public WebRunHandler(
			AlwaysAllow authorizer, 
			BridgeMetrics metrics,
			BounceConfig config,
			Authenticator authenticator,
			ClientFactory factory
	) {
		super(authorizer, new HttpSender(WebLaunchHandler.class, metrics));
		this.webUrl = config.getWebUrl();
		this.authenticator = authenticator;
		this.factory = factory;
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		Client client = factory.get(ctx.channel());
		RequestInfo info = parseUrl(req, PATH);
		if(StringUtils.isEmpty(info.getToken())) {
			throw new HttpException(HttpResponseStatus.BAD_REQUEST, "Missing token");
		}
		try {
			AppHandoffToken authenticationToken = new AppHandoffToken(info.getToken());
			authenticationToken.setHost(((InetSocketAddress) ctx.channel().remoteAddress()).getHostString());
			authenticationToken.setRememberMe(true);
			client.login(authenticationToken);
			
			FullHttpResponse response = redirect(info.toQueryString(webUrl).toString());
			DefaultCookie cookie = authenticator.createCookie(client.getSessionId());
			response.headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie));
			return response;
		}
		catch(AuthenticationException e) {
			logger.debug("Failed to authenticate token, redirecting to web anyway");
			return redirect(info.toQueryString(webUrl).toString());
		}
	}

}

