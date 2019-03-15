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
package com.iris.oauth.dao;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.UUID;

public interface OAuthDAO {

   void insertCode(String appId, String code, UUID person, Map<String,String> attrs);
   void removeCode(String appId, String code);
   void removeAccess(String appId, String access);
   void removeRefresh(String appId, String refresh);
   void updateTokens(String appId, String access, String refresh, UUID person);
   void updateAccessToken(String appId, String access, UUID person);
   void updateAttrs(String appId, UUID person, Map<String,String> attrs);
   Map<String,String> getAttrs(String appId, UUID person);
   void removePersonAndTokens(String appId, UUID person);
   Pair<UUID, Integer> getPersonWithCode(String appId, String code);
   Pair<UUID, Integer> getPersonWithAccess(String appId, String access);
   Pair<UUID, Integer> getPersonWithRefresh(String appId, String refresh);

}

