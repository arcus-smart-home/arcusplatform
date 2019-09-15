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

import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import groovy.lang.Script;

import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;

/**
 * The meta-class for the outer-script execution.
 */
// TODO collapse with DriverScriptMetaClass
public class CapabilityScriptMetaClass extends MetaClassImpl {

   public CapabilityScriptMetaClass(Class cls) {
      super(cls, new MetaMethod [] { });
   }

   @Override
   public Object invokeMethod(Class sender, Object object, String methodName, Object[] originalArguments, boolean isCallToSuper, boolean fromInsideClass) {
      return getBinding(object).invokeMethod(methodName, originalArguments);
   }

   @Override
   public Object invokeMethod(Object object, String methodName, Object[] arguments) {
      return getBinding(object).invokeMethod(methodName, arguments);
   }

   private static CapabilityEnvironmentBinding getBinding(Object instance) {
      Script s = (Script) instance;
      return (CapabilityEnvironmentBinding) s.getBinding();
   }
   
}

