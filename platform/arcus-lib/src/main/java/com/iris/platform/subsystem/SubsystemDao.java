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
package com.iris.platform.subsystem;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.address.Address;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.platform.model.ModelEntity;

/**
 * DAO for working with subsystems.  A place may have
 * 0 or 1 subsystems of each type associated with it. An
 * instance of a subsystem is always associated with a place.
 * When creating a new subsystem or replacing all the attributes
 * use the {@link #save(SubsystemModel)} method.  To do partial
 * updates use {@link #updateAttributes(Address, Map)} and {@link #removeAttributes(Address, Set)}.
 */
public interface SubsystemDao {

   List<ModelEntity> listByPlace(UUID placeId);
   
   @Nullable ModelEntity findByAddress(Address address);
   
   Date save(ModelEntity model);
   Date save(ModelEntity model, Map<String,Object> failedFromPrevious);

   ModelEntity copyAndSave(ModelEntity model);
   ModelEntity copyAndSave(ModelEntity model, Map<String,Object> failedFromPrevious);
   
   void deleteByAddress(Address address);
   
   void deleteByPlace(UUID placeId);
   
   default @Nullable ModelEntity findByAddress(String address) {
      return findByAddress(Address.fromString(address));
   }
   
   default @Nullable ModelEntity findByPlaceAndNamespace(UUID placeId, String namespace) {
      // TODO finalize this address format
      return findByAddress(Address.platformService(placeId, namespace));
   }
   
   default void deleteByAddress(String address) {
      deleteByAddress(Address.fromString(address));
   }
   
   default void deleteByPlaceAndNamespace(UUID placeId, String namespace) {
      deleteByAddress(Address.platformService(placeId, namespace));
   }
   
}

