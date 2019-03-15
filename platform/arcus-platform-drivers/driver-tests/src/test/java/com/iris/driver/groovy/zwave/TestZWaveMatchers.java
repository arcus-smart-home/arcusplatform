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
package com.iris.driver.groovy.zwave;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.util.IrisCollections;
import com.iris.validators.ValidationException;

public class TestZWaveMatchers extends GroovyDriverTestCase {

   @Test
   public void TestManufacturerMatcher() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_MANUFACTURER, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherManufacturer.driver");

      attributes.set(key, 1);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestManufacturerMatcherSet() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_MANUFACTURER, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherManufacturerSet.driver");

      attributes.set(key, 4);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 8);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 15);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 16);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 23);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 42);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestProductTypeMatcher() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTTYPE, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherProductType.driver");

      attributes.set(key, 1);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestProductTypeMatcherSet() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTTYPE, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherProductTypeSet.driver");

      attributes.set(key, 4);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 8);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 15);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 16);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 23);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 42);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestProductIdMatcher() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTID, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherProductId.driver");

      attributes.set(key, 1);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestProductIdMatcherSet() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> key = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTID, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherProductIdSet.driver");

      attributes.set(key, 4);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 8);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 15);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 16);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 23);
      assertEquals(true, driver.supports(attributes));
      attributes.set(key, 42);
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, 2);
      assertEquals(false, driver.supports(attributes));
   }

   // FIXME command class matchers are broken
   @Ignore @Test
   public void TestCommandClassMatcher() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Set<Byte>> key = AttributeKey.createSetOf(ZWaveProtocol.ATTR_COMMANDCLASSES, Byte.class);
      DeviceDriver driver = factory.load("ZWaveMatcherCommandClass.driver");

      attributes.set(key, IrisCollections.setOf((byte)0x17));
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, IrisCollections.setOf((byte)0x04, (byte)0x08, (byte)0x0f, (byte)0x10, (byte)0x17, (byte)0x2a));
      assertEquals(true, driver.supports(attributes));
   }

   // FIXME command class matchers are broken
   @Ignore @Test
   public void TestCommandClassHighMatcher() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Set<Byte>> key = AttributeKey.createSetOf(ZWaveProtocol.ATTR_COMMANDCLASSES, Byte.class);
      DeviceDriver driver = factory.load("ZWaveMatcherCommandClassHigh.driver");

      attributes.set(key, IrisCollections.setOf((byte)0xff));
      assertEquals(true, driver.supports(attributes));

      attributes.set(key, IrisCollections.setOf((byte)0x04, (byte)0x08, (byte)0x0f, (byte)0x10, (byte)0x17, (byte)0x2a));
      assertEquals(false, driver.supports(attributes));
   }

   // FIXME command class matchers are broken
   @Ignore @Test
   public void TestCommandClassMatcherAll() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Set<Byte>> key = AttributeKey.createSetOf(ZWaveProtocol.ATTR_COMMANDCLASSES, Byte.class);
      DeviceDriver driver = factory.load("ZWaveMatcherCommandClassAll.driver");

      // Matches all in superset.
      attributes.set(key, IrisCollections.setOf((byte)0x04, (byte)0x08, (byte)0x0f, (byte)0x10, (byte)0x17, (byte)0x2a));
      assertEquals(true, driver.supports(attributes));

      // Exact match.
      attributes.set(key, IrisCollections.setOf((byte)0x08, (byte)0x0f, (byte)0x10));
      assertEquals(true, driver.supports(attributes));

      // Missing two command classes.
      attributes.set(key, IrisCollections.setOf((byte)0x08));
      assertEquals(false, driver.supports(attributes));

      // Missing one command class.
      attributes.set(key, IrisCollections.setOf((byte)0x04, (byte)0x08, (byte)0x0f, (byte)0x17, (byte)0x2a));
      assertEquals(false, driver.supports(attributes));
   }

   @Test
   public void TestMultipleMatchers() throws ValidationException {
      AttributeMap attributes = AttributeMap.newMap();
      AttributeKey<Integer> manufacturerKey = AttributeKey.create(ZWaveProtocol.ATTR_MANUFACTURER, Integer.class);
      AttributeKey<Integer> productTypeKey = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTTYPE, Integer.class);
      AttributeKey<Integer> productIdKey = AttributeKey.create(ZWaveProtocol.ATTR_PRODUCTID, Integer.class);
      DeviceDriver driver = factory.load("ZWaveMatcherMultiple.driver");

      // Set all to good values
      attributes.set(manufacturerKey, 1);
      attributes.set(productTypeKey, 3);
      attributes.set(productIdKey, 4);
      assertEquals(true, driver.supports(attributes));
      attributes.set(productIdKey, 8);
      assertEquals(true, driver.supports(attributes));
      attributes.set(productIdKey, 15);
      assertEquals(true, driver.supports(attributes));
      attributes.set(productIdKey, 16);
      assertEquals(true, driver.supports(attributes));
      attributes.set(productIdKey, 23);
      assertEquals(true, driver.supports(attributes));
      attributes.set(productIdKey, 42);
      assertEquals(true, driver.supports(attributes));

      // Set manufacturer to bad value
      attributes.set(manufacturerKey, 2);
      assertEquals(false, driver.supports(attributes));

      // Set manufacturer back to good value, set productType to bad value
      attributes.set(manufacturerKey, 1);
      attributes.set(productTypeKey, 2);
      assertEquals(false, driver.supports(attributes));

      // Set productType back to good value, set productId to bad value.
      attributes.set(productTypeKey, 3);
      attributes.set(productIdKey, 5);
      assertEquals(false, driver.supports(attributes));

      // Set everything to bad values.
      attributes.set(manufacturerKey, 2);
      attributes.set(productTypeKey, 2);
      assertEquals(false, driver.supports(attributes));
   }
}

