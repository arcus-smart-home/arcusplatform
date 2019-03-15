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
package com.iris.platform.scene;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.model.CompositeId;
import com.iris.messages.model.Model;
import com.iris.messages.model.PersistentModel;
import com.iris.messages.model.Place;

/**
 * 
 */
public interface SceneDao {

   List<SceneDefinition> listByPlace(UUID placeId);
   
   List<Model> listModelsByPlace(UUID placeId);
   
   @Nullable SceneDefinition findById(UUID placeId, Integer actionId);
   
   void save(SceneDefinition definition);
   
   PersistentModel save(PersistentModel model);
   
   boolean delete(UUID placeId, Integer actionId);

   // enables an optimized create flow
   default void create(Place place, SceneDefinition definition) {
      definition.setPlaceId(place.getId());
      definition.setSequenceId(-1);
      save(definition);
   }
   
   default List<SceneDefinition> listByPlace(@Nullable Place place) { 
      return place != null ? listByPlace(place.getId()) : Collections.emptyList();
   }
   
   /**
    * Determines the templates in use for the given placeId
    * @param placeId
    * @return
    */
   default Set<String> getTemplateIds(UUID placeId) {
      return listByPlace(placeId).stream().map(SceneDefinition::getTemplate).collect(Collectors.toSet());
   }
   
   default @Nullable SceneDefinition findById(CompositeId<UUID, Integer> id) {
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
   
   default boolean delete(Address address) {
      if(address == null) {
         return false;
      }
      UUID placeId = (UUID) address.getId();
      Integer actionId = ((PlatformServiceAddress) address).getContextQualifier();
      return delete(placeId, actionId);
   }
   
}

