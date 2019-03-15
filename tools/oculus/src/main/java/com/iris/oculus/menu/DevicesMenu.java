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
package com.iris.oculus.menu;

import javax.inject.Inject;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.iris.oculus.modules.device.DeviceController;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * 
 */
public class DevicesMenu extends BaseComponentWrapper<JMenu> {
   private String label = "Devices";
   
   private DeviceController controller;
   private PairingDeviceController pairingController;

   @Inject
   public DevicesMenu(DeviceController controller, PairingDeviceController pairingController) {
      this.controller = controller;
      this.pairingController = pairingController;
   }
   
   @Override
   protected JMenu createComponent() {
      JMenu menu = new JMenu(label);
      menu.add(new JMenuItem(controller.actionCreateMockDevice()));
      menu.add(new JMenuItem(controller.actionRegisterIpDevice()));
      menu.add(new JMenuItem(Actions.build("Pairing Wizard", pairingController::showPairingWizard)));
      menu.add(new JMenuItem(Actions.build("Pairing Cart", pairingController::showPairingCart)));
      menu.add(new JMenuItem(controller.actionReload()));
      
      return menu;
   }

}

