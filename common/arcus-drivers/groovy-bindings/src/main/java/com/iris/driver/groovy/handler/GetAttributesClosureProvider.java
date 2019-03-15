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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.handler.GetAttributesProvider;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;

/**
 *
 */
public class GetAttributesClosureProvider implements GetAttributesProvider {
   private final String namespace;
   private final Closure<?> closure;

   /**
    * Constructor for getAttributes at the driver level (all namespaces)
    * @param closure
    */
   public GetAttributesClosureProvider(Closure<?> closure) {
      this(null, closure);
   }

   /**
    * Constructor for getAttributes within the context of a single namespace.
    * @param namespace
    * @param closure
    */
   public GetAttributesClosureProvider(String namespace, Closure<?> closure) {
      this.namespace = namespace;
      this.closure = closure;
   }

   @Override
   public String getNamespace() {
      return namespace;
   }

   @Override
   public Map<String, Object> getAttributes(DeviceDriverContext context, Set<String> names) {
      GroovyContextObject.setContext(context);
      Closeable c = EnvironmentBinding.setRuntimeVar("message", MessageBody.buildMessage(Capability.CMD_GET_ATTRIBUTES, Collections.<String, Object>singletonMap("names", names)));
      try {
         Object o = closure.call(names);
         if(o instanceof Map) {
            return (Map<String, Object>) o;
         }
         if(o instanceof AttributeMap) {
            return ((AttributeMap) o).toMap();
         }
         GroovyContextObject.getContext().getLogger().warn("Invalid return type from getAttributes [{}]", o);
         return Collections.emptyMap();
      }
      finally {
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

