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
package com.iris.bridge.server.http.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.SessionActionHelper.Action;
import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.netty.server.session.IrisNettyClientClientToken;

@Singleton
@HttpPost("/adhocsession")
public class AdHocSessionAction extends HttpResource {

	private final SessionRegistry registry;
	private final ClientFactory factory;
	private static final String redirectUri = "/sessions";
	
	@Inject
	public AdHocSessionAction(SessionRegistry registry, ClientFactory factory, AlwaysAllow alwaysAllow, BridgeMetrics metrics) {
      super(alwaysAllow, new HttpSender(AdHocSessionAction.class, metrics));
      this.registry = registry;
      this.factory = factory;
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		doAction(req, ctx);
		DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		res.headers().set(HttpHeaders.Names.LOCATION, redirectUri);
		return res;
	}
	
	private void doAction(FullHttpRequest request, ChannelHandlerContext ctx) {
		HttpRequestParameters params = new HttpRequestParameters(request);
		String token = params.getParameter(SessionActionHelper.PARAM_TOKEN);
		Action action = SessionActionHelper.Action.valueOf(params.getParameter(SessionActionHelper.PARAM_ACTION));
		if(token != null && action != null) {
			Session curSession = findSessionByToken(token);
			if(curSession != null) {
				if(action == Action.LOGOUT) {
					factory.get(curSession.getChannel()).logout();
				}else if(action == Action.DISCONNECT) {
					curSession.destroy();
				}
			}
		}				
	}

	private Session findSessionByToken(String token) {
		IrisNettyClientClientToken clientToken = new IrisNettyClientClientToken(token);
		Session curSession = registry.getSession(clientToken);
		return curSession;		
	}

	
}

