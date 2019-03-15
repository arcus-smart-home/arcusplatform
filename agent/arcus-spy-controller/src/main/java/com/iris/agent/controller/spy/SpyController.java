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
package com.iris.agent.controller.spy;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.agent.router.Port;
import com.iris.agent.router.Router;
import com.iris.agent.router.SnoopingPortHandler;
import com.iris.agent.spy.SpyService;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

/**
 * The Spy Controller is used to manage diagnostic information about the hub mostly
 * for the purposes of development.
 * 
 * In order to activate the Spy Service the environment variable IRIS_HUB_SPY_ACTIVE
 * must be defined.
 * 
 * @author Erik Larson
 *
 */
@Singleton
public class SpyController implements SnoopingPortHandler {
	
	@Inject
	public SpyController(Router router) {
		router.connect("spy", this);
	}

	@Override
	@Nullable
	public Object recv(Port port, PlatformMessage message) throws Exception {
		// The Spy Controller doesn't process messages.
		return null;
	}

	@Override
	public void recv(Port port, ProtocolMessage message) {
		// The Spy Controller doesn't process messages.
	}

	@Override
	public void recv(Port port, Object message) {
		// The Spy Controller doesn't process messages.
	}

	@Override
	public boolean isInterestedIn(PlatformMessage msg) {
		SpyService.INSTANCE.spyOnPlatformMessage(msg);
		// The Spy Controller is never interested in any messages.
		return false;
	}

	@Override
	public boolean isInterestedIn(ProtocolMessage msg) {
		
		// The Spy Controller is never interested in any messages.
		return false;
	}

}

