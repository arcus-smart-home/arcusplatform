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
 * 
 */
public class DelegateModel implements Model {
   
   private final Model delegate;
   /**
    * 
    */
   public DelegateModel(Model delegate) {
      this.delegate = delegate;
   }
   
   protected Model delegate() {
      return delegate;
   }
   
   /**
    * @return
    * @see com.iris.messages.model.Model#getId()
    */
   public String getId() {
      return delegate.getId();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#getAddress()
    */
   public Address getAddress() {
      return delegate.getAddress();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#getType()
    */
   public String getType() {
      return delegate.getType();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#getCapabilities()
    */
   public Set<String> getCapabilities() {
      return delegate.getCapabilities();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#getInstances()
    */
   public Map<String, Set<String>> getInstances() {
      return delegate.getInstances();
   }
   /**
    * @param capabilityNamespace
    * @return
    * @see com.iris.messages.model.Model#supports(java.lang.String)
    */
   public boolean supports(String capabilityNamespace) {
      return delegate.supports(capabilityNamespace);
   }
   /**
    * @param instanceId
    * @param capabilityNamespace
    * @return
    * @see com.iris.messages.model.Model#hasInstanceOf(java.lang.String, java.lang.String)
    */
   public boolean hasInstanceOf(String instanceId, String capabilityNamespace) {
      return delegate.hasInstanceOf(instanceId, capabilityNamespace);
   }
   /**
    * @param name
    * @return
    * @see com.iris.messages.model.Model#getAttribute(java.lang.String)
    */
   public Object getAttribute(String name) {
      return delegate.getAttribute(name);
   }
   
   @Override
   public <T> Optional<T> getAttribute(TypeMarker<T> type, String name) {
      return delegate.getAttribute(type, name);
   }

   @Override
   public <T> T getAttribute(TypeMarker<T> type, String name, T defaultValue) {
      return delegate.getAttribute(type, name, defaultValue);
   }

   /**
    * @param name
    * @param value
    * @return
    * @see com.iris.messages.model.Model#setAttribute(java.lang.String, java.lang.Object)
    */
   public Object setAttribute(String name, Object value) {
      return delegate.setAttribute(name, value);
   }
   
   /**
    * @param attributes
    * @see com.iris.messages.model.Model#update(java.util.Map)
    */
   public void update(Map<String, Object> attributes) {
      delegate.update(attributes);
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#toMap()
    */
   public Map<String, Object> toMap() {
      return delegate.toMap();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#keys()
    */
   public Iterable<String> keys() {
      return delegate.keys();
   }
   /**
    * @return
    * @see com.iris.messages.model.Model#values()
    */
   public Iterable<Object> values() {
      return delegate.values();
   }

}

