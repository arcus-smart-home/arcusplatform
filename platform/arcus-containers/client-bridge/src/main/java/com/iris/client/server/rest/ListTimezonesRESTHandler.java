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
package com.iris.client.server.rest;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.service.PlaceService;
import com.iris.platform.location.TimezonesManager;
import com.netflix.governator.annotations.WarmUp;

@Singleton
@HttpPost("/" + PlaceService.NAMESPACE + "/ListTimezones")
public class ListTimezonesRESTHandler extends RESTHandler {
	private static final Logger logger = LoggerFactory.getLogger(ListTimezonesRESTHandler.class);


	private final TimezonesManager tzResourceManager;

	@Inject
	public ListTimezonesRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics,
			TimezonesManager tzResourceManager, RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(ListTimezonesRESTHandler.class, metrics),restHandlerConfig);
		this.tzResourceManager = tzResourceManager;
	}

	@WarmUp
	public void start() {

	}

	
	@Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {
		List<Map<String, Object>> timezones = tzResourceManager.getParsedData();
		if (timezones == null) {
			throw new ErrorEventException("notavailable", "Service is not currently available");
		}
		return PlaceService.ListTimezonesResponse.builder().withTimezones(timezones).build();
	}

}

