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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.model.CompositeId;
import com.iris.messages.model.Place;

/**
 * 
 */
public interface ActionDao {

   List<ActionDefinition> listByPlace(UUID placeId);
   
   Date updateLastExecutionTime(UUID placeId, int actionId);
   
   @Nullable ActionDefinition findById(UUID placeId, Integer actionId);
   
   void save(ActionDefinition definition);
   
   boolean delete(UUID placeId, Integer actionId);

   // enables an optimized create flow
   default void create(Place place, ActionDefinition definition) {
      definition.setPlaceId(place.getId());
      definition.setSequenceId(-1);
      save(definition);
   }
   
   default List<ActionDefinition> listByPlace(Place place) { 
      return place != null ? listByPlace(place.getId()) : Collections.emptyList();
   }
   
   default void updateLastExecutionTime(ActionDefinition action) {
      if(action != null) {
         Date lastExecuted = updateLastExecutionTime(action.getPlaceId(), action.getSequenceId());
         action.setLastExecuted(lastExecuted);
      }
   }
   
   default @Nullable ActionDefinition findById(CompositeId<UUID, Integer> id) {
      if(id == null) {
         return null;
      }
      return findById(id.getPrimaryId(), id.getSecondaryId());
   }
   
   default boolean delete(CompositeId<UUID, Integer> id) {
      if(id == null) {
         return false;
      }
      return delete(id.getPrimaryId(), id.getSecondaryId());
   }
   
}

