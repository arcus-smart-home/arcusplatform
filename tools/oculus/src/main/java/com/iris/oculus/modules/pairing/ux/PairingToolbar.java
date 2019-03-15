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
package com.iris.oculus.modules.pairing.ux;

import java.util.function.Supplier;

import javax.swing.Action;
import javax.swing.JPanel;

import com.iris.client.capability.PairingDevice;
import com.iris.client.capability.PairingDeviceMock;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.Listeners;
import com.iris.client.model.PairingDeviceModel;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.oculus.modules.BaseToolbar;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.modules.pairing.wizard.PairingWizard;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Toolbar;

/**
 * @author tweidlin
 *
 */
public class PairingToolbar extends BaseToolbar<PairingDeviceModel> {
   private Action pairingWizard;
   private Action pairingCart;
   private Action createMock;
   private Action mockNextState;
   private Action mockSetState;
   private Action mockMispaired;
   private Action mockPaired;
   private Action dismissDevice;
   private Action removeDevice;
   private Action forceRemoveDevice;

   private ListenerRegistration listener = Listeners.unregistered();

   public PairingToolbar(PairingDeviceController controller, SessionController session) {
      this.pairingWizard = Actions.build("Pairing Wizard", controller::showPairingWizard);
      this.pairingCart = Actions.build("Pairing Cart", controller::showPairingCart);
      this.createMock = Actions.build("+ Mock", controller::createMock);
      this.mockNextState = Actions.build("Mock Phase >", (Supplier<PairingDeviceModel>) this::model, controller::nextPairingPhase);
      this.mockSetState = Actions.build("Mock Set Phase...", (Supplier<PairingDeviceModel>) this::model, controller::choosePairingPhase);
      this.mockMispaired = Actions.build("Mock Mispaired", (Supplier<PairingDeviceModel>) this::model, (PairingDeviceModel model) -> controller.setPairingPhase(model, PairingDevice.PAIRINGPHASE_FAILED));
      this.mockPaired = Actions.build("Mock Paired", (Supplier<PairingDeviceModel>) this::model, (PairingDeviceModel model) -> controller.setPairingPhase(model, PairingDevice.PAIRINGPHASE_PAIRED));
      this.dismissDevice = Actions.build("Dismiss", (Supplier<PairingDeviceModel>) this::model, controller::dismiss);
      this.removeDevice = Actions.build("Remove", (Supplier<PairingDeviceModel>) this::model, controller::remove);
      this.forceRemoveDevice = Actions.build("Force Remove", (Supplier<PairingDeviceModel>) this::model, controller::forceRemove);
      
      clear();
   }

   @Override
   protected JPanel createComponent() {
      return
         Toolbar
            .builder()
            .left().addButton(pairingWizard)
            .left().addButton(pairingCart)
            .left().addButton(createMock)
            .center().addButton(mockSetState)
            .center().addButton(mockNextState)
            .center().addButton(mockMispaired)
            .center().addButton(mockPaired)
            .right().addButton(dismissDevice)
            .right().addButton(removeDevice)
            .right().addButton(forceRemoveDevice)
            .build();
   }


   @Override
   protected void setModel(PairingDeviceModel model) {
      super.setModel(model);
      syncDeviceActions(model);
      dismissDevice.setEnabled(PairingDeviceModel.PAIRINGSTATE_PAIRED.equals(model.getPairingState()));
      removeDevice.setEnabled(true);
      forceRemoveDevice.setEnabled(true);
      Listeners.unregister(listener);
      listener = model.addListener((p) -> syncDeviceActions(model));
   }

   private void syncDeviceActions(PairingDeviceModel model) {
      boolean isMockPairing = model.getCaps().contains(PairingDeviceMock.NAMESPACE) && PairingDeviceCapability.PAIRINGSTATE_PAIRING.equals(model.getPairingState());
      mockNextState.setEnabled(isMockPairing);
      mockSetState.setEnabled(isMockPairing);
      mockMispaired.setEnabled(isMockPairing);
      mockPaired.setEnabled(isMockPairing);
      dismissDevice.setEnabled(PairingDeviceModel.PAIRINGSTATE_PAIRED.equals(model.getPairingState()));
   }

   @Override
   protected void clearModel() {
      super.clearModel();
      clear();
   }

   // split into its own method so it can be referenced from the constructor
   private void clear() {
      mockNextState.setEnabled(false);
      mockSetState.setEnabled(false);
      mockMispaired.setEnabled(false);
      mockPaired.setEnabled(false);
      dismissDevice.setEnabled(false);
      removeDevice.setEnabled(false);
      forceRemoveDevice.setEnabled(false);
      listener = Listeners.unregister(listener);
   }
   
}

