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
package com.iris.oculus.modules.device.dialog;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.JDialog;

import com.iris.client.model.DeviceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.capability.ux.CapabilityResponsePopUp;
import com.iris.oculus.modules.device.settings.DeviceSettingsView;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.Models;
import com.iris.oculus.widget.Toolbar;

/**
 * Non-modal, so it can stay open and follow the device selected in the main window.
 */
@SuppressWarnings("serial")
public class DeviceSettingsDialog extends JDialog {
   private final DeviceSettingsView view = new DeviceSettingsView();
   private final Action close = Actions.build("Close", this::close);

   public DeviceSettingsDialog() {
      super(Oculus.getMainWindow(), "Device Settings");
      setModal(false);
      add(view.getComponent(), BorderLayout.CENTER);
      add(
            Toolbar
               .builder()
               .right().addButton(view.actionRefresh())
               .right().addButton(close)
               .build(),
            BorderLayout.SOUTH
      );
      setSize(1250, 640);
      setLocationRelativeTo(Oculus.getMainWindow());
      CapabilityResponsePopUp.installEscapeCloseOperation(this);
      addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            close();
         }
      });
   }

   public void show(DeviceModel model) {
      setTitle("Device Settings: " + Objects.toString(Models.nameOf(model), model.getAddress()));
      view.setModel(model);
      setVisible(true);
   }

   public void close() {
      view.clearModel();
      setVisible(false);
   }
}
