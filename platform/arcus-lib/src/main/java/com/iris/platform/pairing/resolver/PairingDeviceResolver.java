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
package com.iris.platform.pairing.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.UUID;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.common.base.Function;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

@Singleton
public class PairingDeviceResolver implements ArgumentResolverFactory<PlatformMessage, MessageBody> {
	private static final Type PairingDeviceWrapper = new TypeToken<PersistentModelWrapper<PairingDevice>>() { }.getType();

	private final PlatformMessageBus messageBus;
	private final PairingDeviceDao pairingDeviceDao;
	protected final PlacePopulationCacheManager populationCacheMgr;

	@Inject
	public PairingDeviceResolver(
			PlatformMessageBus messageBus,
			PairingDeviceDao pairingDeviceDao,
			PlacePopulationCacheManager populationCacheMgr
	) {
		this.messageBus = messageBus;
		this.pairingDeviceDao = pairingDeviceDao;
		this.populationCacheMgr = populationCacheMgr;
	}
	
	@Override
	public Function<? super PlatformMessage, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
		if(PairingDevice.class.equals(parameter)) {
			return this::findPairingDevice;
		}
		if(PersistentModelWrapper.class.equals(TypeUtils.getRawType(parameter, null)) && TypeUtils.isAssignable(PairingDeviceWrapper, parameter)) {
			return this::findPairingDeviceWrapper;
		}
		return null;
	}

	@Override
	public Function<Object, MessageBody> getResolverForReturnType(Method method) {
		return null;
	}

	public PairingDevice findPairingDevice(PlatformMessage message) {
		Errors.assertPlaceMatches(message, (UUID) message.getDestination().getId());
		PairingDevice device = pairingDeviceDao.findByAddress(message.getDestination());
		Errors.assertFound(device, message.getDestination());
		return device;
	}

	public PersistentModelWrapper<PairingDevice> findPairingDeviceWrapper(PlatformMessage message) {
		PairingDevice device = findPairingDevice(message);
		return new PersistentModelWrapper<PairingDevice>(messageBus, pairingDeviceDao, device.getPlaceId(), populationCacheMgr.getPopulationByPlaceId(device.getPlaceId()), device);
	}

}

