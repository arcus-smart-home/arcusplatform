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
package com.iris.platform.subsystem.pairing.state;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubCapability.PairingRequestRequest;
import com.iris.messages.capability.HubCapability.UnpairingRequestRequest;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.iris.messages.capability.PairingSubsystemCapability.SearchRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingResponse;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.type.PairingApplication;
import com.iris.messages.type.PairingInput;
import com.iris.messages.type.PairingStep;
import com.iris.platform.subsystem.pairing.BridgePairingInfo;
import com.iris.platform.subsystem.pairing.HubPairingInfo;
import com.iris.platform.subsystem.pairing.PairingProtocol;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing.ProductCacheInfo;
import com.iris.platform.subsystem.state.State;
import com.iris.prodcat.ExternalApplication;
import com.iris.prodcat.ExternalApplication.PlatformType;
import com.iris.prodcat.Input;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.Step;

public abstract class PairingState implements State<PairingSubsystemModel> {
	// FIXME move to I18N
	private static final String TXT_INSTALL_LINK = "Manufacturer Instructions";	
	
	private PairingStateName stateName;
	
	protected PairingState(PairingStateName stateName) {
		this.stateName = stateName;
	}

	protected void resetSearchTimeout(SubsystemContext<PairingSubsystemModel> context, Optional<ProductCatalogEntry> product) {		
		Date timeout = setTimeout(context, PairingUtils.pairingTimeoutMs(product));
		context.model().setSearchTimeout(timeout);
	}
	
	@Nullable
	protected ProductCacheInfo resetSearchTimeout(SubsystemContext<PairingSubsystemModel> context) {	
		Date timeout = null;
		ProductCacheInfo cacheInfo = null;
		if(StringUtils.isNotBlank(context.model().getSearchProductAddress())) {
			cacheInfo = ProductCacheInfo.get(context);
			if(cacheInfo != null && cacheInfo.getPairingTimeoutMs() != null) {
				timeout = setTimeout(context, cacheInfo.getPairingTimeoutMs().longValue());
			}			
		}
		if(timeout == null) {
			timeout = setTimeout(context, PairingUtils.pairingTimeoutMs());			
		}		
		context.model().setSearchTimeout(timeout);
		return cacheInfo;
	}
	
