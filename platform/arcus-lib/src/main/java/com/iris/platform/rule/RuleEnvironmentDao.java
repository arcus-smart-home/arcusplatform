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
/**
 * 
 */
package com.iris.platform.rule;

import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.iris.messages.model.Place;

/**
 * Allows all the objects associated with the rule system to be loaded.
 */
// TODO add callbacks for when things are updated
public interface RuleEnvironmentDao {

   // TODO this will be replaced with streamByPartition once partitions are supported
   Stream<RuleEnvironment> streamAll();
   
   RuleEnvironment findByPlace(UUID placeId);
   
   void deleteByPlace(UUID placeId);
   
   default RuleEnvironment findByPlace(Place place) {
      Preconditions.checkNotNull(place, "place may not be null");
      return findByPlace(place.getId());
   }
   
   default void deleteByPlace(Place place) {
      Preconditions.checkNotNull(place, "place may not be null");
      
      deleteByPlace(place.getId());
   }
}

