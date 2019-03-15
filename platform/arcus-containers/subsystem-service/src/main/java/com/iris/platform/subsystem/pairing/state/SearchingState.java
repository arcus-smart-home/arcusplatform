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

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.PairingSubsystemCapability.PairingFailedEvent;
import com.iris.messages.capability.PairingSubsystemCapability.PairingIdleTimeoutEvent;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.service.BridgeService;
import com.iris.platform.subsystem.pairing.BridgePairingInfo;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing.ProductCacheInfo;
import com.iris.prodcat.ProductCatalogEntry;

// FIXME should really split HubSearching and CloudSearching they are fairly different
public class SearchingState extends PairingState {
	private static final String VAR_IDLE_TIMEOUT = "idleTimeout";
	private static final String VAR_CLOUD_RETRY = "cloudTimeout";
	private static final String IPCD_ERROR_CODE = "IPCD_UNEXPECTED_ERROR";

	SearchingState() {
		super(PairingStateName.Searching);
	}

	@Override
	public String onStarted(SubsystemContext<PairingSubsystemModel> context) {
		sendBridgePairingRequestIfNeeded(context);
		return super.onStarted(context);
	}

	@Override
	public String onEnter(SubsystemContext<PairingSubsystemModel> context) {	
		String curProductAddressStr = context.model().getSearchProductAddress();
		if(StringUtils.isNotBlank(curProductAddressStr)) {			
			ProductCacheInfo cacheInfo = resetSearchTimeout(context);
			resetIdleTimeout(context, cacheInfo!=null?cacheInfo.getPairingIdleTimeoutMs():null);
		}else{
			resetSearchTimeout(context);
			resetIdleTimeout(context, null);
		}
		sendBridgePairingRequestIfNeeded(context);
		return super.onEnter(context);
	}

	@Override
	public void onExit(SubsystemContext<PairingSubsystemModel> context) {
		super.onExit(context);
		context.model().setSearchIdle(false);
		context.model().setSearchIdleTimeout(new Date(0));
		context.setVariable(VAR_IDLE_TIMEOUT, null);
		context.setVariable(VAR_CLOUD_RETRY, null);
	}

	@Override
	public String search(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request, Optional<ProductCatalogEntry> product, Map<String, Object> form) {
		String next = super.search(context, request, product, form);
		if(name().equals(next)) {
			Integer timeout = product.map(ProductCatalogEntry::getPairingIdleTimeoutMs).orElse(null);
			resetIdleTimeout(context, timeout);
			sendBridgePairingRequestIfNeeded(context);
		}
		return next;
	}

	@Override
	public String onTimeout(SubsystemContext<PairingSubsystemModel> context) {
		if(context.getVariable(VAR_IDLE_TIMEOUT).isNull()) {
			context.setVariable(VAR_IDLE_TIMEOUT, true);
			if(!context.model().getSearchDeviceFound()) {
				context.model().setSearchIdle(true);
				context.broadcast(PairingIdleTimeoutEvent.instance());
			}
			if(!context.getVariable(VAR_CLOUD_RETRY).isNull()) {
				sendBridgePairingRequestIfNeeded(context);
			}
			SubsystemUtils.setTimeout(context.model().getSearchTimeout(), context, name());
			return name();
		}
		else if(!context.getVariable(VAR_CLOUD_RETRY).isNull()) {
			sendBridgePairingRequestIfNeeded(context);
			SubsystemUtils.setTimeout(context.model().getSearchTimeout(), context, name());
			return name();
		}
		else {
			// search completely timed out
			return PairingStateName.Idle.name();
		}
	}

	@Override
	public String onCloudPairingSucceeded(SubsystemContext<PairingSubsystemModel> context) {
		return PairingStateName.Idle.name();
	}

	@Override
	public String onCloudPairingFailed(SubsystemContext<PairingSubsystemModel> context, String errorCode, String error) {
		MessageBody failedEvent = PairingFailedEvent.builder()
				.withCode(errorCode)
				.withDescription(error)
				.build();
		context.broadcast(failedEvent);
		return PairingStateName.Idle.name();
	}

	protected void resetIdleTimeout(SubsystemContext<PairingSubsystemModel> context, Integer productPairingIdleTimeoutMs) {
		context.setVariable(VAR_IDLE_TIMEOUT, null);
		context.model().setSearchIdle(false);
		if(StringUtils.isNotBlank(PairingSubsystemModel.getSearchProductAddress(context.model()))) {
			long timeoutValue = PairingUtils.idleTimeoutMs();
			if(productPairingIdleTimeoutMs != null) {
				timeoutValue = productPairingIdleTimeoutMs.longValue();
			}
			Date idleTimeout = setTimeout(context, timeoutValue);		
			context.model().setSearchIdleTimeout(idleTimeout);
		}else{
			//Assuming this is advanced search mode since there is no product address.  Clear out the SearchIdleTimeout
			context.model().setSearchIdleTimeout(PairingUtils.DEFAULT_TIMEOUT);
		}
		
	}

