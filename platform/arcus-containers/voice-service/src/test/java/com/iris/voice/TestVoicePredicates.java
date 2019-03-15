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
package com.iris.voice;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;

public class TestVoicePredicates {

   @Test
   public void testIsHubOffline() {
      assertFalse(VoicePredicates.isHubOffline(null));

      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE));

      assertFalse(VoicePredicates.isHubOffline(m));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(HubCapability.NAMESPACE));
      m.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);

      assertFalse(VoicePredicates.isHubOffline(m));

      m.setAttribute(HubCapability.ATTR_STATE, HubCapability.STATE_DOWN);

      assertTrue(VoicePredicates.isHubOffline(m));
   }

   @Test
   public void testIsHubRequired() {
      assertFalse(VoicePredicates.isHubRequired(null));

      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(PersonCapability.NAMESPACE));

      assertFalse(VoicePredicates.isHubRequired(m));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(SceneCapability.NAMESPACE));

      assertTrue(VoicePredicates.isHubRequired(m));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE));

      assertFalse(VoicePredicates.isHubRequired(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZIGB");
      assertTrue(VoicePredicates.isHubRequired(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZWAV");
      assertTrue(VoicePredicates.isHubRequired(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "SCOM");
      assertTrue(VoicePredicates.isHubRequired(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "IPCD");
      assertFalse(VoicePredicates.isHubRequired(m));

   }

   @Test
   public void testIsDeviceOffline() {
      assertFalse(VoicePredicates.isDeviceOffline(null, false));

      Model m = new SimpleModel();
      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DeviceCapability.NAMESPACE));

      assertFalse(VoicePredicates.isDeviceOffline(m, false));

      m.setAttribute(
         Capability.ATTR_CAPS,
         ImmutableSet.of(
            DeviceCapability.NAMESPACE,
            DeviceAdvancedCapability.NAMESPACE,
            DeviceConnectionCapability.NAMESPACE
         )
      );
      m.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      m.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZIGB");
      assertTrue(VoicePredicates.isDeviceOffline(m, true));
      assertFalse(VoicePredicates.isDeviceOffline(m, false));

      m.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      assertTrue(VoicePredicates.isDeviceOffline(m, false));
   }

   @Test
   public void testIsJammed() {
      Model m = new SimpleModel();
      assertFalse(VoicePredicates.isLockJammed(m));

      m.setAttribute(Capability.ATTR_CAPS, ImmutableSet.of(DoorLockCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE));
      assertFalse(VoicePredicates.isLockJammed(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of("some_error", "foo"));
      assertFalse(VoicePredicates.isLockJammed(m));

      m.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of("WARN_JAM", "jammed"));
      assertTrue(VoicePredicates.isLockJammed(m));
   }

}

