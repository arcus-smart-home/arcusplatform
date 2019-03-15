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
package com.iris.oculus.modules.device;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Comparator;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.iris.client.capability.Device;
import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.capability.DeviceOta;
import com.iris.client.model.DeviceModel;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.modules.device.ux.DeviceToolbar;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;


/**
 * @author tweidlin
 *
 */
@Singleton
public class DeviceSection extends BaseComponentWrapper<Component> implements OculusSection {
   private DeviceController controller;

   @Inject
   public DeviceSection(DeviceController controller) {
      this.controller = controller;
   }

   protected Component createToolbar() {
      DeviceToolbar toolbar = new DeviceToolbar();
      controller.addSelectedListener(toolbar);
      return toolbar.getComponent();
   }

   protected Component createInfoBar() {
      JTextArea version = new JTextArea("No device selected");
      version.setBackground(UIManager.getColor("OptionPane.background"));
      version.setEditable(false);
      controller.addSelectedListener((model) -> {
         if(model == null) {
            version.setText("No device selected");
            return;
         }

         DeviceAdvanced device = (DeviceAdvanced) model;
         String commit = device.getDrivercommit();
         if(commit == null) {
            commit = "SNAPSHOT";
         }
         else {
            commit = commit.substring(0, 6);
         }
         String text = String.format(
               "%s v%s-%s (%s)",
               device.getDrivername(),
               device.getDriverversion(),
               commit,
               device.getDriverhash()
         );
         version.setText(text);
         version.invalidate();
      });
      return version;
   }

   protected TableModel<DeviceModel> createTableModel() {
      return
            TableModelBuilder
               .builder(controller.getStore())
               .columnBuilder()
                  .withName("Name")
                  .withGetter(DeviceModel::getName)
                  .add()
               .columnBuilder()
                  .withName("State")
                  .withGetter(DeviceController::getDeviceState)
                  .add()
               .columnBuilder()
                  .withName("Protocol")
                  .withGetter(DeviceController::getProtocolId)
                  .add()
               .columnBuilder()
                  .withName("Driver")
                  .withGetter(DeviceController::getDriverInfo)
                  .add()
               .columnBuilder()
                  .withName("Firmware")
                  .withGetter((d) -> d instanceof DeviceOta ? ((DeviceOta) d).getCurrentVersion() : ((d instanceof DeviceAdvanced) ? ((DeviceAdvanced) d).getFirmwareVersion() : null))
                  .withRenderer((Component destination, Object value) -> {
                     JLabel label = (JLabel) destination;
                     if(value == null) {
                        label.setText("Unknown");
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                        label.setForeground(UIManager.getColor("Label.disabledForeground"));
                     }
                     else {
                        label.setText(String.valueOf(value));
                        label.setForeground(UIManager.getColor("Label.foreground"));
                     }
                  })
                  .add()
               .columnBuilder()
                  .withName("Added")
                  .withGetter((d) -> d instanceof DeviceAdvanced ? ((DeviceAdvanced) d).getAdded() : "<i>Unknown</i>")
                  .add()
               .build();
   }

   protected JPanel createSummaryToolbar() {
      JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
      toolbar.add(new JButton(controller.actionCreateMockDevice()));
      return toolbar;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Devices";
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.util.BaseComponentWrapper#createComponent()
    */
   @Override
   protected Component createComponent() {
      return
            ModelStoreViewBuilder
               .builder(controller.getStore())
               .sortedBy(Comparator.comparing(DeviceModel::getName))
               .withListTableModel(createTableModel(), controller.getSelection(), createSummaryToolbar())
               .withTypeName("Device")
               .withModelSelector(
                     Device.ATTR_NAME,
                     controller.getSelection(),
                     controller.actionReload(),
                     createInfoBar()
               )
               .withToolbarComponent(createToolbar())
               .build();
   }

}

