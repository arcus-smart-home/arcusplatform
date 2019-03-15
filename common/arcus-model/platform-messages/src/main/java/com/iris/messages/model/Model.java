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
package com.iris.messages.model;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Optional;
import com.iris.messages.address.Address;
import com.iris.util.TypeMarker;

/**
 * An in-memory representation of a capability object.
 */
// TODO rename to Attributable?
public interface Model {

   String getId();
   
   Address getAddress();
   
   String getType();
   
   Set<String> getCapabilities();
   
   Map<String, Set<String>> getInstances();
   
   boolean supports(String capabilityNamespace);
   
   boolean hasInstanceOf(String instanceId, String capabilityNamespace);
   
   Object getAttribute(String name);
   
   <T> Optional<T> getAttribute(TypeMarker<T> type, String name);
   
   <T> T getAttribute(TypeMarker<T> type, String name, T defaultValue);
   
   Object setAttribute(String name, Object value);
   
   void update(Map<String, Object> attributes);
   
   Map<String, Object> toMap();
   
   Iterable<String> keys();
   
   Iterable<Object> values();
   
}

