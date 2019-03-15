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

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.util.Addresses;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.PairingSubsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.Model;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.widget.wizard.Wizard;

public class PairingWizard {

	public static ClientFuture<Void> start(String placeId) {
		Model m = IrisClientFactory.getModelCache().get(Addresses.toObjectAddress(PairingSubsystem.NAMESPACE, placeId));
		return 
			Wizard
				.builder(new SelectProductPage())
				.addStep(new PairingInstructionsPage())
				.addStep(new SearchingPage())
				.addStep(new CustomizePage())
				.build()
				.prompt(new PairingInfo(m))
				.onSuccess((i) -> onSuccess(i, placeId))
				.onFailure((e) -> dismissAll())
				.transform((i) -> null);
	}
	
	public static ClientFuture<Void> showPairingQueue(String placeId) {
		Model m = IrisClientFactory.getModelCache().get(Addresses.toObjectAddress(PairingSubsystem.NAMESPACE, placeId));
		return 
			Wizard
				.builder(new SearchingPage())
				.addStep(new CustomizePage())
				.build()
				.prompt(new PairingInfo(m))
				.onSuccess((i) -> onSuccess(i, placeId))
				.onFailure((e) -> dismissAll())
				.transform((i) -> null);
	}
	
	private static void onSuccess(PairingInfo info, String placeId) {
		PostCustomizationDialog
			.prompt(info)
			.onSuccess((action) -> {
				switch(action) {
				case PAIR_ANOTHER:
					start(placeId);
					break;
				case CUSTOMIZE_ANOTHER:
					showPairingQueue(placeId);
					break;
				default:
					dismissAll();
				}
			})
			.onFailure((e) -> dismissAll());
	}
	
	private static void dismissAll() {
		ServiceLocator.getInstance(PairingDeviceController.class).dismissAll(true);
	}
}

