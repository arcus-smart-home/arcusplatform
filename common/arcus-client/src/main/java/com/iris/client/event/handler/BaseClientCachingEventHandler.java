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
package com.iris.client.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.ClientEvent;
import com.iris.client.service.ClientCachingService;

public abstract class BaseClientCachingEventHandler<E extends ClientEvent> implements ClientCachingEventHandler<E>{
	public final Logger logger = LoggerFactory.getLogger(BaseClientCachingEventHandler.class);
	protected ClientCachingService cachingService;

	@Override
	public final void setCachingService(ClientCachingService cacheService) {
		this.cachingService = cacheService;
	}
}

