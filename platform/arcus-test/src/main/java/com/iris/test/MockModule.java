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
package com.iris.test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.MockType;

import com.google.inject.AbstractModule;
import com.google.inject.Key;

/**
 *
 */
// TODO move to arcus-test
public class MockModule extends AbstractModule {
   private Map<Key<Object>, Object> services = new LinkedHashMap<>();
   
   public MockModule() {
      
   }
   
   public MockModule(Class<?>... services) {
      for(Class<?> service: services) {
         addMockService(service);
      }
   }
   
   public MockModule(Set<Class<?>> services) {
      addMockServices(services);
   }
   
   public MockModule addMockServices(Set<Class<?>> services) {
      for(Class<?> service: services) {
         addMockService(service);
      }
      return this;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public MockModule addMockService(Class<?> service) {
      // compiler isn't smart enough to recognize that the same object has to have the same type
      return addMockService((Class)service, (Class)service);
   }
   
   public <T> MockModule addMockService(Class<T> type, Class<? extends T> value) {
      return addMockService(Key.get(type), value);
   }

   @SuppressWarnings("unchecked")
   public <T> MockModule addMockService(Key<T> key, Class<? extends T> value) {
      this.services.put((Key<Object>) key, EasyMock.createMock(MockType.DEFAULT, value));
      return this;
   }

   @Override
   protected void configure() {
      for(Map.Entry<Key<Object>, Object> entry: services.entrySet()) {
         bind(entry.getKey()).toInstance(entry.getValue());
      }
   }
   
   public <T> T get(Class<T> type) {
      return get(Key.get(type));
   }

   public <T> T get(Key<T> type) {
      return (T) services.get(type);
   }

   public void reset() {
      EasyMock.reset(services.values().toArray());
   }
   
   public void replay() {
      EasyMock.replay(services.values().toArray());
   }
   
   public void verify() {
      EasyMock.verify(services.values().toArray());
   }

}

