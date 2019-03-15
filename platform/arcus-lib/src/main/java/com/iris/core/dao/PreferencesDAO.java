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

import java.util.Map;
import java.util.UUID;

public interface PreferencesDAO
{
   void save(UUID personId, UUID placeId, Map<String, Object> prefs);

   /**
    * Merges {@code prefs} into any existing prefs for the given {@code personId} and {@code placeId}.  It should
    * overwrite any existing prefs with matching keys, but should not affect any other prefs with different keys.
    */
   void merge(UUID personId, UUID placeId, Map<String, Object> prefs);

   Map<String, Object> findById(UUID personId, UUID placeId);

   void deleteForPerson(UUID personId);

   void delete(UUID personId, UUID placeId);

   void deletePref(UUID personId, UUID placeId, String prefKey);
}

