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
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.*;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.bridge.server.netty.BridgeHeaders.OsType;
import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders.Names;

public abstract class AbstractBounceHandler extends HttpResource {
	private static final Logger logger = LoggerFactory.getLogger(AbstractBounceHandler.class);
	
	public static final String PARAM_OS = "os";
	public static final String PARAM_OSVERSION = "version";
	public static final String PARAM_TOKEN = "token";
	
	public static final String ATTR_LOCATION = "location";

	public AbstractBounceHandler(
			RequestAuthorizer authorizer, 
			HttpSender httpSender
	) {
		super(authorizer, httpSender);
	}

	@Nullable
	protected String getIp(Channel channel) {
		SocketAddress address = channel.remoteAddress();
		if(address instanceof InetSocketAddress) {
			return ((InetSocketAddress) address).getAddress().getHostAddress();
		}
		else {
			logger.warn("Non inet socket address from client: {}", address);
			return null;
		}
	}

	protected RequestInfo parseUrl(FullHttpRequest request, String pathPrefix) throws HttpException {
		QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
		String path = decoder.path();
		if(path.startsWith("http://") || path.startsWith("https://")) {
			try {
				path = new URL(path).getPath();
			} 
			catch (MalformedURLException e) {
				logger.warn("Unparseable path: [{}]", path, e);
				throw new HttpException(HttpResponseStatus.NOT_FOUND);
			}
		}
		if(!path.startsWith(pathPrefix)) {
			throw new HttpException(HttpResponseStatus.NOT_FOUND);
		}
		
		RequestInfo info = new RequestInfo();
		info.setSubpath(path.length() > pathPrefix.length() ? path.substring(pathPrefix.length(), path.length()) : "");
		for(Map.Entry<String, List<String>> e: decoder.parameters().entrySet()) {
			switch(e.getKey()) {
			case PARAM_OS:
				info.setType(parseOsType(e.getValue().isEmpty() ? "" : e.getValue().get(0)));
				break;
			case PARAM_OSVERSION:
				// no-op
				break;
			case PARAM_TOKEN:
				info.setToken(e.getValue().isEmpty() ? "" : e.getValue().get(0));
				break;
			default:
				info.addQueryParam(e.getKey(), e.getValue());
			}
		}
		if(info.getType() == null) {
			info.setType( BridgeHeaders.getOsType(request) );
		}
		return info;
	}
	
	@Nullable
	private OsType parseOsType(String reportedType) throws HttpException {
		reportedType = reportedType != null ? reportedType.trim().toLowerCase() : "";
		switch(reportedType) {
		case Session.TYPE_IOS:
			return OsType.IOS;
		case Session.TYPE_ANDROID:
			return OsType.ANDROID;
		case "":
			return null;
		default:
			throw new HttpException(HttpResponseStatus.BAD_REQUEST, "Invalid os type");
		}
	}
	
	protected FullHttpResponse redirect(String url) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		response.headers().add(HttpHeaderNames.LOCATION, url);
		response.headers().add(HttpHeaderNames.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
		response.content().writeBytes(JSON.toJson(ImmutableMap.of(ATTR_LOCATION, url)).getBytes(Charsets.UTF_8));
		return response;
	}

}

