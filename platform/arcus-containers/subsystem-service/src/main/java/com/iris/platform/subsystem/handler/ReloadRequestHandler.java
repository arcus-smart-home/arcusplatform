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
/**
 * 
 */
package com.iris.platform.subsystem.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.messages.PlatformMessage;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.service.SubsystemService;
import com.iris.platform.subsystem.SubsystemRegistry;

@Singleton
public class ReloadRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(ReloadRequestHandler.class);
	
	private final SubsystemRegistry registry;
	
	@Inject
	public ReloadRequestHandler(SubsystemRegistry registry) {
		this.registry = registry;
	}
	
	@Request(value = SubsystemService.ReloadRequest.NAME, service = true)
	public void handleRequest(SubsystemExecutor executor, PlatformMessage message) {
		logger.debug("Flushing subsystems for {}", message.getPlaceId());
		UUID placeId = UUID.fromString(message.getPlaceId());
		registry.removeByPlace(placeId);
	}

}

