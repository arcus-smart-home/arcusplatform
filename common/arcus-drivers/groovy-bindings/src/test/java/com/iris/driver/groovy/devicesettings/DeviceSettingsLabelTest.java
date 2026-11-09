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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * A param's {@code label} is the short name clients show, sent as {@code name}, and
 * {@code unit} is the unit of a range value.
 */
public class DeviceSettingsLabelTest {

   private static final int TYPE = 0x0401;

   @Test
   public void labelAndUnitReachTheParamsAttribute() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(range("rampRate", 9, "Ramp rate", "s"));
      block.param(enumParam("ledMode", 2, "LED indicator"));

      List<Map<String, Object>> params = block.build().buildParamsAttribute(key -> null);
      assertEquals("rampRate", params.get(0).get("paramId"));
      assertEquals("Ramp rate", params.get(0).get("name"));
      assertEquals("s", params.get(0).get("unit"));
      assertEquals("LED indicator", params.get(1).get("name"));
      assertFalse(params.get(1).containsKey("unit"));
   }

   @Test
   public void paramWithoutALabelKeepsItsIdAsTheName() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(range("rampRate", 9, null, null));

      Map<String, Object> param = block.build().buildParamsAttribute(key -> null).get(0);
      assertEquals("rampRate", param.get("name"));
      assertFalse(param.containsKey("unit"));
   }

   @Test
   public void rejectsABlankLabel() {
      assertRejected(range("rampRate", 9, " ", null), "label must not be blank");
   }

   @Test
   public void rejectsALabelThatStartsOrEndsWithASpace() {
      assertRejected(range("rampRate", 9, "Ramp rate ", null), "label must not be blank or start or end with a space");
      assertRejected(range("rampRate", 9, " Ramp rate", null), "label must not be blank or start or end with a space");
   }

   @Test
   public void rejectsALabelThatIsNotText() {
      Map<String, Object> args = range("rampRate", 9, null, null);
      args.put("label", 5);
      assertRejected(args, "label must be a string");
   }

   @Test
   public void rejectsAUnitOnAnythingButARange() {
      Map<String, Object> args = enumParam("ledMode", 2, "LED indicator");
      args.put("unit", "s");
      assertRejected(args, "only a range has one");
   }

   @Test
   public void rejectsTwoParamsWithOneLabelOnTheSameProduct() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      block.param(range("rampRateOn", 9, "Ramp rate", "s"));
      block.param(range("rampRateOff", 27, "Ramp rate", "s"));
      try {
         block.build();
         fail("built two params with the same label");
      } catch (IllegalArgumentException e) {
         assertTrue(e.getMessage(), e.getMessage().contains("both have the label 'Ramp rate'"));
      }
   }

   @Test
   public void rejectsLabelsThatDifferOnlyInCase() {
      assertNotBuilt("both have the label",
            range("ledMode", 2, "LED indicator", null), range("ledColor", 23, "LED Indicator", null));
   }

   @Test
   public void comparesTheLabelsOfSettingsThatAreNotZWaveParameters() {
      // in either order, since the driver-only one has no parameter number to compare
      assertNotBuilt("both have the label", range("chimeLevel", 6, "Chime volume", "%"), driverOnly("chimeVolume", "Chime volume"));
      assertNotBuilt("both have the label", driverOnly("chimeVolume", "Chime volume"), range("chimeLevel", 6, "Chime volume", "%"));
      assertNotBuilt("both have the label", driverOnly("chimeVolume", "Chime volume"), driverOnly("doorbellVolume", "Chime volume"));
   }

   @Test
   public void allowsOneLabelOnDifferentProducts() {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      Map<String, Object> gen1 = range("supervisionTimeout", 2, "Supervision timeout", "s");
      gen1.put("products", Collections.singletonList(Arrays.asList(TYPE, 0x0101)));
      Map<String, Object> gen2 = range("reportTimeout", 5, "Supervision timeout", "ms");
      gen2.put("products", Collections.singletonList(Arrays.asList(TYPE, 0x0301)));
      block.param(gen1);
      block.param(gen2);

      assertEquals(2, block.build().getParams().size());
   }

   @SafeVarargs
   private static void assertNotBuilt(String reason, Map<String, Object>... params) {
      DeviceSettingsBlockContext block = new DeviceSettingsBlockContext();
      for (Map<String, Object> param : params) {
         block.param(param);
      }
      try {
         block.build();
         fail("built " + Arrays.asList(params));
      } catch (IllegalArgumentException e) {
         assertTrue(e.getMessage(), e.getMessage().contains(reason));
      }
   }

   private static Map<String, Object> driverOnly(String name, String label) {
      Map<String, Object> args = new HashMap<>();
      args.put("name", name);
      args.put("driverOnly", true);
      args.put("type", "range");
      args.put("min", 10);
      args.put("max", 100);
      args.put("label", label);
      return args;
   }

   private static void assertRejected(Map<String, Object> args, String reason) {
      try {
         new DeviceSettingsBlockContext().param(args);
         fail("accepted " + args);
      } catch (IllegalArgumentException e) {
         assertTrue(e.getMessage(), e.getMessage().contains(reason));
      }
   }

   private static Map<String, Object> range(String name, int id, String label, String unit) {
      Map<String, Object> args = new HashMap<>();
      args.put("name", name);
      args.put("id", id);
      args.put("type", "range");
      args.put("min", 0);
      args.put("max", 99);
      if (label != null) {
         args.put("label", label);
      }
      if (unit != null) {
         args.put("unit", unit);
      }
      return args;
   }

   private static Map<String, Object> enumParam(String name, int id, String label) {
      Map<String, Object> args = new HashMap<>();
      args.put("name", name);
      args.put("id", id);
      args.put("type", "enum");
      Map<String, String> values = new HashMap<>();
      values.put("0", "Off");
      values.put("1", "On");
      args.put("values", values);
      args.put("label", label);
      return args;
   }
}
