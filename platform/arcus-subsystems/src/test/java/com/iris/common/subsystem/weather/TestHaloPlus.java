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
package com.iris.common.subsystem.weather;

import java.util.HashMap;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.WeatherRadioCapability;
import com.iris.messages.capability.WeatherRadioCapability.StopPlayingStationRequest;
import com.iris.messages.model.subs.WeatherSubsystemModel;
import com.iris.util.TypeMarker;

public class TestHaloPlus extends WeatherSubsystemTestCase {

   public static final String HALO_RADIO_ALERT = WeatherRadioCapability.ATTR_ALERTSTATE + ":" + WeatherRadioCapability.ALERTSTATE_ALERT;
   
   @Test
   public void testOnStarted() {
      startSubsystem();
      WeatherSubsystemModel model = context.model();     
      Set<String> weatherRadioDevices = model.getWeatherRadios();
      
      assertTrue(weatherRadioDevices.contains(haloPlusDevice1.getAddress().getRepresentation()));
      assertTrue(weatherRadioDevices.contains(haloPlusDevice2.getAddress().getRepresentation()));
   }
   
   @Test
   public void testSnoozeAll() {
      startSubsystem();

      assertEquals(WeatherRadioCapability.ALERTSTATE_ALERT, haloPlusDevice1.getAttribute( WeatherRadioCapability.ATTR_ALERTSTATE));
      assertEquals(WeatherRadioCapability.ALERTSTATE_ALERT, haloPlusDevice2.getAttribute( WeatherRadioCapability.ATTR_ALERTSTATE));
      
      assertEquals(WeatherRadioCapability.PLAYINGSTATE_PLAYING, haloPlusDevice1.getAttribute( WeatherRadioCapability.ATTR_PLAYINGSTATE));
      assertEquals(WeatherRadioCapability.PLAYINGSTATE_PLAYING, haloPlusDevice2.getAttribute( WeatherRadioCapability.ATTR_PLAYINGSTATE));         
      
      subsystem.onSnoozeAllAlerts(weatherPlatformMessage(MessageBody.buildMessage("test",new HashMap<String,Object>())), context);

      assertContainsRequestMessageWithAttrs(StopPlayingStationRequest.NAME, new HashMap<String,Object>());
    
   }
   
   public static Set<String> getAlertingHaloDevices(SubsystemContext<WeatherSubsystemModel> context) {
      return context.model().getAttribute(TypeMarker.setOf(String.class), HALO_RADIO_ALERT, ImmutableSet.<String> of());
   }
   
   
   
}

