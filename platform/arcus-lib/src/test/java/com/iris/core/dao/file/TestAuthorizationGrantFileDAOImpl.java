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
package com.iris.core.dao.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.security.authz.AuthorizationGrant;

public class TestAuthorizationGrantFileDAOImpl {

   private static final UUID entityId = UUID.fromString("08300b50-c109-4528-a705-2510a53bcbe7");

   private AuthorizationGrantDAO fileDao = new AuthorizationGrantFileDAOImpl();

   @Test
   public void testFindForPerson() {
      List<AuthorizationGrant> grants = fileDao.findForEntity(entityId);
      assertNotNull(grants);
      assertEquals(1, grants.size());
   }

   @Test
   public void testSaveNewGrant() {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setAccountId(UUID.randomUUID());
      grant.setAccountOwner(false);
      grant.setEntityId(entityId);
      grant.setPlaceId(UUID.randomUUID());
      grant.setPlaceName("office");
      grant.addPermissions("dev:r:*");

      fileDao.save(grant);

      List<AuthorizationGrant> grants = fileDao.findForEntity(entityId);
      assertEquals(2, grants.size());
   }

   @Test
   public void testUpdateExistingGrant() {
      List<AuthorizationGrant> grants = fileDao.findForEntity(entityId);
      AuthorizationGrant grant = grants.get(0);
      grant.addPermissions("dev:r:" + UUID.randomUUID().toString());
      fileDao.save(grant);

      grants = fileDao.findForEntity(entityId);
      assertEquals(grant, grants.get(0));
   }

   @Test
   public void testRemoveGrant() {
      List<AuthorizationGrant> grants = fileDao.findForEntity(entityId);
      assertNotNull(grants);
      assertEquals(1, grants.size());
      fileDao.removeGrant(entityId, UUID.fromString("6f293fd5-b65b-4a63-a5ed-1309eda1122a"));
      grants = fileDao.findForEntity(entityId);
      assertNotNull(grants);
      assertEquals(0, grants.size());
   }
}

