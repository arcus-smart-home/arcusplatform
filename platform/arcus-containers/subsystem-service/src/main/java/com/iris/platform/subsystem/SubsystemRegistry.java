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

import java.util.Optional;
import java.util.UUID;

import com.iris.common.subsystem.SubsystemExecutor;

/**
 * 
 */
public interface SubsystemRegistry {

   Optional<SubsystemExecutor> loadByPlace(UUID placeId);
   
   Optional<SubsystemExecutor> loadByPlace(UUID placeId, UUID accountId);
   
   void removeByPlace(UUID placeId);
   
   void clear();
}

