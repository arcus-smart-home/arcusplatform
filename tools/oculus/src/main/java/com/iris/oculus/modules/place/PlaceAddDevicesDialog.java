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
package com.iris.oculus.modules.place;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.DeviceModel;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.Fields;

/**
 * Shows a pairing dialog and returns the list of device
 * addresses that were paired.
 */
public class PlaceAddDevicesDialog extends Dialog<Integer> implements PlaceAddDevicesController.Callback {
   private PlaceAddDevicesController controller;
   private DefaultListModel<DeviceAdvanced> model = new DefaultListModel<>();

   public PlaceAddDevicesDialog(PlaceAddDevicesController controller) {
      this.init();
      this.controller = controller;
      this.controller.addCallback(this);
   }

   private void init() {
      setTitle("Searching for Devices...");
      setModal(false);
      setDefaultCloseOperation(HIDE_ON_CLOSE);
   }

   @Override
   protected Integer getValue() {
      return model.size();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.widget.Dialog#prompt()
    */
   @Override
   public ClientFuture<Integer> prompt() {
      this.model.removeAllElements();
      return super.prompt();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.widget.Dialog#onHide()
    */
   @Override
   protected void onHide() {
      controller.stopAddingDevices();;
      super.onHide();
   }

   @Override
   protected Component createContents() {
      JLabel label = new JLabel("Searching for devices to be added to the place, when pairing is successful you should hear the hub make a triple-beep");
      JButton done = new JButton("Done");
      done.addActionListener((event) -> submit());
      JList<DeviceAdvanced> list =
            Fields
               .<DeviceAdvanced>listBuilder()
               .withModel(model)
               .withRenderer((device) -> render(device), "")
               .labelled("devices")
               .build()
               .getComponent();
      list.setMinimumSize(new Dimension(0, 250));
      list.setPreferredSize(new Dimension(0, 250));

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(label, BorderLayout.NORTH);
      panel.add(list, BorderLayout.CENTER);
      panel.add(done, BorderLayout.SOUTH);
      return panel;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.modules.hub.HubPairingController.Callback#showPairing()
    */
   @Override
   public void showPairing() {
      prompt();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.modules.hub.HubPairingController.Callback#hidePairing()
    */
   @Override
   public void hidePairing() {
      submit();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.modules.hub.HubPairingController.Callback#deviceAdded(com.iris.client.model.DeviceModel)
    */
   @Override
   public void deviceAdded(DeviceModel model) {
      this.model.add(0, (DeviceAdvanced) model);
   }

   protected String render(DeviceAdvanced device) {
      String commit = device.getDrivercommit();
      if (commit != null) {
         commit = "-" + commit.substring(0,7);
      }

      return "Discovered " + device.getName() + " @ " + device.getAdded() + "\tDriver: " + device.getDrivername() + " v" + device.getDriverversion() + commit;
   }
}

