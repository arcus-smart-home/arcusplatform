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
/**
 *
 */
package com.iris.driver.groovy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DevicePowerCapability;

/**
 *
 */
public class TestMatchers extends GroovyDriverTestCase {
   private Map<String, Object> matchers = new HashMap<String, Object>();
   private DriverBinding bindings;
   private GroovyDriverBuilder builder;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      bindings = new DriverBinding(ServiceLocator.getInstance(CapabilityRegistry.class), factory);
      builder = bindings.getBuilder();
   }

   @Test
   public void testNoMatchers() {
      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      assertTrue(GroovyValidator.getValidator().hasErrors());
      assertEquals(Predicates.alwaysFalse(), matcher);
   }

   @Test
   public void testVendorExactMatch() throws Exception {
      builder.addMatcher("vendor", "vendor");
      bindings.addDriverMatchers(matchers);

      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      GroovyValidator.throwIfErrors();

      AttributeMap map = AttributeMap.newMap();
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "vendor");
      assertTrue(matcher.apply(map));

      // currently case-sensitive
      map.set(DeviceCapability.KEY_VENDOR, "VeNdOr");
      assertFalse(matcher.apply(map));
   }

   @Test
   public void testVendorRegex() throws Exception {
      matchers.put("vendor", Pattern.compile("(?i)^vendor.*"));
      bindings.addDriverMatchers(matchers);

      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      GroovyValidator.throwIfErrors();

      AttributeMap map = AttributeMap.newMap();
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "vendor");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "VeNdOrAndThenSome");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "somethinElseVendor");
      assertFalse(matcher.apply(map));
   }

   @Test
   public void testVendorInvalid() {
      matchers.put("vendor", 42);
      bindings.addDriverMatchers(matchers);

      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      assertTrue(GroovyValidator.getValidator().hasErrors());
      assertEquals(Predicates.alwaysFalse(), matcher);
   }

   @Test
   public void testFirstClassMatchers() {
      String value = "value";

      {
         bindings.addDriverMatchers(Collections.<String, Object>singletonMap("vendor", value));
         Predicate<AttributeMap> matcher = builder.doCreateMatchers();
         AttributeMap attributes = AttributeMap.newMap();
         assertFalse(matcher.apply(attributes));
         attributes.set(DeviceCapability.KEY_VENDOR, value);
      }

      {
         bindings.addDriverMatchers(Collections.<String, Object>singletonMap("model", value));
         Predicate<AttributeMap> matcher = builder.doCreateMatchers();
         AttributeMap attributes = AttributeMap.newMap();
         assertFalse(matcher.apply(attributes));
         attributes.set(DeviceCapability.KEY_MODEL, value);
      }

      {
         bindings.addDriverMatchers(Collections.<String, Object>singletonMap("protocol", value));
         Predicate<AttributeMap> matcher = builder.doCreateMatchers();
         AttributeMap attributes = AttributeMap.newMap();
         assertFalse(matcher.apply(attributes));
         attributes.set(DeviceAdvancedCapability.KEY_PROTOCOL, value);
      }

      {
         bindings.addDriverMatchers(Collections.<String, Object>singletonMap("subprotocol", value));
         Predicate<AttributeMap> matcher = builder.doCreateMatchers();
         AttributeMap attributes = AttributeMap.newMap();
         assertFalse(matcher.apply(attributes));
         attributes.set(DeviceAdvancedCapability.KEY_SUBPROTOCOL, value);
      }

      {
         bindings.addDriverMatchers(Collections.<String, Object>singletonMap("protocolid", value));
         Predicate<AttributeMap> matcher = builder.doCreateMatchers();
         AttributeMap attributes = AttributeMap.newMap();
         assertFalse(matcher.apply(attributes));
         attributes.set(DeviceAdvancedCapability.KEY_PROTOCOLID, value);
      }
   }

   @Test
   public void testAndMatchers() {
      matchers.put("vendor", Pattern.compile("(?i)^vendor.*"));
      matchers.put("model", "model");
      bindings.addDriverMatchers(matchers);

      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      assertFalse(GroovyValidator.getValidator().hasErrors());

      AttributeMap map = AttributeMap.newMap();
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "vendor");
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "Modelo");
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "model");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "somethinElseVendor");
      assertFalse(matcher.apply(map));
   }

   @Test
   public void testOrMatchers() {
      matchers.put("vendor", "vendor");
      matchers.put("model", "model1");
      bindings.addDriverMatchers(matchers);

      matchers.put("vendor", "vendor");
      matchers.put("model", "model2");
      bindings.addDriverMatchers(matchers);

      matchers.put("vendor", "vendor");
      matchers.put("model", "model3");
      bindings.addDriverMatchers(matchers);
      
      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      assertFalse(GroovyValidator.getValidator().hasErrors());

      AttributeMap map = AttributeMap.newMap();
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_VENDOR, "vendor");
      assertFalse(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "model1");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "model2");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "model3");
      assertTrue(matcher.apply(map));

      map.set(DeviceCapability.KEY_MODEL, "model4");
      assertFalse(matcher.apply(map));

   }

   @Test
   public void testCapabilityMatcher() {
      matchers.put(DevicePowerCapability.ATTR_LINECAPABLE, true);
      bindings.addDriverMatchers(matchers);

      Predicate<AttributeMap> matcher = builder.doCreateMatchers();
      assertFalse(GroovyValidator.getValidator().toString(), GroovyValidator.getValidator().hasErrors());

      AttributeMap map = AttributeMap.newMap();
      assertFalse(matcher.apply(map));

      map.set(DevicePowerCapability.KEY_LINECAPABLE, false);
      assertFalse(matcher.apply(map));

      map.set(DevicePowerCapability.KEY_LINECAPABLE, true);
      assertTrue(matcher.apply(map));
   }

}

