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
package com.iris.platform.model;

import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.platform.model.handler.AddTagsRequestHandler;
import com.iris.platform.model.handler.GetAttributesRequestHandler;
import com.iris.platform.model.handler.RemoveTagsRequestHandler;
import com.iris.platform.model.handler.SetAttributesRequestHandler;

/**
 * Marks the type() as OBJECT and sets up a default matcher based on
 * the assumption that name() is the namespace.
 *  
 * @author tweidlin
 */
public abstract class ObjectDispatcherModule extends CapabilityDispatcherModule {

	@Override
	protected AddressMatcher matcher() {
		return AddressMatchers.fromString(String.format("SERV:%s:*", name()));
	}
	
	@Override
	public Type type() {
		return Type.OBJECT;
	}

	/**
	 * Binds handlers for the base capability namespace.
	 * If customizations to these methods are needed this should not be called
	 * and the handlers should be added individually.
	 */
	protected void bindBaseHandlers() {
		annotatedObjects().addBinding().to(GetAttributesRequestHandler.class);
		annotatedObjects().addBinding().to(SetAttributesRequestHandler.class);
		annotatedObjects().addBinding().to(AddTagsRequestHandler.class);
		annotatedObjects().addBinding().to(RemoveTagsRequestHandler.class);
	}

}

