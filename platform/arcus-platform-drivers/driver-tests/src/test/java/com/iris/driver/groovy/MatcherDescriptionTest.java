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
package com.iris.driver.groovy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.MatchCriterion;
import com.iris.driver.MatchCriterion.Comparison;
import com.iris.messages.capability.DeviceCapability;
import com.iris.protocol.zwave.ZWaveProtocol;

/**
 * Loads MatcherDescriptions.driver, which has four matcher lines, and checks what the driver
 * definition says each line tests.
 */
public class MatcherDescriptionTest extends GroovyDriverTestCase {
   private static final AttributeKey<Integer> MANUFACTURER = AttributeKey.create(ZWaveProtocol.ATTR_MANUFACTURER, Integer.class);
   private static final AttributeKey<Integer> PRODUCT_TYPE = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTTYPE, Integer.class);
   private static final AttributeKey<Integer> PRODUCT_ID = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTID, Integer.class);

   private DeviceDriver driver;
   private List<List<MatchCriterion>> matchers;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      driver = factory.load("MatcherDescriptions.driver");
      matchers = driver.getDefinition().getMatchers();
   }

   @Test
   public void testEachLineIsDescribedSeparately() {
      assertEquals(4, matchers.size());
      // 0x0063, 0x4F50 and 0x3032 / 0x3035
      assertEquals(
         Arrays.asList(
            new MatchCriterion(ZWaveProtocol.ATTR_MANUFACTURER, Comparison.EQUALS, "99"),
            new MatchCriterion(ZWaveProtocol.ATTR_PRODUCTTYPE, Comparison.EQUALS, "20304"),
            new MatchCriterion(ZWaveProtocol.ATTR_PRODUCTID, Comparison.EQUALS, "12338")
         ),
         matchers.get(0)
      );
      assertEquals(
         Arrays.asList(
            new MatchCriterion(ZWaveProtocol.ATTR_MANUFACTURER, Comparison.EQUALS, "99"),
            new MatchCriterion(ZWaveProtocol.ATTR_PRODUCTTYPE, Comparison.EQUALS, "20304"),
            new MatchCriterion(ZWaveProtocol.ATTR_PRODUCTID, Comparison.EQUALS, "12341")
         ),
         matchers.get(1)
      );
   }

   @Test
   public void testListOfValues() {
      assertEquals(
         Arrays.asList(
            new MatchCriterion(ZWaveProtocol.ATTR_MANUFACTURER, Comparison.EQUALS, "1"),
            new MatchCriterion(ZWaveProtocol.ATTR_PRODUCTID, Comparison.IN, "[4, 8]")
         ),
         matchers.get(2)
      );
   }

   @Test
   public void testRegex() {
      assertEquals(
         Collections.singletonList(new MatchCriterion(DeviceCapability.ATTR_VENDOR, Comparison.REGEX, "^Jas")),
         matchers.get(3)
      );
   }

   @Test
   public void testDriverMatchesEveryLine() {
      assertTrue(driver.supports(zwave(0x0063, 0x4F50, 0x3032)));
      assertTrue(driver.supports(zwave(0x0063, 0x4F50, 0x3035)));
      assertTrue(driver.supports(zwave(0x0001, 0x9999, 0x08)));
      assertFalse(driver.supports(zwave(0x0063, 0x4F50, 0x3033)));

      AttributeMap vendor = AttributeMap.newMap();
      vendor.set(DeviceCapability.KEY_VENDOR, "Jasco");
      assertTrue(driver.supports(vendor));
   }

   private static AttributeMap zwave(int manufacturer, int productType, int productId) {
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(MANUFACTURER, manufacturer);
      attributes.set(PRODUCT_TYPE, productType);
      attributes.set(PRODUCT_ID, productId);
      return attributes;
   }
}
