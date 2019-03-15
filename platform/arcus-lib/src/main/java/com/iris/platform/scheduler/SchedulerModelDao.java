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
package com.iris.platform.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.address.Address;
import com.iris.platform.model.ModelEntity;

public interface SchedulerModelDao {

   /**
    * Gets all the Schedulers associated with the given
    * place.
    * @param placeId
    * @param includeWeekdays
    * @return
    */
   List<ModelEntity> listByPlace(UUID placeId, boolean includeWeekdays);
   
   /**
    * Gets the Scheduler associated with the address.
    * This may be either the Scheduler's address or
    * the address of the target object.
    * @param address
    * @return
    */
   @Nullable
   ModelEntity findByAddress(Address address);
   
   ModelEntity findOrCreateByTarget(UUID placeId, Address target);
   
   /**
    * Saves the given Scheduler.
    * @param entity
    * @return
    * @throws IllegalArgumentException If entity is not a Scheduler
    */
   ModelEntity save(ModelEntity entity) throws IllegalArgumentException;
   
   /**
    * Deletes the given Scheduler.  Address may be the Scheduler address,
    * the address of the target object, or the place address.
    * @param address
    * @return
    */
   void deleteByAddress(Address address);
   
   /**
    * Deletes all Schedulers associated with the
    * given place.
    * @param placeId
    */
   void deleteByPlace(UUID placeId);
   
   void delete(ModelEntity model);
   
   /**
    * Updates the given attributes, this call will never create
    * a new row.
    * @param address
    * @param attributes
    * @return The new last modified date for the object.
    */
   void updateAttributes(Address address, Map<String, Object> attributes);

   void removeAttributes(Address address, Set<String> attributes);
   
}

