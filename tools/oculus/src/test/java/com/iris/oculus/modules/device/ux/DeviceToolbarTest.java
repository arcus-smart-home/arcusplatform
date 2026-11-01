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
package com.iris.oculus.modules.device.ux;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.Arrays;

import javax.swing.JButton;

import org.junit.Test;

import com.iris.client.capability.DeviceSettings;
import com.iris.client.model.DeviceModel;

public class DeviceToolbarTest {
   private final DeviceToolbar toolbar = new DeviceToolbar();

   private static DeviceModel device(String... caps) {
      DeviceModel device = createNiceMock(DeviceModel.class);
      expect(device.getCaps()).andReturn(Arrays.asList(caps)).anyTimes();
      replay(device);
      return device;
   }

   private JButton settingsButton() {
      return find(toolbar.getComponent());
   }

   private static JButton find(Container parent) {
      for(Component child: parent.getComponents()) {
         if(child instanceof JButton && "Device Settings".equals(((JButton) child).getText())) {
            return (JButton) child;
         }
         if(child instanceof Container) {
            JButton found = find((Container) child);
            if(found != null) {
               return found;
            }
         }
      }
      return null;
   }

   @Test
   public void testDisabledUntilADeviceIsSelected() {
      assertFalse(settingsButton().isEnabled());
   }

   @Test
   public void testEnabledForADeviceWithSettings() {
      toolbar.onEvent(device("base", "dev", DeviceSettings.NAMESPACE));

      assertTrue(settingsButton().isEnabled());
   }

   @Test
   public void testDisabledForADeviceWithoutSettings() {
      toolbar.onEvent(device("base", "dev", DeviceSettings.NAMESPACE));

      toolbar.onEvent(device("base", "dev"));

      assertFalse(settingsButton().isEnabled());
   }

   @Test
   public void testDisabledWhenTheSelectionClears() {
      toolbar.onEvent(device("base", "dev", DeviceSettings.NAMESPACE));

      toolbar.onEvent(null);

      assertFalse(settingsButton().isEnabled());
   }
}
