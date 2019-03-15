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
package com.iris.client.model.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.client.IrisClient;
import com.iris.client.capability.Capability;
import com.iris.client.model.Model;
import com.iris.client.model.ModelFactory;

/**
 * 
 */
public class ProxyModelFactory implements ModelFactory {
   private final Map<Method, ModelInvocationFunction> defaultFunctions =
         new HashMap<Method, ModelInvocationFunction>();
   private final Map<String, Map<Method, ModelInvocationFunction>> handlers =
         Collections.synchronizedMap(new HashMap<String, Map<Method, ModelInvocationFunction>>());
   private final IrisClient client;

   public ProxyModelFactory() {
      this(null);
   }
   
   public ProxyModelFactory(IrisClient client)  {
      this.client = client;
      this.defaultFunctions.putAll(getFunctionsFor(Object.class));
      this.defaultFunctions.putAll(getFunctionsFor(Model.class));
      this.defaultFunctions.putAll(getFunctionsFor(Capability.class));
   }
   
   @Override
   public Model create(
         Map<String, Object> attributes,
         Collection<Class<? extends Capability>> types
   ) {
      Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>(types.size() + 2);
      interfaces.add(Model.class);
      interfaces.add(Capability.class);
      
      Map<Method, ModelInvocationFunction> handlers = new HashMap<Method, ModelInvocationFunction>(defaultFunctions);
      for(Class<? extends Capability> type: types) {
         interfaces.add(type);
         handlers.putAll(getFunctionsFor(type));
      }
      
      DelegateProxyModel model = new DelegateProxyModel(attributes, client);
      return (Model) Proxy.newProxyInstance(
            Capability.class.getClassLoader(), 
            interfaces.toArray(new Class<?>[interfaces.size()]), 
            new ModelInvocationHandler(model, handlers)
      );
   }

   @Override
   public <M extends Model> M create(Map<String, Object> attributes, Class<M> model) {
      return (M) create(attributes, Collections.<Class<? extends Capability>>singletonList(model));
   }

   @Override
   public <M extends Model> M create(
         Map<String, Object> attributes, 
         Class<M> model,
         Collection<Class<? extends Capability>> types
   ) {
      List<Class<? extends Capability>> caps = new ArrayList<Class<? extends Capability>>(types.size() + 1);
      caps.add(model);
      caps.addAll(types);
      return (M) create(attributes, caps);
   }

   private Map<Method, ModelInvocationFunction> getFunctionsFor(Class<?> type) {
      Map<Method, ModelInvocationFunction> methods = handlers.get(type.getName());
      if(methods == null) {
         synchronized(handlers) {
            methods = handlers.get(type.getName());
            if(methods == null) {
               methods = buildMethods(type);
               handlers.put(type.getName(), methods);
            }
         }
      }
      return methods;
   }

   private Map<Method, ModelInvocationFunction> buildMethods(Class<?> type) {
      Method [] declared = type.getDeclaredMethods();
      Map<Method, ModelInvocationFunction> methods = 
            new HashMap<Method, ModelInvocationFunction>(declared.length + 1);
      for(Method m: type.getDeclaredMethods()) {
         methods.put(m, ModelInvocationFunctions.wrap(m));
      }
      for(Class<?> parent: type.getInterfaces()) {
         if(Model.class.equals(parent) || Capability.class.equals(parent) || Object.class.equals(parent)) {
            continue;
         }
         methods.putAll(getFunctionsFor(parent));
      }
      return methods;
   }

}

