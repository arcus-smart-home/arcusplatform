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
package com.iris.driver.groovy.context;

import groovy.lang.Closure;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;

public class ConnectedClosure extends Closure<Object> {

   public ConnectedClosure(Object owner) {
      super(owner);
   }

   protected void doCall() {
      DeviceDriverContext context = GroovyContextObject.getContext();
      context.setConnected();
   }
}

