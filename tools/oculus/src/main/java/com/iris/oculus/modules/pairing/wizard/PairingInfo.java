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
package com.iris.oculus.modules.pairing.wizard;


import com.google.common.base.Optional;
import com.iris.client.capability.PairingDevice.CustomizeResponse;
import com.iris.client.capability.PairingSubsystem.StartPairingResponse;
import com.iris.client.model.Model;
import com.iris.client.model.PairingDeviceModel;
import com.iris.client.model.ProductModel;

public class PairingInfo {
	private final Model pairingSubsystem;
	private Optional<ProductModel> product;
	private StartPairingResponse pairingInstructions;
	private PairingDeviceModel pairingDevice;
	private CustomizeResponse customizations;
	
	public PairingInfo(Model pairingSubsystem) {
		this.pairingSubsystem = pairingSubsystem;
	}
	
	public Model getPairingSubsystem() {
		return pairingSubsystem;
	}

	public Optional<ProductModel> getProduct() {
		return product;
	}

	public void setProduct(Optional<ProductModel> product) {
		this.product = product;
	}

	public StartPairingResponse getPairingInstructions() {
		return pairingInstructions;
	}

	public void setPairingInstructions(StartPairingResponse pairingInstructions) {
		this.pairingInstructions = pairingInstructions;
	}

	public PairingDeviceModel getPairingDevice() {
		return pairingDevice;
	}

	public void setPairingDevice(PairingDeviceModel pairingDevice) {
		this.pairingDevice = pairingDevice;
	}

	public CustomizeResponse getCustomizations() {
		return customizations;
	}

	public void setCustomizations(CustomizeResponse customizations) {
		this.customizations = customizations;
	}

}

