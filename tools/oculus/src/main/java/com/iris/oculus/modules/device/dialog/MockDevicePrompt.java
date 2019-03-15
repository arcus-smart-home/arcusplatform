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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.iris.Utils;
import com.iris.client.event.ClientFuture;
import com.iris.oculus.widget.Dialog;

public class MockDevicePrompt {

   private static class InstanceRef {
      private static final CreateMockDeviceDialog INSTANCE = new CreateMockDeviceDialog();
   }

   public static ClientFuture<String> prompt() {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      return InstanceRef.INSTANCE.prompt();
   }

   @SuppressWarnings("serial")
   private static class CreateMockDeviceDialog extends Dialog<String> {
      JLabel instructions = new JLabel("Select the capability of the mock device.");
      JComboBox<String> capabilityCombo = new JComboBox<>(MOCK_TYPES);
      JButton create = new JButton("Create");

      CreateMockDeviceDialog() {
         setTitle("Create Mock Device");

      }

      @Override
      protected String getValue() {
         return (String)capabilityCombo.getSelectedItem();
      }

      @Override
      protected Component createContents() {
         create.addActionListener((e) -> this.submit());

         JPanel panel = new JPanel();
         panel.setLayout(new GridBagLayout());

         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridy = 0;
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.weightx = 1;

         panel.add(instructions, gbc.clone());
         gbc.gridy++;

         panel.add(capabilityCombo, gbc.clone());
         gbc.gridy++;
         gbc.anchor = GridBagConstraints.NORTHEAST;
         gbc.weighty = 1.0;
         gbc.fill = GridBagConstraints.NONE;

         panel.add(create, gbc.clone());

         return panel;
      }

      @Override
      protected void onShow() {
         capabilityCombo.requestFocusInWindow();
      }

      private static final String[] MOCK_TYPES = {
        "Blind",
        "Button",
        "Camera",
        "CODetector",
        "ContactSensor",
        "DimmerSwitch",
        "ElectricalMeter",
        "FanControl",
        "GarageDoor",
        "GenieAladdinBridge",
        "GenieAladdinGarageDoor",
        "GenieGarageDoor",
        "GlassBreakSensor",
        "Halo",
        "HaloPlus",
        "HueFallback",
        "IrrigationController1Zone",
        "IrrigationController12Zone",
        "KeyFob2Button",
        "KeyFob4Button",
        "CentraLiteKeyFobV2",
        "KeyPad",
        "LightBulb",
        "LightBulbWithDimmer",
        "LightBulbWithDimmerAndColorTemp",
        "LutronCasetaSmartBridge",
        "LockNoBuzzin",
        "LockWithBuzzin",
        "MotionSensor",
        "OTADevice",
        "Pendant",
        "PetDoor",
        "Shade",
        "Siren",
        "SmokeCOSensor",
        "SmokeSensor",
        "SoilMoistureSensor",
        "SomfyV1Bridge",
        "SomfyV1Shade",
        "Switch",
        "Thermostat",
        "ThermostatNoHumidity",
        "HoneywellTCCThermostat",
        "TiltSensor",
        "TiltSensorWithContact",
        "TwinstarSpaceHeater",
        "Vent",
        "WaterHeater",
        "WaterLeakDetector",
        "WaterSoftener",
        "WaterValve",
        "ZWaveFallback",
        "Fallback / Unsupported Device"
      };

      /**
      private static final String[] MOCK_TYPES = {
          "Alert",
          "Button",
          "CarbonMonoxide",
          "Clock",
          "Color",
          "ContactSensor",
          "DimmerSwitch",
          "FanControl",
          "GarageDoor",
          "GlassBreakSensor",
          "KeyFob1Button",
          "LightBulb",
          "LightBulbWithDimmer",
          "LockNoBuzzin",
          "LockWithBuzzin",
          "MotionSensor",
          "Pendant",
          "Siren",
          "SmokeCOSensor",
          "SmokeSensor",
          "Switch",
          "Thermostat",
          "ThermostatNoHumidity",
          "TiltSensor",
          "WaterLeakDetector",
          "WaterValve",

          "DoorLock",
          "Fan",
          "Glass",
          "Identify",
          "Illuminance",
          "Indicator",
          "LeakGas",
          "LeakH2O",

          "MotorizedDoor",
          "PowerUse",
          "Presence",
          "RelativeHumidity",
          "Smoke",

          "Temperature",

          "Tilt",
          "Valve"
        };
      **/
   }
}

