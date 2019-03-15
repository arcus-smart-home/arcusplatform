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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.security.dao.AppHandoffDao;
import com.iris.security.dao.AppHandoffDao.SessionHandoff;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringEncoder;

@HttpGet(AppLaunchHandler.PATH)
@HttpGet(AppLaunchHandler.PATH + "?*")
@HttpGet(AppLaunchHandler.PATH + "/*")
@Singleton
public class AppLaunchHandler extends AbstractBounceHandler {
	private static final Logger logger = LoggerFactory.getLogger(AppLaunchHandler.class);
	
	public static final String PATH = "/app/launch";
	
	private static final String URL_FORMAT = "%s/%s/run";
	
	private final String appHost;
	private final AppHandoffDao appHandoffDao;
	private final ClientFactory factory;
	
	@Inject
	public AppLaunchHandler(
			SessionAuth authorizer, 
			BridgeMetrics metrics,
			BounceConfig config,
			AppHandoffDao appHandoffDao,
			ClientFactory factory
	) {
		super(authorizer, new HttpSender(AppLaunchHandler.class, metrics));
		this.appHost = config.getAppUrl();
		this.appHandoffDao = appHandoffDao;
		this.factory = factory;
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		Client client = factory.get(ctx.channel());
		QueryStringEncoder encoder = getUrl(req);
		SessionHandoff handoff = new SessionHandoff();
		handoff.setIp(getIp(ctx.channel()));
		handoff.setPersonId(client.getPrincipalId());
		handoff.setUrl(encoder.toString());
		handoff.setUsername(client.getPrincipalName());
		String token = appHandoffDao.newToken(handoff);
		encoder.addParam(PARAM_TOKEN, token);
		return redirect(encoder.toString());
	}

	private QueryStringEncoder getUrl(FullHttpRequest request) throws Exception {
		RequestInfo info = parseUrl(request, PATH);
		logger.debug("Extracted request info [{}]", info);
		String prefix = String.format(URL_FORMAT, appHost, info.getType().name().toLowerCase());
		return info.toQueryString(prefix);
	}
	
}