	protected void startHubPairing(
			SubsystemContext<PairingSubsystemModel> context,
			PlatformMessage request,
			Optional<ProductCatalogEntry> entry,
			Function<SubsystemContext<PairingSubsystemModel>, MessageBody> generateSuccessResponse
	) {
		Model hub = assertHubExistAndNotOffline(context);
		
		if(HubModel.isStateUNPAIRING(hub)) {
			stopHubUnpairing(context, hub.getAddress());
		}
		MessageBody pairingRequest =
				HubCapability.PairingRequestRequest
					.builder()
					.withActionType(HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING)
					.withTimeout(PairingUtils.pairingTimeoutMs(entry))
					.withProductPairingMode(getProductPairingMode(entry))
					.build();
		long version;
		switch(request.getMessageType()) {
		case StartPairingRequest.NAME:
			version = PairingUtils.setPairingStepsPending(context, entry.map(ProductCatalogEntry::getAddress).orElse(""));
			break;
		case SearchRequest.NAME:
			version = PairingUtils.setSearchingPending(context, entry.map(ProductCatalogEntry::getAddress).orElse(""));
			break;
		default:
			throw new IllegalArgumentException("Unsupported message type [" + request.getMessageType() + "] for triggering pairing");
		}
		sendHubRequest(context, pairingRequest, new ResponseAction<PairingSubsystemModel>() {
			@Override
			public void onResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
				try {
					PairingStateMachine.get( context ).onHubPairing();
					context.sendResponse(request, generateSuccessResponse.apply(context));
					PairingUtils.clearPendingPairingStateIf(context, version);
				}
				catch(Exception e) {
					context.sendResponse(request, Errors.fromException(e));
				}
			}
			
			@Override
			public void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
				ErrorEvent error = Errors.fromException(new PairingSubsystemCapability.HubOfflineException("Timeout contacting hub"));
				PairingStateMachine.get( context ).onHubPairingFailed(error.getMessage());
				context.sendResponse(request, error);
				PairingUtils.clearPendingPairingStateIf(context, version);
			}
			
			@Override
			public void onError(SubsystemContext<PairingSubsystemModel> context, Throwable cause) {
				ErrorEvent error = Errors.fromException(cause);
				PairingStateMachine.get( context ).onHubPairingFailed(error.getMessage());
				context.sendResponse(request, error);
				PairingUtils.clearPendingPairingStateIf(context, version);
			}
		});
	}
	
	protected void startHubUnpairing(
			SubsystemContext<PairingSubsystemModel> context, 
			PlatformMessage request,
			Function<SubsystemContext<PairingSubsystemModel>, MessageBody> generateSuccessResponse
	) {
		Model hub = assertHubExistAndNotOffline(context);
		if(HubModel.isStatePAIRING(hub)) {
			stopHubPairing(context, hub.getAddress());
		}
		// Only z-wave supports a generic unpairing mode (we sometimes call it orphan remove)
		MessageBody unpairingRequest =
				HubCapability.UnpairingRequestRequest
					.builder()
					.withActionType(HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING)
					.withTimeout(PairingUtils.pairingTimeoutMs(context))
					.withForce(false)
					.build();
		long version = PairingUtils.setFactoryResetPending(context); 
		sendHubRequest(context, unpairingRequest, new ResponseAction<PairingSubsystemModel>() {
			@Override
			public void onResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
				try {
					context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_HUB_UNPAIRING);
					PairingStateMachine.get( context ).onHubUnpairing();
					context.sendResponse(request, generateSuccessResponse.apply(context));
					PairingUtils.clearPendingPairingStateIf(context, version);
				}
				catch(Exception e) {
					onError(context, e);
				}
			}
			
			@Override
			public void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
				ErrorEvent error = Errors.fromException(new PairingSubsystemCapability.HubOfflineException("Timeout contacting hub"));
				PairingStateMachine.get( context ).onHubPairingFailed(error.getMessage());
				context.sendResponse(request, error);
				PairingUtils.clearPendingPairingStateIf(context, version);
			}
			
			@Override
			public void onError(SubsystemContext<PairingSubsystemModel> context, Throwable cause) {
				ErrorEvent error = Errors.fromException(cause);
				PairingStateMachine.get( context ).onHubPairingFailed(error.getMessage());
				context.sendResponse(request, error);
				PairingUtils.clearPendingPairingStateIf(context, version);
			}
		});
	}

	protected void sendHubRequest(
			SubsystemContext<PairingSubsystemModel> context, 
			MessageBody request,
			ResponseAction<PairingSubsystemModel> handler
	) {
		try {
			Address hubAddress = 
					PairingUtils
						.getHubModel(context)
						.orElseThrow(() -> new PairingSubsystemCapability.HubMissingException("This operation requires a hub"))
						.getAddress();
			context.sendAndExpectResponse(hubAddress, request, PairingUtils.requestTimeoutMs(), TimeUnit.MILLISECONDS, handler);
		}
		catch(Exception e) {
			handler.onError(context, e);
		}
	}

	protected String getMode(ProductCatalogEntry product) {
		return Boolean.TRUE.equals(product.getHubRequired()) ? PairingSubsystemCapability.PAIRINGMODE_HUB : PairingSubsystemCapability.PAIRINGMODE_CLOUD;
	}

	protected void stopHubPairingIfNeeded(SubsystemContext<PairingSubsystemModel> context) {
		Optional<Model> hubRef = PairingUtils.getHubModel(context);
		if(!hubRef.isPresent()) {
			return;
		}
		
		Model hub = hubRef.get();
		if(HubModel.isStatePAIRING(hub)) {
			stopHubPairing(context, hub.getAddress());
		}
		else if(HubModel.isStateUNPAIRING(hub)) {
			stopHubUnpairing(context, hub.getAddress());
		}
	}
	
	protected void stopHubPairing(SubsystemContext<PairingSubsystemModel> context, Address hubAddress) {
		MessageBody message = 
				PairingRequestRequest
					.builder()
					.withActionType(PairingRequestRequest.ACTIONTYPE_STOP_PAIRING)
					.build();
		context.request(hubAddress, message);
	}
	
	protected void stopHubUnpairing(SubsystemContext<PairingSubsystemModel> context, Address hubAddress) {
		MessageBody message = 
				UnpairingRequestRequest
					.builder()
					.withActionType(UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING)
					.build();
		context.request(hubAddress, message);
	}

	@Override
	public String name() {
		return stateName.name();
	}
	
	@Override
	public String onEnter(SubsystemContext<PairingSubsystemModel> context) {
		context.model().setPairingModeChanged(new Date());
		return State.super.onEnter(context);
	}

	// allow start pairing from any state
	public String startPairing(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request, ProductCatalogEntry entry) {
		boolean hubRequired = Boolean.TRUE.equals(entry.getHubRequired());
		if(hubRequired && !PairingUtils.isMockPairing(context)) {
			startHubPairing(context, request, Optional.of(entry), (c) -> startPairingResponse(c, entry, c.model().getPairingMode()));
			return name();
		}
		else {
			String mode = determinePairingMode(entry);
			PairingUtils.setSearchProductAddressIfNecessary(context, entry.getAddress());
			context.model().setPairingMode(mode);
			context.sendResponse(request, startPairingResponse(context, entry, mode));
			return PairingStateName.PairingSteps.name();
		}
	}

	// allow search from any state
	public String search(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request, Optional<ProductCatalogEntry> product, Map<String, Object> form) {
		String productAddress = product.map(ProductCatalogEntry::getAddress).orElse("");
		boolean hubRequired = !product.isPresent() || Boolean.TRUE.equals(product.get().getHubRequired());
		boolean mockPairing = PairingUtils.isMockPairing(context);

		if (!product.isPresent() || product.get().getPairingMode().equals(ProductCatalogEntry.PairingMode.BRIDGED_DEVICE)) {
			startPairingBridgeDevices(context, product);
		}

		if(mockPairing) {
			PairingUtils.setSearchProductAddressIfNecessary(context, productAddress);
			//If product is not specified implying advanced search, default to hub
			context.model().setPairingMode(determinePairingMode(product.orElse(null)));
			context.sendResponse(request, searchResponse(context));
			return PairingStateName.Searching.name();
		}
		else if(hubRequired) {
			startHubPairing(context, request, product, (c) -> searchResponse(c));
			return name();
		}
		else {			
			Optional<BridgePairingInfo> infoRef = createBridgePairingInfo(product.get(), form);
			if(infoRef.isPresent() && !mockPairing) {
				PairingUtils.setBridgePairingInfo(context, infoRef.get());
			}
			else {
				PairingUtils.clearBridgePairingInfo(context);
			}
			PairingUtils.setSearchProductAddressIfNecessary(context, productAddress);
			context.model().setPairingMode(determinePairingMode(product.orElse(null)));

         context.sendResponse(request, searchResponse(context));
			return PairingStateName.Searching.name();
		}
	}

	private Iterable<Model> getBridgeModels(SubsystemContext<PairingSubsystemModel> context, Optional<ProductCatalogEntry> childProduct) {
		if (!childProduct.isPresent() || StringUtils.isEmpty(childProduct.get().getDevRequired())) {
			return context.models().getModels(m ->
					m != null
							&& m.getCapabilities() != null
							&& m.getCapabilities().contains(BridgeCapability.NAMESPACE));
		}

		return context.models().getModels(m ->
				m != null
				&& m.getCapabilities() != null
				&& m.getCapabilities().contains(BridgeCapability.NAMESPACE)
				&& DeviceModel.getProductId(m, "").equals(childProduct.get().getDevRequired())
		);
	}

   private void startPairingBridgeDevices(SubsystemContext<PairingSubsystemModel> context, Optional<ProductCatalogEntry> product) {
		getBridgeModels(context, product).forEach(model -> {
				MessageBody body = BridgeCapability.StartPairingRequest.builder().withTimeout(PairingUtils.pairingTimeoutMs(product)).build();
				context.request(model.getAddress(), body);
			});
   }

	private void stopPairingBridgeDevices(SubsystemContext<PairingSubsystemModel> context){
		if (!PairingUtils.isMockPairing(context) && (context.model().isPairingModeCLOUD() || context.model().isPairingModeOAUTH())) {
			getBridgeModels(context, Optional.empty()).forEach(model -> {
				MessageBody body = BridgeCapability.StopPairingRequest.instance();
				context.request(model.getAddress(), body);
			});
		}
	}

	public String factoryReset(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request, ProductCatalogEntry product) {
		PairingProtocol protocol = PairingProtocol.forProduct(product);
		if(protocol == PairingProtocol.ZWAV && !PairingUtils.isMockPairing(context)) {
			startHubUnpairing(context, request, (c) -> factoryResetResponse(c, product));
			return name();
		}
		else {
			String mode = protocol == PairingProtocol.ZWAV ? PairingSubsystemCapability.PAIRINGMODE_HUB_UNPAIRING : PairingSubsystemCapability.PAIRINGMODE_IDLE;
			context.model().setPairingMode(mode);
			context.sendResponse(request, factoryResetResponse(context, product));
			return PairingStateName.FactoryResetSteps.name();
		}
	}

	public String stopPairing(SubsystemContext<PairingSubsystemModel> context) {

		stopPairingBridgeDevices(context);

		return PairingStateName.Idle.name();
	}
	
	public String stopUnpairing(SubsystemContext<PairingSubsystemModel> context) {
		return PairingStateName.Idle.name();
	}
	
	public String onDevicePaired(SubsystemContext<PairingSubsystemModel> context, Model device) {
		context.logger().debug("Discovered new device: [{}]", device);
		return name();
	}
	
	public String onHubPairing(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Hub is now in pairing mode");
		Optional<HubPairingInfo> infoRef = PairingUtils.getHubPairingInfo(context);
		if(!infoRef.isPresent()) {
			context.logger().debug("Ignoring hub pairing mode change because a pairing change is not currently requested");
			return name();
		}
		
		HubPairingInfo info = infoRef.get();
		if(info.getPending() == PairingStateName.FactoryResetSteps) {
			context.logger().debug("Ignoring hub pairing mode change because an unpairing change is currently requested");
			return name();
		}
		
		PairingUtils.clearPendingPairingState(context);
		PairingUtils.setSearchProductAddressIfNecessary(context, info.getProductAddress());
		context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_HUB);
		return info.getPending().name();
	}
	
	public String onHubUnpairing(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Hub is now in unpairing mode");
		Optional<HubPairingInfo> infoRef = PairingUtils.getHubPairingInfo(context);
		if(!infoRef.isPresent()) {
			context.logger().debug("Ignoring hub unpairing mode change because a pairing change is not currently requested");
			return name();
		}
		
		HubPairingInfo info = infoRef.get();
		if(info.getPending() != PairingStateName.FactoryResetSteps) {
			context.logger().debug("Ignoring hub unpairing mode change because a pairing change is currently requested");
			return name();
		}
		
		PairingUtils.clearPendingPairingState(context);
		PairingUtils.setSearchProductAddressIfNecessary(context, info.getProductAddress());
		context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_HUB_UNPAIRING);
		return info.getPending().name();
	}
	
	public String onHubPairingIdle(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Hub is no longer in a pairing mode");
		// if we're in cloud pairing mode, ignore hub changes
		if(!PairingSubsystemModel.isPairingModeCLOUD(context.model())) {
			context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_IDLE);
		}
		return name();
	}

	public String onHubPairingFailed(SubsystemContext<PairingSubsystemModel> context, String error) {
		context.logger().debug("Hub is no longer in pairing mode");
		return name();
	}

	public String onCloudPairingSucceeded(SubsystemContext<PairingSubsystemModel> context) {
		context.logger().debug("Cloud is no longer in pairing mode");
		return name();
	}

	public String onCloudPairingFailed(SubsystemContext<PairingSubsystemModel> context, String errorCode, String error) {
		context.logger().debug("Cloud is no longer in pairing mode, failed - [{}] - [{}]", errorCode, error);
		return name();
	}

	private Optional<BridgePairingInfo> createBridgePairingInfo(ProductCatalogEntry product, Map<String, Object> form) {
		BridgePairingInfo info = null;
		for(Step step: product.getPair()) {
			if(CollectionUtils.isEmpty(step.getInputs())) {
				continue;
			}
			
			if(info == null) {
				info = new BridgePairingInfo();
				info.setMessage(step.getMessage());
				info.setAddress(Address.fromString(step.getTarget()));
				info.setAttributes(new HashMap<>());
			}
			
			for(Input input: step.getInputs()) {
				Object value = form.get(input.getName());
				if(value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
					if(Boolean.TRUE.equals(input.getRequired())) {
						throw new PairingSubsystemCapability.RequestParamInvalidException("Missing value for: " + input.getName());
					}
				}
				else {
					info.getAttributes().put(input.getName(), String.valueOf(value));
				}
			}
		}
		return Optional.ofNullable(info);
	}

	private MessageBody factoryResetResponse(SubsystemContext<PairingSubsystemModel> context, ProductCatalogEntry product) {
		FactoryResetResponse.Builder builder =
			FactoryResetResponse
				.builder()
				.withVideo(null) // FIXME add reset video url to product catalog
				.withSteps(getResetSteps(product))
				;
		return builder.build();
	}
	
	private MessageBody startPairingResponse(SubsystemContext<PairingSubsystemModel> context, ProductCatalogEntry product, String mode) {
		StartPairingResponse.Builder builder =
			StartPairingResponse
				.builder()
				.withForm(getForm(product))
				.withMode(mode)
				.withSteps(getPairingSteps(product))
				.withVideo(getVideo(product));
		PairingProtocol.forProduct(product).addOAuthInfo(builder, context);
		return builder.build();
	}
	
	private MessageBody searchResponse(SubsystemContext<PairingSubsystemModel> context) {
		return 
			SearchResponse
				.builder()
				.withMode(context.model().getPairingMode())
				.build();
	}

	@Nullable
	private String getVideo(ProductCatalogEntry product) {
		return product.getPairVideoUrl();
	}

	@Nullable
	private List<Map<String, Object>> getForm(ProductCatalogEntry product) {
		List<Map<String,Object>> inputs = new ArrayList<>();
		for(Step step: product.getPair()) {
			if(CollectionUtils.isEmpty(step.getInputs())) {
				continue;
			}
			
			for(Input input: step.getInputs()) {
				PairingInput pi = new PairingInput();
				pi.setLabel(input.getLabel());
				pi.setName(input.getName());
				switch(input.getType()) {
				case HIDDEN:
					pi.setType(PairingInput.TYPE_HIDDEN);
					break;
				case TEXT:
					pi.setType(PairingInput.TYPE_TEXT);
					break;
				default:
					throw new IllegalStateException("Invalid input type: " + input.getType());
				}
				pi.setValue(input.getValue());
				if(input.getMinlen() != null) {
					pi.setMinlen(input.getMinlen());
				}
				if(input.getMaxlen() != null) {
					pi.setMaxlen(input.getMaxlen());
				}
				pi.setRequired(Boolean.TRUE.equals(input.getRequired()));
				inputs.add(pi.toMap());
			}
		}
		
		return inputs;
	}

	private List<Map<String, Object>> getPairingSteps(ProductCatalogEntry product) {
		List<Map<String,Object>> steps = new ArrayList<>(product.getPair().size());
		int i = 0;
		for(Step step: product.getPair()) {
			i++;
			
			PairingStep s = new PairingStep();
			s.setId("pair/pair" + i);
			s.setOrder(i);
			s.setInfo(step.getSubText());
			s.setInstructions(ImmutableList.of(step.getText()));
			if(!StringUtils.isEmpty(step.getLinkUrl())) {
				s.setLinkText(!StringUtils.isEmpty(step.getLinkText()) ? step.getLinkText() : step.getLinkUrl());
				s.setLinkUrl(step.getLinkText());
			}
			else if(step.isShowInstallManual()) {
				s.setLinkText(!StringUtils.isEmpty(step.getLinkText()) ? step.getLinkText() : TXT_INSTALL_LINK);
				s.setLinkUrl(product.getInstallManualUrl());
			}
			
			if(step.getExternalApplications() != null && step.getExternalApplications().size() > 0) {
				List<Map<String, Object>> appList = new ArrayList<Map<String, Object>>();
				for(ExternalApplication app : step.getExternalApplications()) {
					PairingApplication a = new PairingApplication();
					if(PlatformType.IOS.equals(app.getPlatform())) {
						a.setPlatform(PairingApplication.PLATFORM_IOS);
					}else{
						a.setPlatform(PairingApplication.PLATFORM_ANDROID);
					}
					a.setAppUrl(app.getAppUrl());
					appList.add(a.toMap());
				}
				s.setExternalApps(appList);
			}
			steps.add(s.toMap());
		}
		return steps;
	}

	private List<Map<String, Object>> getResetSteps(ProductCatalogEntry product) {
		List<Map<String,Object>> steps = new ArrayList<>(product.getPair().size());
		int i = 0;
		List<Step> resetSteps;
		if(CollectionUtils.isEmpty(product.getReset()) && PairingProtocol.forProduct(product) == PairingProtocol.ZWAV) {
			resetSteps = product.getRemoval();
		}
		else {
			resetSteps = product.getReset();
		}
		for(Step step: resetSteps) {
			i++;
			
			PairingStep s = new PairingStep();
			s.setId("pair/reset" + i);
			s.setOrder(i);
			// FIXME work around removal steps being one step
			s.setInfo(step.getSubText());
			s.setInstructions(ImmutableList.of(step.getText()));
			steps.add(s.toMap());
		}
		return steps;
	}
	
	private Model assertHubExistAndNotOffline(SubsystemContext<PairingSubsystemModel> context) {
		Model hub = 
				PairingUtils
					.getHubModel(context)
					.orElseThrow(() -> new PairingSubsystemCapability.HubMissingException("This operation requires a hub"));
		if(HubModel.isStateDOWN(hub)) {
			throw new PairingSubsystemCapability.HubOfflineException("This operation requires hub being online");
		}
		return hub;
	}
	
	private String determinePairingMode(ProductCatalogEntry product) {
		if( product == null || Boolean.TRUE.equals(product.getHubRequired())) {
			return PairingSubsystemCapability.PAIRINGMODE_HUB;
		}else{
			if(PairingProtocol.forProduct(product).isOAuth()) {
				return PairingSubsystemCapability.PAIRINGMODE_OAUTH;
			}else{
				return PairingSubsystemCapability.PAIRINGMODE_CLOUD;
			}
		}
	}
	
	private String getProductPairingMode(Optional<ProductCatalogEntry> entry) {
	   String manufacturer = entry.map(ProductCatalogEntry::getManufacturer).orElse("");
	   return "alertme".equals(manufacturer.trim().toLowerCase()) ? "ZBCLEAR" : "";
	}

}

