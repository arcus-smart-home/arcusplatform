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

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.subsystem.state.StateMachine;
import com.iris.prodcat.ProductCatalogEntry;

public class PairingStateMachine extends StateMachine<PairingSubsystemModel, PairingState> {
	private static final PairingState DefaultState;
	private static final Map<String, PairingState> States;
	
	static {
		DefaultState = new IdleState();
		ImmutableMap.Builder<String, PairingState> builder = ImmutableMap.builder();
		add(builder, DefaultState);
		add(builder, new PairingStepsState());
		add(builder, new SearchingState());
		add(builder, new FactoryResetStepsState());
		States = builder.build();
	}
	
	private static void add(Builder<String, PairingState> builder, PairingState state) {
		builder.put(state.name(), state);
	}

	public static PairingStateMachine get(SubsystemContext<PairingSubsystemModel> context) {
		return new PairingStateMachine(context);
	}
	
	private PairingStateMachine(SubsystemContext<PairingSubsystemModel> context) {
		super(context, "pairingState");
		if(context.getVariable("pairingState").isNull()) {
			context.setVariable("pairingState", PairingStateName.Idle);
		}
	}

	@Override
	protected PairingState state(String name) {
		return States.getOrDefault(name, DefaultState);
	}

	// exposed for testing
	protected PairingState current() {
		return super.current();
	}
	
	// exposed for testing
	@Override
	protected void transition(String next) {
		current(); // make sure its initialized
		super.transition(next);
	}

	public MessageBody startPairing(PlatformMessage request, ProductCatalogEntry entry) {
		transition(current().startPairing(context(), request, entry));
		return MessageBody.noResponse();
	}

	public MessageBody search(PlatformMessage request, Optional<ProductCatalogEntry> product, Map<String, Object> form) {
		transition(current().search(context(), request, product, form));
		return MessageBody.noResponse();
	}
	
	public MessageBody factoryReset(PlatformMessage request, ProductCatalogEntry product) {
		transition(current().factoryReset(context(), request, product));
		return MessageBody.noResponse();
	}
	
	public void stopPairing() {
		transition(current().stopPairing(context()));
	}
	
	/**
	 * Invoked when the hub enters pairing mode *via* request from the PairingSubsystem.
	 * This will not be used for generic pairing / unpairing changes from legacy systems.
	 */
	public void onHubPairing() {
		transition(current().onHubPairing(context()));
	}
	
	/**
	 * Invoked when the hub enters unpairing mode *via* request from the PairingSubsystem.
	 * This will not be used for generic pairing / unpairing changes from legacy systems.
	 */
	public void onHubUnpairing() {
		transition(current().onHubUnpairing(context()));
	}
	
	public void onHubPairingIdle() {
		transition(current().onHubPairingIdle(context()));
	}
	
	/**
	 * Invoked when there is an error putting the hub into pairing or unpairing mode.
	 */
	public void onHubPairingFailed(String error) {
		transition(current().onHubPairingFailed(context(), error));
	}
	
	public void onCloudPairingSucceeded() {
		//Avoid putting pairing back to IDLE, I2-3807
		//transition(current().onCloudPairingSucceeded(context()));
		context().logger().debug("onCloudPairingSucceeded is called, current state [{}]", current().name());
	}

	public void onCloudPairingFailed(String errorCode, String error) {
		transition(current().onCloudPairingFailed(context(), errorCode, error));
	}

}