	private void scheduleRetry(SubsystemContext<PairingSubsystemModel> context) {
		if(context.getVariable(VAR_IDLE_TIMEOUT).isNull()) {
			// the retry will be handled by the idle timeout
			context.setVariable(VAR_CLOUD_RETRY, true);
			return;
		}
		
		Date searchTimeout = context.model().getSearchTimeout();
		Date retryPairing = new Date(System.currentTimeMillis() + PairingUtils.requestTimeoutMs());
		if(searchTimeout.before(retryPairing)) {
			SubsystemUtils.setTimeout(searchTimeout, context, name());
		}
		else {
			context.setVariable(VAR_CLOUD_RETRY, true);
			SubsystemUtils.setTimeout(retryPairing, context, name());
		}
	}
	
	private void retry(SubsystemContext<PairingSubsystemModel> context) {
		if(PairingStateMachine.get(context).current() == SearchingState.this) {
			// retry
			sendBridgePairingRequestIfNeeded(context);
		}
	}

	private void sendBridgePairingRequestIfNeeded(SubsystemContext<PairingSubsystemModel> context) {
		// clear any pending retry
		context.setVariable(VAR_CLOUD_RETRY, null);
		Optional<BridgePairingInfo> infoRef = PairingUtils.getBridgePairingInfo(context);
		if(!infoRef.isPresent()) {
			return;
		}
		
		BridgePairingInfo info = infoRef.get();
		MessageBody request;
		switch(info.getMessage()) {
		case BridgeService.RegisterDeviceRequest.NAME:
			request = 
				BridgeService.RegisterDeviceRequest.builder()
					.withAttrs(info.getAttributes())
					.build();
			break;
		default:
			context.logger().error("Unable to form pairing request for message type [{}]", info.getMessage());
			return;
		}
		context.sendAndExpectResponse(
				info.getAddress(), 
				request, 
				PairingUtils.requestTimeoutMs(), 
				TimeUnit.MILLISECONDS, 
				new ResponseAction<PairingSubsystemModel>() {

					@Override
					public void onResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
						if (response != null && response.isError()) {
							handleErrorResponse(context, response);
							return;
						}

						context.logger().debug("RegisterDeviceRequest is successful");
						PairingStateMachine.get(context).onCloudPairingSucceeded();
					}

					@Override
					public void onError(SubsystemContext<PairingSubsystemModel> context, Throwable cause) {
						String error = null;
						String errorCode = null;
						if(cause instanceof ErrorEventException) {
							error = ((ErrorEventException)cause).getDescription();
							errorCode = ((ErrorEventException)cause).getCode();
							if (handleErrorCode(context, ((ErrorEventException) cause).getCode(), cause)) {
								return;
							}
						}
						else {
							errorCode = IPCD_ERROR_CODE;
							error = "Unexpected IPCD pairing error - "+cause.getMessage();
							context.logger().warn("Unexpected IPCD pairing error", cause);
						}
						PairingStateMachine.get(context).onCloudPairingFailed(errorCode, error);
					}

					@Override
					public void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
						retry(context);
					}

					void handleErrorResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
						MessageBody messageBody = response.getValue();
						context.logger().debug("Response is error. Body: [{}]", messageBody);
						String error = null;
						String errorCode = null;
						if (messageBody instanceof ErrorEvent) {
							ErrorEvent errorEvent = (ErrorEvent) response.getValue();
							error = errorEvent.getMessage();
							errorCode = errorEvent.getCode();
							if (handleErrorCode(context, errorEvent.getCode(), errorEvent.getMessage())) {
								return;
							}
						} else {
							errorCode = IPCD_ERROR_CODE;
							error = "Unexpected message body type on IPCD pairing error.";
							context.logger().warn(error);
						}

						PairingStateMachine.get(context).onCloudPairingFailed(errorCode, error);
					}

					boolean handleErrorCode(SubsystemContext<PairingSubsystemModel> context, String errorCode, Object errorMessage) {
						switch (errorCode) {
							case Errors.CODE_NOT_FOUND:								
								context.logger().debug("Device not connected, retry. [{}]", errorMessage);
								scheduleRetry(context);
								return true;
							case Errors.CODE_INVALID_REQUEST:
								context.logger().warn("IPCD device claimed by someone else. [{}]", errorMessage);
								break;
							default:
								context.logger().warn("Unexpected IPCD pairing error. [{}]", errorMessage);
						}
						return false;
					}

				}
		);
	}

}

