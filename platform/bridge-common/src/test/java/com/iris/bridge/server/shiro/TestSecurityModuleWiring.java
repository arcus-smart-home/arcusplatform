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
package com.iris.bridge.server.shiro;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.bridge.MockDaoSecurityModule;
import com.iris.security.SessionConfig;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * Guards the parts of {@code SecurityModule} that shiro-guice wires by bean
 * property rather than by an explicit Guice binding, since nothing else here
 * fails loudly if that mechanism changes under a Shiro upgrade.
 *
 * <p>
 * Sessions are expired by a Cassandra TTL plus the lazy checks in
 * {@code ShiroClient}, so Shiro's own session validation scheduler must stay
 * off: if it came back on it would walk every session in the DAO on a timer.
 * </p>
 */
@Modules({ MockDaoSecurityModule.class, ShiroModule.class })
public class TestSecurityModuleWiring extends IrisTestCase {

   @Inject
   private org.apache.shiro.mgt.SecurityManager securityManager;

   @Inject
   private SessionConfig config;

   /**
    * {@code SecurityModule} binds the session manager inside the private module
    * and never exposes it, so reach it through the exposed security manager.
    */
   private DefaultSessionManager sessionManager;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      sessionManager =
            (DefaultSessionManager) ((SessionsSecurityManager) securityManager).getSessionManager();
   }

   @Test
   public void testSecurityManagerIsTheRealmCollectionConstructor() {
      Assert.assertTrue("expected a DefaultSecurityManager, got " + securityManager.getClass().getName(),
            securityManager instanceof DefaultSecurityManager);
   }

   @Test
   public void testSessionValidationSchedulerIsDisabled() {
      Assert.assertFalse("Shiro's session validation scheduler must stay disabled",
            sessionManager.isSessionValidationSchedulerEnabled());
   }

   @Test
   public void testGlobalSessionTimeoutComesFromSessionConfig() {
      Assert.assertEquals(config.getDefaultSessionTimeoutInSecs() * 1000L, sessionManager.getGlobalSessionTimeout());
   }
}
