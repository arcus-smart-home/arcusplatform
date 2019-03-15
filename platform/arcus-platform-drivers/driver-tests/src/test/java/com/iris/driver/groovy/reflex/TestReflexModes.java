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
package com.iris.driver.groovy.reflex;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.driver.DeviceDriver;
import com.iris.driver.groovy.GroovyDriverTestCase;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.reflex.ReflexRunMode;


/**
 *
 */
public class TestReflexModes extends GroovyDriverTestCase {
   private DeviceDriver driver;

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return ImmutableSet.of(new ReflexPlugin(), new ControlProtocolPlugin());
   }
   
   @Test
   public void testReflexDefault() throws Exception {
      driver = factory.load("ReflexDriverDefault.driver");
      
      assertEquals(ReflexRunMode.HUB, driver.getDefinition().getReflexes().getMode());
      assertEquals(0, driver.getDefinition().getMinimumRequiredReflexVersion());
   }

   @Test
   public void testReflexMixed() throws Exception {
      driver = factory.load("ReflexDriverMixed.driver");
      
      assertEquals(ReflexRunMode.MIXED, driver.getDefinition().getReflexes().getMode());
      assertEquals(0, driver.getDefinition().getMinimumRequiredReflexVersion());
   }

   @Test
   public void testReflexHubRequired() throws Exception {
      driver = factory.load("ReflexDriverHubRequired.driver");
      
      assertEquals(ReflexRunMode.HUB, driver.getDefinition().getReflexes().getMode());
      assertEquals(0, driver.getDefinition().getMinimumRequiredReflexVersion());
   }

   @Test
   public void testReflexPlatformOnly() throws Exception {
      driver = factory.load("ReflexDriverPlatformOnly.driver");
      
      assertEquals(ReflexRunMode.PLATFORM, driver.getDefinition().getReflexes().getMode());
      assertEquals(0, driver.getDefinition().getMinimumRequiredReflexVersion());
   }

}

