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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.modules.device.IpDevice;
import com.iris.oculus.widget.Dialog;
import com.iris.protocol.ipcd.IpcdProtocol;

public class IpDeviceRegistrationPrompt {
   private static final String OTHER_DEVICE_TYPE = "Other";
   private static final String SWANN_CAMERA_TYPE = "SwannWifiBatteryCamera";
   private static final String SWANN_PLUG_TYPE = "SwannWifiPlug";
   private static final String GREATSTAR_INDOOR_PLUG_TYPE = "GreatStarIndoorPlug";
   private static final String GREATSTAR_OUTDOOR_PLUG_TYPE = "GreatStarOutdoorPlug";
   private static final String BASE_INSTRUCTIONS = "Register an IP device.";
   private static final String DEVICE_TYPE_LABEL = "Device Type:";
   private static final String DEVICE_ID_LABEL = "Device Id:";
   private static final String MODEL_LABEL = "Model:";
   private static final String SERIAL_LABEL = "Serial:";

   private static class InstanceRef {
      private static final IpDeviceRegistrationDialog INSTANCE = new IpDeviceRegistrationDialog();
   }
   
   public static ClientFuture<IpDevice> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }
   
   @SuppressWarnings("serial")
   private static class IpDeviceRegistrationDialog extends Dialog<IpDevice> {
      JLabel instructions = new JLabel(BASE_INSTRUCTIONS);
      JLabel typeLabel = new JLabel(DEVICE_TYPE_LABEL);
      JComboBox<String> deviceTypeCombo = new JComboBox<>(getDeviceTypes());
      JLabel idLabel = new JLabel(DEVICE_ID_LABEL);
      JTextField idText = new JTextField();
      JLabel modelLabel = new JLabel(MODEL_LABEL);
      JTextField modelText = new JTextField();
      JLabel serialLabel = new JLabel(SERIAL_LABEL);
      JTextField serialText = new JTextField();
      JButton register = new JButton("Register");
      
      IpDeviceRegistrationDialog() {
         setTitle("Register IP Device");
         deviceTypeCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               if (deviceTypeCombo.getSelectedItem().equals(IpcdProtocol.V1_DEVICE_TYPE_AOSMITH_WATER_HEATER)) {
                  showAOSFields(true);
               }
               else {
                  showAOSFields(false);
               }
            }
            
         });
         deviceTypeCombo.setSelectedItem(OTHER_DEVICE_TYPE);
      }
      
      private void showAOSFields(boolean show) {
         if (modelText.isVisible() != show) {
            modelLabel.setVisible(show);
            modelText.setVisible(show);
            serialLabel.setVisible(show);
            serialText.setVisible(show);
            pack();
         }
      }

      @Override
      protected IpDevice getValue() {
         return new IpDevice(deviceTypeCombo.getSelectedItem().toString(), 
                        idText.getText(),
                        modelText.getText(),
                        serialText.getText());
      }

      @Override
      protected Component createContents() {
         register.addActionListener((e) -> this.submit());
         
         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());
         
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.gridx = 0;
         gbc.gridwidth = 2;
         gbc.ipady = 20;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.weightx = 1;
         panel.add(instructions, gbc.clone());
         
         gbc.gridy++;
         gbc.ipady = 5;
         gbc.gridx = 0;
         gbc.gridwidth = 1;
         panel.add(typeLabel, gbc.clone());
         gbc.gridx = 1;
         panel.add(deviceTypeCombo, gbc.clone());
         
         gbc.gridy++;
         gbc.gridx = 0;
         panel.add(idLabel, gbc.clone());
         gbc.gridx = 1;
         panel.add(idText, gbc.clone());
         
         gbc.gridy++;
         gbc.gridx = 0;
         panel.add(modelLabel, gbc.clone());
         gbc.gridx = 1;
         panel.add(modelText, gbc.clone());
         
         gbc.gridy++;
         gbc.gridx = 0;
         panel.add(serialLabel, gbc.clone());
         gbc.gridx = 1;
         panel.add(serialText, gbc.clone());
         
         gbc.gridy++;
         gbc.gridx = 1;
         gbc.anchor = GridBagConstraints.SOUTHEAST;
         gbc.weighty = 1.0;
         gbc.fill = GridBagConstraints.NONE;
         
         panel.add(register, gbc.clone());
         
         return panel;
      }
      
      private static String[] getDeviceTypes() {
    	  Set<String> v1DeviceTypes = IpcdProtocol.V1_DEVICE_TYPES;
    	  String[] types = new String[v1DeviceTypes.size() + 5];
    	  types = v1DeviceTypes.toArray(types);
    	  types[v1DeviceTypes.size()] = OTHER_DEVICE_TYPE;
    	  types[v1DeviceTypes.size() + 1] = SWANN_CAMERA_TYPE;
    	  types[v1DeviceTypes.size() + 2] = SWANN_PLUG_TYPE;
    	  types[v1DeviceTypes.size() + 3] = GREATSTAR_INDOOR_PLUG_TYPE;
    	  types[v1DeviceTypes.size() + 4] = GREATSTAR_OUTDOOR_PLUG_TYPE;
    	  return types;
      }
   }
}

