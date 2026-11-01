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
package com.iris.modelmanager.engine;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.Properties;

import org.junit.Test;

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverExecutionProfile;
import com.iris.bootstrap.config.PropertiesConfigurationProvider;
import com.iris.modelmanager.context.ModelManagerBaseCassandraModule;

public class ExecutionEngineTest {

   @Test
   public void testSessionUsesConfiguredLocalDatacenter() {
      assertEquals("DC1", driverConfig().getString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER));
   }

   @Test
   public void testSessionUsesServiceRequestTimeout() {
      assertEquals(Duration.ofSeconds(12), driverConfig().getDuration(DefaultDriverOption.REQUEST_TIMEOUT));
   }

   private static DriverExecutionProfile driverConfig() {
      Properties properties = new Properties();
      properties.setProperty("cassandra.local.datacenter", "DC1");

      // consistency is normally set by BaseCassandraModule.configure(), which connects to Cassandra
      ModelManagerBaseCassandraModule module = new ModelManagerBaseCassandraModule(new PropertiesConfigurationProvider(properties), null) {
         @Override
         protected DefaultConsistencyLevel getConsistencyLevel() {
            return DefaultConsistencyLevel.LOCAL_QUORUM;
         }
      };

      return ExecutionEngine.driverConfig(module.getProfile()).getInitialConfig().getDefaultProfile();
   }
}
