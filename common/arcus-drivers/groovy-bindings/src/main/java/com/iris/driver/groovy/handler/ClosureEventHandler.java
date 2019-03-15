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
package com.iris.driver.groovy.handler;

import groovy.lang.Closure;

import java.io.Closeable;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.handler.ContextualEventHandler;

/**
 *
 */
public class ClosureEventHandler implements ContextualEventHandler<Object> {
   private final Closure<?> closure;

   public ClosureEventHandler(Closure<?> closure) {
      this.closure = closure;
   }

   /* (non-Javadoc)
    * @see com.iris.core.driver.handler.ContextualEventHandler#handleEvent(com.iris.core.driver.DeviceDriverContext, java.lang.Object)
    */
   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      GroovyContextObject.setContext(context);
      Closeable c = EnvironmentBinding.setRuntimeVar("message", event);
      try {
         Object o = closure.call(event);
         return !Boolean.FALSE.equals(o);
      }
      finally {
         c.close();
         GroovyContextObject.clearContext();
      }
   }

}

