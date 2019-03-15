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
package com.iris.platform.subsystem.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.InvalidRequestException;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.service.SubsystemService.ListSubsystemsRequest;
import com.iris.platform.subsystem.SubsystemRegistry;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;
import com.iris.util.IrisUUID;

/**
 * @author tweidlin
 *
 */
@Singleton
public class SubsystemExecutorResolver implements ArgumentResolverFactory<PlatformMessage, MessageBody> {
	@SuppressWarnings("serial")
	private final Type optionalSubsystemExecutor = (new TypeToken<Optional<SubsystemExecutor>>() {}).getType();
	
	private SubsystemRegistry registry;
	
	@Inject
	public SubsystemExecutorResolver(SubsystemRegistry registry) {
		this.registry = registry;
	}
	
	@Override
	public Function<? super PlatformMessage, ?> getResolverForParameter(Method method, Type parameter,	Annotation[] annotations) {
		if(SubsystemExecutor.class.equals(parameter)) {
			return this::getExecutor;
		}
		if(optionalSubsystemExecutor.equals(parameter)) {
			return this::getOptionalExecutor;
		}
		if(
				SubsystemContext.class.equals(parameter) ||
				(
					parameter instanceof ParameterizedType &&
					SubsystemContext.class.equals(((ParameterizedType) parameter).getRawType())
				)
		) {
			// FIXME should validate that this is a request handler, and that the context type matches the request
			return this::getSubsystemContext;
		}
		return null;
	}

	@Override
	public Function<Object, MessageBody> getResolverForReturnType(Method method) {
		return null;
	}
	
	private SubsystemExecutor getExecutor(PlatformMessage message) {
      UUID placeId = getPlaceId(message);
		return registry.loadByPlace(placeId).orElseThrow(() -> new NotFoundException(message.getDestination()));
	}
	
	private Optional<SubsystemExecutor> getOptionalExecutor(PlatformMessage message) {
		if(StringUtils.isEmpty(message.getPlaceId())) {
			return Optional.empty();
		}
      UUID placeId = StringUtils.isEmpty(message.getPlaceId()) ? null : IrisUUID.fromString(message.getPlaceId());
		return registry.loadByPlace(placeId);
	}
	
	private SubsystemContext<?> getSubsystemContext(PlatformMessage message) {
		SubsystemExecutor executor = getExecutor(message);
		SubsystemContext<?> context = executor.getContext(message.getDestination());
		Errors.assertFound(context, message.getDestination());
		return context;
	}

   protected UUID getPlaceId(PlatformMessage message) {
      Object id = message.getDestination().getId();
      if(id == null || Address.ZERO_UUID.equals(id)) {
         return getPlaceIdFromServiceRequest(message);
      }
      if(AlarmIncidentCapability.NAMESPACE.equals(message.getDestination().getGroup())) {
      	id = UUID.fromString(message.getPlaceId());
      }
      if(!(id instanceof UUID)) {
         throw new NotFoundException(message.getDestination());
      }
      return (UUID) id;
   }

   // TODO this should not have to be done by both the SubsystemService and the PlatformSubsystemExecutor
   protected UUID getPlaceIdFromServiceRequest(PlatformMessage message) {
      switch(message.getMessageType()) {
      case ListSubsystemsRequest.NAME:
         String placeId = ListSubsystemsRequest.getPlaceId(message.getValue());
         Errors.assertRequiredParam(placeId, ListSubsystemsRequest.ATTR_PLACEID);
         Errors.assertPlaceMatches(message, placeId);
         return IrisUUID.fromString(placeId);
      default:
      	if(StringUtils.isEmpty(message.getPlaceId())) {
      		Errors.assertValidRequest(false, "Must SetActivePlace first");
      	}
      	return IrisUUID.fromString(message.getPlaceId());
      }
   }

}

