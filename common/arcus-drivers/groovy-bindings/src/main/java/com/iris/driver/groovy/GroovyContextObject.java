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
package com.iris.driver.groovy;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.service.executor.DriverExecutors;

/**
 * @deprecated Use {@link DriverExecutors} instead
 */
public abstract class GroovyContextObject {
   private static final ThreadLocal<DeviceDriverContext> ContextRef = new ThreadLocal<DeviceDriverContext>();

   public static DeviceDriverContext getContext() throws IllegalStateException {
      DeviceDriverContext context = ContextRef.get();
      if(context == null) {
         return new GroovyTypeCheckingDeviceDriverContext();
      }
      return context;
   }

   // TODO restrict access to these calls, currently public for test cases

   public static boolean isContextSet() {
      return ContextRef.get() != null;
   }

   public static void setContext(DeviceDriverContext context) {
      ContextRef.set(context);
   }

   public static void clearContext() {
      ContextRef.remove();
   }
}

