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
package com.iris.core.platform;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.Errors;
import com.iris.util.IrisCollections;
import com.iris.util.ThreadPoolBuilder;

/**
 *
 */
@Singleton
public class PlatformServiceDispatcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlatformServiceDispatcher.class);

	private final Executor executor;
	private final PlatformMessageBus bus;
	private final Map<Address, PlatformService> services;

	@Inject
	public PlatformServiceDispatcher(
	      PlatformServiceConfig config,
	      PlatformMessageBus bus,
	      Set<PlatformService> services
   ) {
		this.bus = bus;
		this.services = IrisCollections.toUnmodifiableMap(services, (service) -> service.getAddress());
		this.executor =
		      new ThreadPoolBuilder()
		         .withMaxPoolSize(config.getThreads())
		         .withKeepAliveMs(config.getKeepAliveMs())
		         .withBlockingBacklog()
		         .withNameFormat("platform-service-%d")
		         .withMetrics("platform.service")
		         .build();
		         
	}

	@PostConstruct
	public void init() {
		this.bus.addMessageListener(AddressMatchers.platformServices(this.services.keySet()), (message) -> route(message));
		this.bus.addMessageListener(AddressMatchers.anyOf(Address.broadcastAddress()), (message -> route(message)));
	}

	public void route(PlatformMessage message) {
		// TODO should this handle all things in the "SERV:" namespace
		//      and return an error for unknown destinations?
		Address destination = message.getDestination();
		if(destination == null || destination.isBroadcast()) {
			LOGGER.trace("Dispatching broadcast message [{}] to all services", message);
			executor.execute(() -> services.values().forEach((s) -> { s.handleMessage(message); }));
			return;
		}

		Address serviceAddress = destination;
		if(serviceAddress.getId() != null) {
		   serviceAddress = Address.platformService((String) serviceAddress.getGroup());
		}

		PlatformService service = services.get(serviceAddress);
		if(service == null) {
			LOGGER.trace("Ignoring message to unknown destination [{}]", message);
			if(
			      MessageConstants.SERVICE.equals(destination.getNamespace()) &&
			      !destination.isHubAddress() &&
			      message.isRequest()
			) {
			   ErrorEvent error = Errors.unsuppportedAddress(destination);
			   sendError(message, error);
			}
			return;
		}

		executor.execute(() -> dispatch(service, message));
	}
	
	private void dispatch(PlatformService service, PlatformMessage message) {
	   try {
	      service.handleMessage(message);
	   }
      catch(Exception e) {
         LOGGER.debug("Received error while handling message [{}]", message, e);
         ErrorEvent error = Errors.fromException(e);
         sendError(message, error);
      }
	}

   private void sendError(PlatformMessage message, ErrorEvent error) {
      PlatformMessage responseMessage =
            PlatformMessage
               .buildResponse(message, error)
               .create();
      this.bus.send(responseMessage);
   }

}

