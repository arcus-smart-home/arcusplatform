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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.core.dao.HubDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllRequest;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetRequest;
import com.iris.messages.capability.PairingSubsystemCapability.GetKitInformationRequest;
import com.iris.messages.capability.PairingSubsystemCapability.GetKitInformationResponse;
import com.iris.messages.capability.PairingSubsystemCapability.ListHelpStepsRequest;
import com.iris.messages.capability.PairingSubsystemCapability.ListPairingDevicesRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SearchRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StopSearchingRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.type.KitDeviceId;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.manufacture.kitting.kit.Kit;
import com.iris.platform.manufacture.kitting.kit.KitDevice;
import com.iris.platform.subsystem.pairing.attribute.Initializer;
import com.iris.platform.subsystem.pairing.handler.DismissAllHandler;
import com.iris.platform.subsystem.pairing.handler.ListHelpStepsHandler;
import com.iris.platform.subsystem.pairing.handler.ListPairingDevicesHandler;
import com.iris.platform.subsystem.pairing.handler.StopSearchingHandler;
import com.iris.platform.subsystem.pairing.state.PairingStateMachine;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.util.KitUtil;

@Singleton
@Subsystem(PairingSubsystemModel.class)
@Version(1)
public class PairingSubsystem extends BaseSubsystem<PairingSubsystemModel> {
	@Inject private PairingConfig config; // transitive dependency for many bits of the pairing system
	
	@Inject private ProductLoaderForPairing loader;

	@Inject private Initializer initializer;
	@Inject private ListPairingDevicesHandler listPairingDevicesHandler;
	@Inject private ListHelpStepsHandler listHelpStepsHandler;
	@Inject private StopSearchingHandler stopSearching;
	@Inject private DismissAllHandler dismissHandler;
	
	@Inject private ManufactureKittingDao kitDao;
	@Inject private HubDAO hubDao;

	@Override
	protected void onAdded(SubsystemContext<PairingSubsystemModel> context) {
		super.onAdded(context);
		initializer.onAdded(context);
	}

	@Override
	protected void onStarted(SubsystemContext<PairingSubsystemModel> context) {
		super.onStarted(context);
		initializer.onStarted(context);
		PairingStateMachine.get(context).onStarted();
	}

	@OnScheduledEvent
	public void onEvent(SubsystemContext<PairingSubsystemModel> context, ScheduledEvent event) {
		PairingStateMachine.get( context ).onTimeout( event );
	}
	
	@OnValueChanged(attributes=HubCapability.ATTR_STATE)
	public void onEvent(SubsystemContext<PairingSubsystemModel> context, Model hub) {
		String state = HubModel.getState(hub, HubCapability.STATE_NORMAL);
		switch(state) {
		case HubCapability.STATE_PAIRING:
			// currently ignoring this transition as its handled directly by the search request so that we
			// don't flap the pairing mode attribute for legacy apps
			break;
		case HubCapability.STATE_UNPAIRING:
			// currently ignoring this transition as its handled directly by the search request so that we
			// don't flap the pairing mode attribute for legacy apps
			break;
		case HubCapability.STATE_NORMAL:
			PairingStateMachine.get(context).onHubUnpairing();
			break;
		case HubCapability.STATE_DOWN:
			// FIXME should this be propagated to the current state somehow?  seems important but not sure what it would do
			break;
		default:
			context.logger().warn("Hub reported unsupported state [{}]", state);
		}
	}

	@Request(ListPairingDevicesRequest.NAME)
	public MessageBody listPairingDevicesRequest(SubsystemContext<PairingSubsystemModel> context) {
		return listPairingDevicesHandler.listPairingDevices(context.models());
	}

	@Request(StartPairingRequest.NAME)
	public MessageBody startPairing(
			SubsystemContext<PairingSubsystemModel> context,
			PlatformMessage request,
			@Named(StartPairingRequest.ATTR_PRODUCTADDRESS) String productAddress,
			@Named(StartPairingRequest.ATTR_MOCK)           Boolean mock
	) {
		ProductCatalogEntry product = loader.get(context, productAddress).orElseThrow(() -> new ErrorEventException(Errors.CODE_MISSING_PARAM, StartPairingRequest.ATTR_PRODUCTADDRESS));
		if(Boolean.TRUE.equals(mock)) {
			PairingUtils.setMockPairing(context);
		}
		return PairingStateMachine.get( context ).startPairing(request, product);
	}

	@Request(SearchRequest.NAME)
	public MessageBody search(
			SubsystemContext<PairingSubsystemModel> context,
			PlatformMessage request,
			@Named(SearchRequest.ATTR_PRODUCTADDRESS) String productAddress,
			@Named(SearchRequest.ATTR_FORM)           Map<String, Object> form
	) {
		if(StringUtils.isEmpty(productAddress)) {
			productAddress = context.model().getSearchProductAddress();
		}
		Optional<ProductCatalogEntry> product;
		if(StringUtils.isEmpty(productAddress)) {
			product = Optional.empty();
		}
		else {
			product = loader.get(context, productAddress);
			Errors.assertValidRequest(product.isPresent(), "Invalid productAddress");
		}
		form = Optional.ofNullable( form ).orElse( ImmutableMap.of() );
		return PairingStateMachine.get( context ).search( request, product, form );
	}
	
	@Request(FactoryResetRequest.NAME)
	public MessageBody factoryReset(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request) {
		if(StringUtils.isEmpty(context.model().getSearchProductAddress(""))) {
			throw new PairingSubsystemCapability.RequestStateInvalidException("Can't factory reset while there is no product selected to pair");
		}
		ProductCatalogEntry product = loader.getCurrent(context).orElseThrow(() -> new IllegalStateException("Unable to load selected product"));
		return PairingStateMachine.get( context ).factoryReset( request, product );
	}
	
	@Request(ListHelpStepsRequest.NAME)
	public MessageBody listHelpSteps(SubsystemContext<PairingSubsystemModel> context) {
		return listHelpStepsHandler.listHelpSteps(context);
	}
	
	@Request(StopSearchingRequest.NAME)
	public MessageBody stopSearching(SubsystemContext<PairingSubsystemModel> context) {
		return stopSearching.stopSearching(context);
	}
	
	@Request(DismissAllRequest.NAME)
	public MessageBody dismissAll(SubsystemContext<PairingSubsystemModel> context) {
		return dismissHandler.dismissAll(context);
	}
	
	@Request(GetKitInformationRequest.NAME)
	public MessageBody getKitInformation(
			SubsystemContext<PairingSubsystemModel> context,
			PlatformMessage request) {
		
		Hub hub = hubDao.findHubForPlace(context.getPlaceId());
		if(hub == null) {
			throw new PairingSubsystemCapability.HubMissingException("Hub is not part of a kit");
		}
		String hubId = hub.getId();
		List<Map<String,Object>> devices = new ArrayList<>();		
		Kit kit = kitDao.getKit(hubId);
		if (kit != null) {
			for (KitDevice device : kit.getSortedDevices()) {
				String type = KitUtil.getProductId(device.getType());
				String protocolId = Address.hubProtocolAddress(hubId, "ZIGB", ProtocolDeviceId.fromRepresentation(KitUtil.zigbeeIdToProtocolId(device.getEuid()))).toString();
				KitDeviceId kitId = new KitDeviceId();
				kitId.setProductId(type);
				kitId.setProtocolAddress(protocolId);
				devices.add(kitId.toMap());
			}
		}
		
		return 
				GetKitInformationResponse
				.builder()
				.withKitInfo(devices)
				.build();
	}
	

}

