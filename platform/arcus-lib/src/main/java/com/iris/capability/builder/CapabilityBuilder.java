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
package com.iris.capability.builder;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.capability.key.NamespacedKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.capability.Capability;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.MessageBodyHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.model.CapabilityId;
import com.iris.model.Version;

/**
 *
 */
public class CapabilityBuilder {
	private CapabilityDefinition definition;
	private String instanceId;
	private String hash;
	private Version version = Version.UNVERSIONED;
	private AttributeMap attributes = AttributeMap.newMap();
	private MessageBodyHandler.Builder messageHandlerBuilder = MessageBodyHandler.builder();

	CapabilityBuilder(CapabilityDefinition definition) {
		Preconditions.checkNotNull(definition, "definition");
		this.definition = definition;
	}

	public CapabilityBuilder withInstanceId(String instanceId) {
		this.instanceId = instanceId;
		return this;
	}

	public CapabilityBuilder withVersion(Version version) {
		this.version = version;
		return this;
	}
	
	public CapabilityBuilder withHash(String hash) {
	   this.hash = hash;
	   return this;
	}

	public <V> CapabilityBuilder addAttributeValue(AttributeValue<V> value) {
	   if(!value.getKey().getName().startsWith(definition.getNamespace() + ":")) {
	      throw new IllegalArgumentException("Can't add an attribute from another capability, invalid key [" + value.getKey() + "]");
	   }
	   attributes.add(value);
	   return this;
	}

   public CapabilityBuilder addHandler(ContextualEventHandler<? super MessageBody> handler) {
      Preconditions.checkNotNull(handler, "handler");
      messageHandlerBuilder.addHandler(NamespacedKey.of(definition.getNamespace()), handler);
      return this;
   }

	public CapabilityBuilder addHandler(CommandDefinition definition, ContextualEventHandler<? super MessageBody> handler) {
		Preconditions.checkNotNull(definition, "definition");
		return addHandler(definition.getCommand(), handler);
	}

	public CapabilityBuilder addHandler(String command, ContextualEventHandler<? super MessageBody> handler) {
		Preconditions.checkNotNull(command, "command");
		Preconditions.checkNotNull(handler, "handler");
		if(!definition.getCommands().containsKey(command)) {
			throw new IllegalStateException("Command [" + command + "] is not defined in [" + definition.getNamespace() + "]");
		}
		messageHandlerBuilder.addHandler(NamespacedKey.of(definition.getNamespace(), command), handler);
		return this;
	}

	public Capability create() {
		if(StringUtils.isEmpty(instanceId)) {
			throw new IllegalStateException("Must specify 'instanceId'");
		}
      CapabilityId capabilityId = new CapabilityId(definition.getCapabilityName(), instanceId, version);
		return new Capability(capabilityId, definition, hash, attributes, null, messageHandlerBuilder.build(), null, Collections.emptyList(), Collections.emptyList());
	}

}

