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
package com.iris.modelmanager.context;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.iris.bootstrap.config.PropertiesConfigurationProvider;

public class ModelManagerBaseCassandraModuleTest {

   @Test
   public void testLocalDatacenterDefaultsToDatacenter1() {
      assertEquals("datacenter1", localDatacenter(null, new Properties()));
   }

   @Test
   public void testLocalDatacenterComesFromConfig() {
      assertEquals("DC1", localDatacenter(null, properties("cassandra.local.datacenter", "DC1")));
   }

   @Test
   public void testNamedSchemaReadsItsOwnLocalDatacenter() {
      assertEquals("DC2", localDatacenter("history", properties("cassandra.history.local.datacenter", "DC2")));
   }

   @Test
   public void testNamedSchemaDoesNotFallBackToPlainProperty() {
      assertEquals("datacenter1", localDatacenter("history", properties("cassandra.local.datacenter", "DC1")));
   }

   private static String localDatacenter(String schema, Properties properties) {
      return new ModelManagerBaseCassandraModule(new PropertiesConfigurationProvider(properties), schema) {}
            .getProfile()
            .getLocalDatacenter();
   }

   private static Properties properties(String key, String value) {
      Properties properties = new Properties();
      properties.setProperty(key, value);
      return properties;
   }
}
