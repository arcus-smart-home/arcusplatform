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

import org.easymock.EasyMock;
import org.junit.Test;

import com.iris.bootstrap.config.ConfigurationProvider;

public class ModelManagerBaseCassandraModuleTest {

   /** ExecutionEngine opens its own session from the profile, so a cluster whose datacenter isn't datacenter1 depends on this. */
   @Test
   public void testProfileCarriesLocalDatacenter() {
      ConfigurationProvider config = EasyMock.createNiceMock(ConfigurationProvider.class);
      EasyMock.replay(config);

      ModelManagerBaseCassandraModule module = new ModelManagerBaseCassandraModule(config, null) {
         @Override
         protected String getLocalDatacenter() {
            return "DC1";
         }
      };

      assertEquals("DC1", module.getProfile().getLocalDatacenter());
   }
}
