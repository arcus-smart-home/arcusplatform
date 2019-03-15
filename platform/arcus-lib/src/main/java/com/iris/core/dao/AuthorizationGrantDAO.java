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
package com.iris.core.dao;

import java.util.List;
import java.util.UUID;

import com.iris.security.authz.AuthorizationGrant;

public interface AuthorizationGrantDAO {

   /**
    * This will replace any existing grants with the ones defined within this object.  We could be
    * more sophisticated and might in the future, but this is not likely to be a frequently written
    * table.
    *
    * @param grant
    */
   void save(AuthorizationGrant grant);
   List<AuthorizationGrant> findForEntity(UUID entityId);
   List<AuthorizationGrant> findForPlace(UUID placeId);
   void removeGrant(UUID entityId, UUID placeId);
   void removeGrantsForEntity(UUID entityId);
   void removeForPlace(UUID placeId);
}

