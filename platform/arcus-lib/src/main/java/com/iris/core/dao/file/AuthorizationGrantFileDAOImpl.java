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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
public class AuthorizationGrantFileDAOImpl implements AuthorizationGrantDAO {

   private static final String RESOURCE = "authorizationgrantdata.json";

   private static final Logger logger = LoggerFactory.getLogger(AuthorizationGrantFileDAOImpl.class);

   private final Map<UUID, Map<UUID, AuthorizationGrant>> grants = new HashMap<>();

   public AuthorizationGrantFileDAOImpl() {
      List<AuthorizationGrant> grants = Collections.<AuthorizationGrant>emptyList();
      try {
         grants = loadData();
      } catch(IOException ioe) {
         logger.warn("Error reading canned data: ", ioe);
      }

      grants.forEach((g) -> { save(g); });
   }

   @Override
   public void save(AuthorizationGrant grant) {
      Map<UUID, AuthorizationGrant> placeGrants = grants.get(grant.getEntityId());
      if(placeGrants == null) {
         placeGrants = new HashMap<UUID, AuthorizationGrant>();
         grants.put(grant.getEntityId(), placeGrants);
      }
      placeGrants.put(grant.getPlaceId(), grant);
   }

   @Override
   public List<AuthorizationGrant> findForEntity(UUID entityId) {
      Map<UUID, AuthorizationGrant> placeGrants = grants.get(entityId);
      return placeGrants == null
            ? Collections.<AuthorizationGrant>emptyList()
            : placeGrants.values().stream().collect(Collectors.toList());
   }

   @Override
   public List<AuthorizationGrant> findForPlace(UUID placeId) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void removeForPlace(UUID placeId) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void removeGrantsForEntity(UUID entityId) {
      grants.remove(entityId);
   }

   @Override
   public void removeGrant(UUID entityId, UUID placeId) {
      Map<UUID, AuthorizationGrant> placeGrants = grants.get(entityId);
      if(placeGrants != null) {
         placeGrants.remove(placeId);
      }
   }

   private List<AuthorizationGrant> loadData() throws IOException {
      InputStream is = null;
      Reader reader = null;

      try {
         is = this.getClass().getClassLoader().getResourceAsStream(RESOURCE);
         if(is != null) {
            reader = new InputStreamReader(is, "UTF-8");
            return Arrays.asList(createGson().fromJson(reader, AuthorizationGrant[].class));
         }

         return Collections.<AuthorizationGrant>emptyList();

      } finally {
         Closeables.close(reader, true);
         Closeables.close(is, true);
      }
   }

   private Gson createGson() {
      return new GsonBuilder().create();
   }
}

