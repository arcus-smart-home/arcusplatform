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
package com.iris.oculus.modules.device.settings;

import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.attribute;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.booleanEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.driverOnlyEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.enumEntry;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.param;
import static com.iris.oculus.modules.device.settings.DeviceSettingFixtures.rangeEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.iris.client.ClientMessage;
import com.iris.client.impl.ClientMessageSerializer;

public class DeviceSettingParamTest {

   private static final String VALUE_CHANGE =
         "{\"type\":\"base:ValueChange\","
         + "\"headers\":{\"source\":\"DRIV:dev:00000000-0000-0000-0000-000000000000\",\"isRequest\":false},"
         + "\"payload\":{\"messageType\":\"base:ValueChange\",\"attributes\":{\"devsettings:params\":["
         + "{\"paramId\":\"ledMode\",\"name\":\"ledMode\",\"description\":\"LED behavior\",\"type\":\"enum\","
         + "\"values\":{\"2\":\"Follows Switch\",\"0\":\"Always Off\"},\"dflt\":0,\"value\":2,"
         + "\"zwaveParamNo\":27,\"zwaveParamSize\":1},"
         + "{\"paramId\":\"autoOff\",\"name\":\"autoOff\",\"description\":\"Auto off\",\"type\":\"range\","
         + "\"min\":0,\"max\":4294967295,\"dflt\":0,\"value\":4294967295,"
         + "\"zwaveParamNo\":28,\"zwaveParamSize\":4}"
         + "]}}}";

   @Test
   public void testValuesFromTheWireAreWholeNumbers() {
      ClientMessage message = ClientMessageSerializer.deserialize(VALUE_CHANGE, ClientMessage.class);
      Object attribute = message.getEvent().getAttribute("devsettings:params");
      Object rawValue = ((Map<?, ?>) ((List<?>) attribute).get(0)).get("value");
      // the reason toValue exists, if this stops being a Double the conversion can be simplified
      assertEquals(Double.class, rawValue.getClass());

      List<DeviceSettingParam> params = DeviceSettingParam.fromAttribute(attribute);

      assertEquals(2, params.size());
      assertEquals("2", params.get(0).getValue());
      assertEquals("0", params.get(0).getDefault());
      assertEquals("4294967295", params.get(1).getValue());
      assertEquals(Long.valueOf(4294967295L), params.get(1).getMax());
   }

   @Test
   public void testEnumKeepsDriverOrder() {
      ClientMessage message = ClientMessageSerializer.deserialize(VALUE_CHANGE, ClientMessage.class);
      DeviceSettingParam param = DeviceSettingParam.fromAttribute(message.getEvent().getAttribute("devsettings:params")).get(0);

      assertEquals(Arrays.asList("2", "0"), new ArrayList<>(param.getValues().keySet()));
      assertEquals("Follows Switch", param.getValues().get("2"));
   }

   @Test
   public void testToValue() {
      assertNull(DeviceSettingParam.toValue(null));
      assertEquals("1", DeviceSettingParam.toValue(1.0));
      assertEquals("100", DeviceSettingParam.toValue(100.0));
      assertEquals("0", DeviceSettingParam.toValue(0.0));
      assertEquals("-3", DeviceSettingParam.toValue(-3.0));
      assertEquals("5", DeviceSettingParam.toValue(5L));
      assertEquals("5", DeviceSettingParam.toValue("5"));
      assertEquals("5", DeviceSettingParam.toValue("5.0"));
      assertEquals("1", DeviceSettingParam.toValue(Boolean.TRUE));
      assertEquals("0", DeviceSettingParam.toValue(Boolean.FALSE));
      assertEquals("abc", DeviceSettingParam.toValue("abc"));
   }

   @Test
   public void testMissingOrUnusableAttribute() {
      assertTrue(DeviceSettingParam.fromAttribute(null).isEmpty());
      assertTrue(DeviceSettingParam.fromAttribute(Collections.emptyList()).isEmpty());
      assertTrue(DeviceSettingParam.fromAttribute("not a list").isEmpty());
   }

   @Test
   public void testUnusableEntriesAreSkipped() {
      List<Object> attribute = new ArrayList<>();
      attribute.add("not a map");
      attribute.add(Collections.singletonMap("name", "noParamId"));
      attribute.add(enumEntry("ledMode", 0, 1));

      List<DeviceSettingParam> params = DeviceSettingParam.fromAttribute(attribute);

      assertEquals(1, params.size());
      assertEquals("ledMode", params.get(0).getParamId());
   }

