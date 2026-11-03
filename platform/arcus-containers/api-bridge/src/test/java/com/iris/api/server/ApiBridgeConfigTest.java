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
package com.iris.api.server;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

import com.iris.security.authz.AuthzModule;

public class ApiBridgeConfigTest {

   // without this setting the bridge falls back to permissions alone, which can't tell where the
   // addressed object belongs, and nothing else would fail
   @Test
   public void testApiKeysGoThroughBothAuthorizers() throws Exception {
      Properties properties = new Properties();
      try(InputStream in = new FileInputStream("src/dist/conf/api-bridge.properties")) {
         properties.load(in);
      }
      assertEquals(AuthzModule.AUTHZ_ALGORITHM_LAYERED, properties.getProperty(AuthzModule.AUTHZ_ALGORITHM_PROP));
   }
}
