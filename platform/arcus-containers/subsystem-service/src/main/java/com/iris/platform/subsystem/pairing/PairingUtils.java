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
package com.iris.platform.subsystem.pairing;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.iris.bootstrap.ServiceLocator;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.model.predicate.Predicates;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing.ProductCacheInfo;
import com.iris.platform.subsystem.pairing.state.PairingStateName;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.type.LooselyTypedReference;
import com.iris.util.LazyReference;

public enum PairingUtils {
	INSTANCE;
	
	public static final Date DEFAULT_TIMEOUT = new Date(0);
	
	private static final Predicate<Model> isPairingDevice = Predicates.isA(PairingDeviceCapability.NAMESPACE);
	private static final LazyReference<PairingConfig> PairingConfigRef = LazyReference.fromCallable(() -> ServiceLocator.getInstance(PairingConfig.class));
	
	private enum Variable {
		bridgeInfo,
		mock,
		zwaveRebuild,
		pendingPairingState,
		;
		
		public LooselyTypedReference get(SubsystemContext<?> context) {
			return context.getVariable(name());
		}
		
		public boolean isSet(SubsystemContext<?> context) {
			return !context.getVariable(name()).isNull();
		}
		
		public void set(SubsystemContext<?> context, Object value) {
			context.setVariable(name(), value);
		}

		public void set(SubsystemContext<?> context) {
			context.setVariable(name(), true);
		}

		public void clear(SubsystemContext<?> context) {
			context.setVariable(name(), null);
		}
	}
	
	public static Predicate<Model> isPairingDevice() {
		return isPairingDevice;
	}
	
	public static boolean isPairingDevice(Model m) {
		return isPairingDevice.apply(m);
	}

	// FIXME move to subsystem utils
	public static Optional<Model> getHubModel(SubsystemContext<?> context) {
		Model hub = Iterables.getFirst(context.models().getModelsByType(HubCapability.NAMESPACE), null);
		return Optional.ofNullable(hub);
	}

	public static PairingConfig config() {
		return PairingConfigRef.get();
	}
	
	public static long idleTimeoutMs() {
		return config().getIdleTimeoutMs();
	}
	
	public static long pairingTimeoutMs() {
		return config().getPairingTimeoutMs();
	}
	
	public static long pairingTimeoutMs(SubsystemContext<PairingSubsystemModel> context) {
		ProductCacheInfo info = ProductCacheInfo.get(context);
		if(info != null && info.getPairingTimeoutMs() != null) {
			return info.getPairingTimeoutMs();
		}else{
			return config().getPairingTimeoutMs();
		}
	}
	
	public static long pairingTimeoutMs(Optional<ProductCatalogEntry> product) {
		if(product != null) {
			Integer value = product.map(ProductCatalogEntry::getPairingTimeoutMs).orElse(null);
			if(value != null) {
				return value.longValue();
			}
		}
			
		return config().getPairingTimeoutMs();
	}
	
	public static long requestTimeoutMs() {
		return config().getRequestTimeoutMs();
	}
	
	public static Optional<BridgePairingInfo> getBridgePairingInfo(SubsystemContext<PairingSubsystemModel> context) {
		BridgePairingInfo info = Variable.bridgeInfo.get(context).as(BridgePairingInfo.class);
		return Optional.ofNullable(info);
	}
	
	public static void setBridgePairingInfo(SubsystemContext<PairingSubsystemModel> context, BridgePairingInfo info) {
		Variable.bridgeInfo.set(context, info);
	}
	
	public static void clearBridgePairingInfo(SubsystemContext<PairingSubsystemModel> context) {
		Variable.bridgeInfo.clear(context);
	}
	
	public static boolean isMockPairing(SubsystemContext<PairingSubsystemModel> context) {
		return Variable.mock.isSet(context);
	}
	
	public static void setMockPairing(SubsystemContext<PairingSubsystemModel> context) {
		Variable.mock.set(context);
	}