   @Test
   public void testMissingKeysAreTolerated() {
      DeviceSettingParam param = DeviceSettingParam.fromAttribute(
            Collections.singletonList(Collections.singletonMap("paramId", "bare"))
      ).get(0);

      assertEquals(DeviceSettingParam.Type.UNSUPPORTED, param.getType());
      assertEquals("", param.getDescription());
      assertNull(param.getValue());
      assertNull(param.getDefault());
      assertEquals("Not set", param.describe(param.getValue()));
      assertNotNull(param.validate("1"));
   }

   @Test
   public void testDriverOnlySettingHasNoZWaveDetail() {
      assertEquals("volume, driver setting, 0 to 10", param(driverOnlyEntry("volume", 0, 10, 5, 7)).getDetail());
      assertEquals("volume, Z-Wave parameter 3, size 1, 0 to 10", param(rangeEntry("volume", 0, 10, 5, 7)).getDetail());
   }

   @Test
   public void testDisplayNameFromParamId() {
      assertEquals("Led Mode", param(enumEntry("ledMode", 0, 0)).getDisplayName());
      assertEquals("Input Type R1", param(enumEntry("inputTypeR1", 0, 0)).getDisplayName());
      assertEquals("Btn 1 Led Color", param(enumEntry("btn1LedColor", 0, 0)).getDisplayName());
      assertEquals("Power Restore", param(enumEntry("power_restore", 0, 0)).getDisplayName());
   }

   @Test
   public void testDisplayNameKeepsARealName() {
      Map<String, Object> entry = enumEntry("ledMode", 0, 0);
      entry.put("name", "Indicator LED");

      assertEquals("Indicator LED", param(entry).getDisplayName());
   }

   @Test
   public void testDescribe() {
      DeviceSettingParam enumParam = param(enumEntry("ledMode", 0, 1));
      assertEquals("Always On (1)", enumParam.describe("1"));
      assertEquals("Unlisted (9)", enumParam.describe("9"));

      DeviceSettingParam range = param(rangeEntry("level", 0, 99, 0, 5));
      assertEquals("5", range.describe("5"));

      DeviceSettingParam bool = param(booleanEntry("enabled", 0, 1));
      assertEquals("On (1)", bool.describe("1"));
      assertEquals("Off (0)", bool.describe("0"));
   }

   @Test
   public void testValidateMatchesTheDriver() {
      DeviceSettingParam enumParam = param(enumEntry("ledMode", 0, 1));
      assertNull(enumParam.validate("2"));
      assertNotNull(enumParam.validate("9"));
      assertNotNull(enumParam.validate("Always On"));

      DeviceSettingParam range = param(rangeEntry("offset", -3, 3, 0, 0));
      assertNull(range.validate("-3"));
      assertNull(range.validate("3"));
      assertNotNull(range.validate("4"));
      assertNotNull(range.validate("-4"));
      assertNotNull(range.validate("1.5"));
      assertNotNull(range.validate("1.0"));
      assertNotNull(range.validate(""));
      assertNotNull(range.validate(null));

      DeviceSettingParam bool = param(booleanEntry("enabled", 0, 0));
      assertNull(bool.validate("0"));
      assertNull(bool.validate("1"));
      assertNotNull(bool.validate("true"));
      assertNotNull(bool.validate("2"));
   }

   @Test
   public void testHasSameDefinitionIgnoresTheValue() {
      DeviceSettingParam param = param(rangeEntry("level", 0, 99, 0, 5));

      assertTrue(param.hasSameDefinition(param(rangeEntry("level", 0, 99, 0, 6))));
      assertFalse(param.hasSameDefinition(param(rangeEntry("level", 0, 50, 0, 5))));
      assertFalse(param.hasSameDefinition(param(rangeEntry("other", 0, 99, 0, 5))));
      assertFalse(param.hasSameDefinition(param(driverOnlyEntry("level", 0, 99, 0, 5))));
   }

   @Test
   public void testAttributeHelperKeepsOrder() {
      List<DeviceSettingParam> params = DeviceSettingParam.fromAttribute(
            attribute(enumEntry("first", 0, 0), booleanEntry("second", 0, 0))
      );

      assertEquals("first", params.get(0).getParamId());
      assertEquals("second", params.get(1).getParamId());
   }

   @Test
   public void theDisplayDescriptionDropsTheParameterNumber() {
      Map<String, Object> entry = new java.util.HashMap<>();
      entry.put("paramId", "sirenVolume");
      entry.put("type", "range");
      entry.put("description", "Parameter 6: Siren volume");

      assertEquals("Siren volume", DeviceSettingFixtures.param(entry).getDisplayDescription());
   }
}
