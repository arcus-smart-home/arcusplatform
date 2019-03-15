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
/**
 * 
 */
package com.iris.oculus.modules.pairing;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.PairingCustomizationStep;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Contact;
import com.iris.client.capability.Device;
import com.iris.client.capability.PairingDevice.CustomizeResponse;
import com.iris.client.capability.PairingDeviceMock;
import com.iris.client.capability.PairingDeviceMock.UpdatePairingPhaseRequest;
import com.iris.client.capability.PairingSubsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.Model;
import com.iris.client.model.PairingDeviceModel;
import com.iris.client.service.PairingDeviceService;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.capability.CapabilityController;
import com.iris.oculus.modules.pairing.ux.ShowHelpDialog;
import com.iris.oculus.modules.pairing.wizard.PairingWizard;
import com.iris.oculus.util.Models;

/**
 * 
 */
@Singleton
public class PairingDeviceController extends BaseController<PairingDeviceModel> {

   @Inject
   public PairingDeviceController(DefinitionRegistry registry) {
      super(PairingDeviceModel.class);
   }

   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      ClientRequest request = new ClientRequest();
      request.setAddress(getPairingSubsystemAddress());
      request.setCommand(PairingSubsystem.ListPairingDevicesRequest.NAME);
      request.setRestfulRequest(false);
      return
         IrisClientFactory
            .getClient()
            .request(request)
            .transform((response) -> new PairingSubsystem.ListPairingDevicesResponse(response).getDevices());
   }

   public ClientFuture<Void> showHelpSteps() {
      PairingSubsystem.ListHelpStepsRequest request = new PairingSubsystem.ListHelpStepsRequest();
      request.setAddress(getPairingSubsystemAddress());
      ClientFuture<ClientEvent> response = IrisClientFactory.getClient().request(request);
      Oculus.showProgress(response, "Loading Help Steps");
      response.onSuccess(this::onHelpStepsLoaded);
      return response.transform((o) -> null);
   }

   public ClientFuture<Void> dismissAll(boolean warn) {
//      Model m = getPairingSubsystem();
//      if(warn && !((Collection<?>) m.get(PairingSubsystem.ATTR_PAIRINGDEVICES)).isEmpty()) {
//         // FIXME warn the user before they dismiss
//      }
      PairingSubsystem.DismissAllRequest request = new PairingSubsystem.DismissAllRequest();
      request.setAddress(getPairingSubsystemAddress());
      ClientFuture<ClientEvent> response = IrisClientFactory.getClient().request(request);
      Oculus.showProgress(response, "Dismissing Pairing Device");
      return response.transform((o) -> null);
   }
   
   public ClientFuture<CustomizeResponse> customize(PairingDeviceModel device) {
      ClientFuture<CustomizeResponse> response = device.customize();
      Oculus.showProgress(response, "Loading Customization Options");
      return response;
   }

   private void onHelpStepsLoaded(ClientEvent response) {
      ShowHelpDialog.prompt(new PairingSubsystem.ListHelpStepsResponse(response));
   }
   
   private String getPairingSubsystemAddress() {
      return Addresses.toObjectAddress(PairingSubsystem.NAMESPACE, getPlaceId());
   }
   
   private Model getPairingSubsystem() {
      return Models.getIfLoaded(getPairingSubsystemAddress()).orElseThrow(() -> new IllegalStateException("Pairing subsystem isn't loaded"));
   }

   public ClientFuture<Void> customizeFavorite(PairingDeviceModel pairingDevice, boolean selected) {
      ClientRequest request = new ClientRequest();
      request.setAddress(pairingDevice.getDeviceAddress());
      request.setCommand(selected ? Capability.AddTagsRequest.NAME : Capability.RemoveTagsRequest.NAME);
      request.setAttribute(Capability.AddTagsRequest.ATTR_TAGS, ImmutableSet.of("FAVORITE"));
      return 
         IrisClientFactory.getClient()
            .request(request)
            .chain((r) -> addCustomization(pairingDevice, PairingCustomizationStep.ACTION_FAVORITE));
   }
   
   public ClientFuture<Void> customizeName(PairingDeviceModel pairingDevice, String name) {
      ClientRequest request = new ClientRequest();
      request.setAddress(pairingDevice.getDeviceAddress());
      request.setCommand(Capability.CMD_SET_ATTRIBUTES);
      request.setAttribute(Device.ATTR_NAME, name);
      return 
         IrisClientFactory.getClient()
            .request(request)
            .chain((r) -> addCustomization(pairingDevice, PairingCustomizationStep.ACTION_NAME));
   }
   
   public ClientFuture<Void> customizeContactType(PairingDeviceModel pairingDevice, String value) {
      ClientRequest request = new ClientRequest();
      request.setAddress(pairingDevice.getDeviceAddress());
      request.setCommand(Capability.CMD_SET_ATTRIBUTES);
      request.setAttribute(Contact.ATTR_USEHINT, value);
      return 
         IrisClientFactory.getClient()
            .request(request)
            .chain((r) -> addCustomization(pairingDevice, PairingCustomizationStep.ACTION_CONTACT_TYPE));
   }
   
   public ClientFuture<Void> addCustomization(PairingDeviceModel pairingDevice, String customization) {
      return
         pairingDevice
            .addCustomization(customization)
            .transform((r) -> null);
   }
   
   public void createMock() {
      ServiceLocator
         .getInstance(CapabilityController.class)
         .getServiceAction(PairingDeviceService.CMD_CREATEMOCK)
         .actionPerformed(new ActionEvent(this, 0, PairingDeviceService.CMD_CREATEMOCK));
   }
   
   public void showPairingWizard() {
      PairingWizard.start(getPlaceId());
   }
   
   public void showPairingCart() {
      PairingWizard.showPairingQueue(getPlaceId());
   }
   
   public void choosePairingPhase(PairingDeviceModel model) {
      ServiceLocator
         .getInstance(CapabilityController.class)
         .getModelAction(UpdatePairingPhaseRequest.NAME, model)
         .actionPerformed(new ActionEvent(this, 0, UpdatePairingPhaseRequest.NAME));
   }
   
   public void nextPairingPhase(PairingDeviceModel model) {
      setPairingPhase(model, null);
   }
   
   public void setPairingPhase(PairingDeviceModel model, @Nullable String phase) {
      UpdatePairingPhaseRequest request = new UpdatePairingPhaseRequest();
      request.setPhase(phase);
      Oculus.showProgress(model.request(request), "Updating pairing phase...");
   }
   
   public void dismiss(PairingDeviceModel model) {
      Oculus.showProgress(model.dismiss(), "Dismissed!");
   }
   
   public void remove(PairingDeviceModel model) {
      Oculus.showProgress(model.remove(), "Remove-inating...");
   }

   public void forceRemove(PairingDeviceModel model) {
      Oculus.showProgress(model.forceRemove(), "Wow.  You're not kidding around.  If you see this message, something is probably wrong... Think on it.");
   }

}

