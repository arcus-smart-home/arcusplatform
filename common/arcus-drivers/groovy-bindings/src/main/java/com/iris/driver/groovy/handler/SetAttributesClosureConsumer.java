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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.handler.SetAttributesConsumer;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

/**
 *
 */
public class SetAttributesClosureConsumer implements SetAttributesConsumer {
   private final String namespace;
   private final Closure<?> closure;
   private final Exception declSite;

   /**
    * Constructor for setAttributes for any namespace (within a driver).
    * @param closure
    */
   public SetAttributesClosureConsumer(Closure<?> closure) {
      this(null, closure);
   }

   /**
    * Constructor for setAttributes within the context of a single namespace
    * (within a driver or a capability).
    * @param namespace
    * @param closure
    */
   public SetAttributesClosureConsumer(String namespace, Closure<?> closure) {
      this.namespace = namespace;
      this.closure = closure;
      this.declSite = new Exception();
   }

   public Exception getDeclarationSite() {
      return declSite;
   }

   @Override
   public String getNamespace() {
      return namespace;
   }

   @Override
   public Set<String> setAttributes(DeviceDriverContext context, Map<String, Object> attributes) {
      GroovyContextObject.setContext(context);
      Closeable c = EnvironmentBinding.setRuntimeVar("message", MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes));
      try {
         Object o = closure.call(attributes);
         if(o == null) {
            return attributes.keySet();
         }
         if(o instanceof Set) {
            return (Set<String>) o;
         }
         if(o instanceof Collection) {
            return new HashSet<>((Collection<String>) o);
         }
         GroovyContextObject.getContext().getLogger().warn("Invalid return type from setAttributes [{}]", o);
         return attributes.keySet();
      } catch (Throwable th) {
         GroovyContextObject.getContext().getLogger().warn("Exception in driver [{}]", th);
         throw th;
      } finally {
         try {
            c.close();
         }
         catch (IOException e) {
            // ignore
         }
         GroovyContextObject.clearContext();
      }
   }

}

