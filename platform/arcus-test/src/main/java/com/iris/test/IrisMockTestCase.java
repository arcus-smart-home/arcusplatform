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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.Module;

/**
 *
 */
// consider folding this into IrisTestCase
public class IrisMockTestCase extends IrisTestCase {

   static {
      System.setProperty("redirect.base.url", "http://fakeurl");
      System.setProperty("device-ota-firmware.download.host", "http://fakeurl");
   }

   private MockModule mocks;
   
   protected void replay() {
      mocks.replay();
   }
   
   protected void reset() {
      mocks.reset();
   }
   
   protected void verify() {
      mocks.verify();
   }

   @Override
   protected Set<Module> modules() {
      Set<Module> modules = super.modules();
      mocks = new MockModule();
      for(Map.Entry<Key<?>, Class<?>> e: mockServices().entrySet()) {
         mocks.addMockService((Key) e.getKey(), (Class) e.getValue());
      }
      modules.add(mocks);
      return modules;
   }

   protected Map<Key<?>, Class<?>> mockServices() {
      Map<Key<?>, Class<?>> mockServices = new HashMap<Key<?>, Class<?>>();
      discoverMockServices(mockServices, this.getClass());
      return mockServices;
   }

   private void discoverMockServices(
         Map<Key<?>, Class<?>> mocks,
         Class<?> type
   ) {
      Mocks discovered = type.getAnnotation(Mocks.class);
      if(discovered != null) {
         for(Class<?> cls: discovered.value()) {
            mocks.put(Key.get(cls), cls);
         }
      }
      for(Class<?> iface: type.getInterfaces()) {
         discoverMockServices(mocks, iface);
      }
      Class<?> parent = type.getSuperclass();
      if(parent != null && !Object.class.equals(parent)) {
         discoverMockServices(mocks, parent);
      }
   }
   
}