	public static void clearMockPairing(SubsystemContext<PairingSubsystemModel> context) {
		Variable.mock.clear(context);
	}
	
	public static boolean isZWaveRebuildRequired(SubsystemContext<PairingSubsystemModel> context) {
		return Variable.zwaveRebuild.isSet(context);
	}
	
	public static void setZWaveRebuildRequired(SubsystemContext<PairingSubsystemModel> context) {
		Variable.zwaveRebuild.set(context);
	}

	public static void clearZWaveRebuildRequired(SubsystemContext<PairingSubsystemModel> context) {
		Variable.zwaveRebuild.clear(context);
	}
	
	public static Optional<HubPairingInfo> getHubPairingInfo(SubsystemContext<PairingSubsystemModel> context) {
		LooselyTypedReference ref = Variable.pendingPairingState.get(context);
		return ref.isNull() ? Optional.empty() : Optional.of(ref.as(HubPairingInfo.class));
	}

	// use a version long to make sure failures don't cancel out a subsequent version of the pending state
	// start with a timestamp so that cancelling in a spot we weren't expecting doesn't reset us back to
	// 0 and result in a still pending operation cancelling its replacement
	
	public static long setPairingStepsPending(SubsystemContext<PairingSubsystemModel> context, String productAddress) {
		return setPendingState(context, PairingStateName.PairingSteps, productAddress);
	}

	public static long setSearchingPending(SubsystemContext<PairingSubsystemModel> context) {
		return setPendingState(context, PairingStateName.Searching, context.model().getSearchProductAddress());
	}
	
	public static long setSearchingPending(SubsystemContext<PairingSubsystemModel> context, String productAddress) {
		return setPendingState(context, PairingStateName.Searching, productAddress);
	}
	
	public static long setFactoryResetPending(SubsystemContext<PairingSubsystemModel> context) {
		return setPendingState(context, PairingStateName.FactoryResetSteps, context.model().getSearchProductAddress());
	}
	
	public static void clearPendingPairingStateIf(SubsystemContext<PairingSubsystemModel> context, long version) {
		LooselyTypedReference ref = Variable.pendingPairingState.get(context);
		if(ref.isNull()) {
			return; // already cleared
		}
		if(ref.as(HubPairingInfo.class).getVersion() != version) {
			return; // its been replaced
		}
		clearPendingPairingState(context);
	}

	public static void clearPendingPairingState(SubsystemContext<PairingSubsystemModel> context) {
		Variable.pendingPairingState.clear(context);
	}
	
	/**
	 * Only set SearchProductAddress on the PairingSubsystemModel if the newSearchProductAddress value is 
	 * different from the existing one.  If it is different, will also create a new ProductCacheInfo to be 
	 * saved in PairingSubsystemModel context.
	 * @param context
	 * @param newSearchProductAddress
	 */
	public static void setSearchProductAddressIfNecessary(SubsystemContext<PairingSubsystemModel> context, String newSearchProductAddress) {
		String existingProductAddr = context.model().getSearchProductAddress("");
		if(!existingProductAddr.equals(newSearchProductAddress)) {
			context.model().setSearchProductAddress(newSearchProductAddress);
			if(StringUtils.isNotBlank(newSearchProductAddress) ) {
				ProductLoaderForPairing loader = ServiceLocator.getInstance(ProductLoaderForPairing.class);
				Optional<ProductCatalogEntry> product = loader.get(context, newSearchProductAddress);
				ProductCacheInfo.saveOrClear(context, product);
			}
		}
	}
	
	private static long setPendingState(SubsystemContext<PairingSubsystemModel> context, PairingStateName state, String productAddress) {
		LooselyTypedReference current = Variable.pendingPairingState.get(context);
		long version = System.currentTimeMillis();
		if(!current.isNull()) {
			version = current.as(HubPairingInfo.class).getVersion() + 1;
		}
		Variable.pendingPairingState.set(context, new HubPairingInfo(state, productAddress, version));
		return version;
	}

	

}

