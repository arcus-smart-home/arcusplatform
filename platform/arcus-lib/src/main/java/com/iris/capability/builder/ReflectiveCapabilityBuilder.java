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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.bootstrap.guice.Injectors;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.driver.capability.Capability;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.model.Version;

/**
 *
 */
public class ReflectiveCapabilityBuilder extends CapabilityBuilder {
	CapabilityDefinition definition;
	Set<String> commands = new HashSet<>();

	ReflectiveCapabilityBuilder(CapabilityDefinition definition) {
	   super(definition);
	   this.definition = definition;
   }

   public boolean hasHandler(String command) {
      return commands.contains(command);
   }

	public Capability with(Object capability) {
		Preconditions.checkNotNull(capability, "capability");
		withInstanceId(Injectors.getServiceName(capability));
		withVersion(Injectors.getServiceVersion(capability, Version.UNVERSIONED));
		Class<?> type = capability.getClass();
		for(Method m: type.getMethods()) {
			discoverCommandHandlers(m, capability);
		}
		Set<String> commandsToImplement = new HashSet<>(definition.getCommands().keySet());
		commandsToImplement.remove("DiscoverDriver");
		commandsToImplement.removeAll(commands);
		if(!commandsToImplement.isEmpty()) {
		   throw new IllegalStateException("Missing method implementations for " + commandsToImplement);
		}
		return create();
	}

	private boolean discoverCommandHandlers(Method m, Object instance) {
		CommandDefinition command = getCommandFor(m);
		if(command == null) {
			return false;
		}
		ContextualEventHandler<MessageBody> handler = ReflectiveCommandHandler.create(command, m, instance);
		if(hasHandler(command.getCommand())) {
			throw new IllegalStateException("Multiple public methods named [" + command.getCommand() + "] have been defined, only one is allowed!");
		}
		addHandler(command, handler);
		commands.add(command.getCommand());
		return true;
	}

   private CommandDefinition getCommandFor(Method method) {
		String name = getName(method);
		return definition.getCommands().get(name);
	}

	private String getName(Method method) {
		// TODO support only @Named methods?
	   Named name = method.getAnnotation(Named.class);
	   if(name != null) {
	   	return name.value();
	   }
	   com.google.inject.name.Named googleNamed = method.getAnnotation(com.google.inject.name.Named.class);
	   if(googleNamed != null) {
	   	return googleNamed.value();
		}
	   return StringUtils.capitalize(method.getName());
   }


}

