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
package com.iris.oauth.auth;

import com.iris.bridge.server.auth.basic.BasicAuthCredentials;
import org.junit.Before;
import org.junit.Test;

import com.iris.oauth.OAuthConfig;
import com.iris.oauth.app.AppRegistry;
import com.iris.test.IrisTestCase;

public class TestApplicationAuth extends IrisTestCase {

   private ApplicationAuth auth;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      OAuthConfig config = new OAuthConfig();
      config.setAppsPath("classpath:///applications.json");
      auth = new ApplicationAuth(new AppRegistry(config), null);
   }

   @Test
   public void testMissingHeaderFailsAuth() {
      assertFalse(auth.doCheckAuthorization(null));
      assertFalse(auth.doCheckAuthorization(new BasicAuthCredentials("", "")));
   }

   @Test
   public void testAppDoesntExistFailsAuth() {
      assertFalse(auth.doCheckAuthorization(new BasicAuthCredentials("wrong", "app1")));
   }

   @Test
   public void testWrongSecretFailsAuth() {
      assertFalse(auth.doCheckAuthorization(new BasicAuthCredentials("app1", "foobar")));
   }

   @Test
   public void testPassesAuth() {
      assertTrue(auth.doCheckAuthorization(new BasicAuthCredentials("app1", "app1")));
   }
}

