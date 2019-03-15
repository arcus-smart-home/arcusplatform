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
package com.iris.oculus.modules.capability;

import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.key.NamespacedKey;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.Model;
import com.iris.io.json.JSON;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.capability.ux.CapabilityInputPrompt;
import com.iris.oculus.modules.capability.ux.CapabilityResponsePopUp;
import com.iris.oculus.util.Actions;

@Singleton
public class CapabilityController {
	private IrisClient client;
	private DefinitionRegistry registry;
	
	@Inject
	public CapabilityController(
			IrisClient client,
			DefinitionRegistry registry
	) {
		this.client = client;
		this.registry = registry;
	}
	
	public ClientFuture<ClientEvent> execute(ClientRequest request) {
   	CapabilityResponsePopUp popUp = new CapabilityResponsePopUp();
   	popUp.setCommand(request.getCommand());
   	popUp.setAttributes(request.getAttributes());
   	return
	   	client
	   		.request(request)
	   		.onSuccess(popUp::onResponse)
	   		.onFailure(popUp::onError);
	}
	
	public Action getModelAction(String name, Model model) {
		NamespacedKey key = NamespacedKey.parse(name);
		if(key.isInstanced()) {
			return getModelAction(key.getNamespace(), key.getName(), model, key.getInstance());
		}
		else {
			return getModelAction(key.getNamespace(), key.getName(), model);
		}
	}
	
	public Action getModelAction(String capability, String method, Model model) {
		CapabilityDefinition cd = registry.getCapability(capability);
		Preconditions.checkNotNull(cd, "No capability named [%s] found", capability);
		String command = cd.getNamespace() + ":" + method;
		MethodDefinition md = cd.getMethods().stream().filter((m) -> command.equals(m.getName())).findFirst().orElse(null);
		Preconditions.checkNotNull(md, "No method named [%s] on capability [%s] found", method, capability);

		return toAction(model.getAddress(), command, md);
	}
	
	public Action getModelAction(String capability, String method, Model model, String instance) {
		Preconditions.checkArgument(model.getInstances().containsKey(instance), "Object does not support instance [%s]", instance);
		CapabilityDefinition cd = registry.getCapability(capability);
		Preconditions.checkNotNull(cd, "No capability named [%s] found", capability);
		String command = cd.getNamespace() + ":" + method;
		MethodDefinition md = cd.getMethods().stream().filter((m) -> command.equals(m.getName())).findFirst().orElse(null);
		Preconditions.checkNotNull(md, "No method named [%s] on capability [%s] found", method, capability);

		return toAction(model.getAddress() + ":" + instance, command, md);
	}
	
	public Action getServiceAction(String name) {
		NamespacedKey key = NamespacedKey.parse(name);
		return getServiceAction(key.getNamespace(), key.getName());
	}
	
	public Action getServiceAction(String service, String method) {
		ServiceDefinition sd = registry.getService(service);
		Preconditions.checkNotNull(sd, "No service named [%s] found", service);
		MethodDefinition md = sd.getMethods().stream().filter((m) -> method.equals(m.getName())).findFirst().orElse(null);
		Preconditions.checkNotNull(md, "No method named [%s] on service [%s] found", method, service);

		String address = Addresses.toServiceAddress(sd.getNamespace());
		String command = sd.getNamespace() + ":" + md.getName();
		return toAction(address, command, md);
	}
	
	private Action toAction(String address, String command, MethodDefinition md) {
		String label;
		Runnable action;
		if(md.getParameters().isEmpty()) {
			label = md.getName();
			action = () -> execute(buildRequest(address, command, md.isRestful(), Collections.emptyMap()));
		}
		else {
			label = md.getName() + "...";
			action = 
					() ->
						CapabilityInputPrompt
							.prompt(md)
							.onSuccess((attributes) -> execute(buildRequest(address, command, md.isRestful(), attributes)));
		}
		return Actions.build(label, action);
	}

	private ClientRequest buildRequest(String address, String command, boolean restful, Map<String, Object> attributes) {
		ClientRequest request = new ClientRequest();
		request.setAddress(address);
		request.setCommand(command);
		request.setRestfulRequest(restful);
		request.setAttributes(attributes);
		return request;
	}

}

