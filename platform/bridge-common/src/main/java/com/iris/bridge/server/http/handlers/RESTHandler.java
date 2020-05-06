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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.capability.util.Addresses;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.PlaceService;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

public abstract class RESTHandler extends HttpResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(RESTHandler.class);

	private final RESTHandlerConfig restHandlerConfig;

	public RESTHandler(RequestAuthorizer authorizer, HttpSender httpSender, RESTHandlerConfig restHandlerConfig) {
		super(authorizer, httpSender);
		this.restHandlerConfig = restHandlerConfig;
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
		ClientMessage request;
		MessageBody response;

		try {
			request = decode(req);
		} catch (Throwable t) {
			LOGGER.debug("Unable to decode request", t);
			return response(HttpResponseStatus.BAD_REQUEST, "plain/text", "Unable to decode request");
		}

		HttpResponseStatus status = HttpResponseStatus.OK;
		try {
		   /* preHandleValidation is typically a NOOP. However
		    * there may be times where a RESTHandler might need
		    * AUTH style checks that require access to the
		    * ChannelHandlerContext.
		    */
		   assertValidRequest(req, ctx);
			response = doHandle(request, ctx);
		} catch (Throwable th) {
			LOGGER.error("Error handling client message", th);
			response = Errors.fromException(th);
			status = overrideErrorResponseStatus(th);
			if(status == null) {
				status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			}
		}

		ClientMessage message = ClientMessage.builder().withCorrelationId(request.getCorrelationId())
				.withDestination(request.getSource()).withSource(Addresses.toServiceAddress(PlaceService.NAMESPACE))
				.withPayload(response).create();

		return response(status, BridgeHeaders.CONTENT_TYPE_JSON_UTF8, JSON.toJson(message));
	}

	protected void assertValidRequest(FullHttpRequest req, ChannelHandlerContext ctx) {
	   return;
	}
	
	protected abstract MessageBody doHandle(ClientMessage request) throws Exception;
	
	protected MessageBody doHandle(ClientMessage request, ChannelHandlerContext ctx) throws Exception {
		return doHandle(request);
	}

	protected ClientMessage decode(FullHttpRequest request) {
		return RESTHandlerUtil.decode(request);
	}

	protected FullHttpResponse response(HttpResponseStatus status, String contentType, String contents) {
		FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				Unpooled.copiedBuffer(contents, CharsetUtil.UTF_8));
		if (restHandlerConfig.isSendChunked()) {
			HttpUtil.setTransferEncodingChunked(httpResponse, true);
		}
		httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
		return httpResponse;
	}
	
	/**
	 * Allow its subclass to have a chance to handle the error scenario to return a different response status code from 
	 * the default HttpResponseStatus.INTERNAL_SERVER_ERROR
	 * @param error
	 * @return
	 */
	protected HttpResponseStatus overrideErrorResponseStatus(Throwable error) {
		return HttpResponseStatus.INTERNAL_SERVER_ERROR;
	}
}

