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
package com.iris.driver.groovy.devicesettings;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.protocol.Protocol;

public class DeviceSettingsStartedHandlerTest {

   @Test
   @SuppressWarnings("unchecked")
   public void rebuildsTheParamsFromTheDriverWithoutTouchingTheDevice() throws Exception {
      DeviceSettingsParamDefinition rampRate = DeviceSettingsParamDefinition.builder()
            .withParamId("rampRate")
            .withLabel("Ramp rate")
            .withUnit("s")
            .withDescription("Parameter 9: Ramp rate (seconds)")
            .withZwaveParamNo(9)
            .withMin(0).withMax(99).withDefaultValue(1)
            .build();
      DeviceSettingsStartedHandler handler = new DeviceSettingsStartedHandler(new DeviceSettingsContext(Arrays.asList(rampRate)));

      DeviceDriverContext context = createNiceMock(DeviceDriverContext.class);
      expect(context.getVariable("devsettings:rampRate")).andReturn(3L).anyTimes();
      Capture<Object> saved = Capture.newInstance();
      expect(context.setAttributeValue(eq((AttributeKey<Object>) (AttributeKey<?>) DeviceSettingsCapability.KEY_PARAMS), capture(saved))).andReturn(null);
      context.sendToDevice(EasyMock.<Protocol<Object>>anyObject(), anyObject(), anyInt());
      expectLastCall().andThrow(new AssertionError("sent to the device on start")).anyTimes();
      context.sendToDevice(anyString(), anyObject(byte[].class), anyInt());
      expectLastCall().andThrow(new AssertionError("sent to the device on start")).anyTimes();
      replay(context);

      assertFalse(handler.handleEvent(context, DriverEvent.driverStarted()));

      List<Map<String, Object>> params = (List<Map<String, Object>>) saved.getValue();
      assertEquals(1, params.size());
      assertEquals("Ramp rate", params.get(0).get("name"));
      assertEquals("s", params.get(0).get("unit"));
      assertEquals(3L, params.get(0).get("value"));
      verify(context);
   }
}
