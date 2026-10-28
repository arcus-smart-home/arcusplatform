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
package com.iris.security;

import java.util.Collections;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 * Guards the session manager that the bridges actually run.
 *
 * <p>
 * {@code TestSecurityModuleWiring} covers {@code SecurityModule}'s own
 * session manager, but client-bridge and api-bridge both override
 * {@code bindSessionManager} without calling {@code super}, so they never see
 * the {@code shiro.sessionValidationSchedulerEnabled} constant bound there.
 * In those containers the scheduler is turned off by
 * {@link GuicySessionManager#init()} under {@code @PostConstruct} instead,
 * which is the path these tests pin.
 * </p>
 */
public class GuicySessionManagerTest extends Assert {

   private GuicySessionManager manager() {
      SessionDAO dao = EasyMock.createNiceMock(SessionDAO.class);
      CacheManager cacheManager = EasyMock.createNiceMock(CacheManager.class);
      EasyMock.replay(dao, cacheManager);

      return new GuicySessionManager(
            dao, cacheManager, Collections.emptySet(), new SessionConfig());
   }

   /**
    * The reason {@link GuicySessionManager#init()} has to exist: Shiro ships
    * the validation scheduler enabled, so losing the {@code @PostConstruct}
    * call would silently leave it on and walk every row in the session DAO on
    * a timer.
    */
   @Test
   public void testShiroDefaultsTheValidationSchedulerOn() {
      assertTrue("Shiro changed its default; init() may no longer be load-bearing",
            new DefaultSessionManager().isSessionValidationSchedulerEnabled());
   }

   @Test
   public void testInitDisablesTheValidationScheduler() {
      GuicySessionManager manager = manager();
      manager.init();

      assertFalse("Shiro's session validation scheduler must stay disabled",
            manager.isSessionValidationSchedulerEnabled());
   }

   @Test
   public void testInitAppliesGlobalSessionTimeoutFromConfig() {
      GuicySessionManager manager = manager();
      manager.init();

      assertEquals(new SessionConfig().getDefaultSessionTimeoutInSecs() * 1000L,
            manager.getGlobalSessionTimeout());
   }
}
