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

import java.awt.Component;

import com.iris.bootstrap.ServiceLocator;
import com.iris.client.capability.PairingSubsystem;
import com.iris.client.capability.PairingSubsystem.StartPairingRequest;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.oculus.modules.product.ProductController;
import com.iris.oculus.modules.product.ProductListView;
import com.iris.oculus.widget.wizard.Wizard;
import com.iris.oculus.widget.wizard.Wizard.Transition;

public class SelectProductPage implements Transition<PairingInfo, PairingInfo> {
	private ProductListView view;
	private ProductController controller;
	private PairingInfo input;
	
	@Override
	public void update(Wizard<?, ?> dialog, PairingInfo input) {
		if(view == null) {
			controller = ServiceLocator.getInstance(ProductController.class);
			controller.reloadProducts();
			view = new ProductListView(controller);
		}
		this.input = input;
	}

	@Override
	public Component show(Wizard<?, ?> dialog) {
		dialog.setNextEnabled(true);
		return view.getComponent();
	}

	@Override
	public PairingInfo getValue() {
		this.input.setProduct(view.getSelectedItem());
		return this.input;
	}

	@Override
	public ClientFuture<PairingInfo> commit() {
		PairingInfo info = getValue();
		if(!info.getProduct().isPresent()) {
			return Futures.failedFuture(new UnsupportedOperationException("Generic pairing not supported yet"));
		}
		StartPairingRequest request = new PairingSubsystem.StartPairingRequest();
		request.setAddress(info.getPairingSubsystem().getAddress());
		request.setProductAddress(info.getProduct().get().getAddress());
		request.setMock(true);
		return 
			input.getPairingSubsystem()
				.request(request)
				.transform((event) -> {
					PairingSubsystem.StartPairingResponse response = new PairingSubsystem.StartPairingResponse(event);
					info.setPairingInstructions(response);
					return info;
				});
	}

}

