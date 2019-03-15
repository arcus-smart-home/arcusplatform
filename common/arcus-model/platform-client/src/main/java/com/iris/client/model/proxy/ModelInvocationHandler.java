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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 
 */
class ModelInvocationHandler implements InvocationHandler {
   private final DelegateProxyModel delegate;
   private final Map<Method, ModelInvocationFunction> methods;
   
   ModelInvocationHandler(DelegateProxyModel delegate, Map<Method, ModelInvocationFunction> methods) {
      this.delegate = delegate;
      this.methods = methods;
   }
   
   DelegateProxyModel getDelegate() {
      return delegate;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      ModelInvocationFunction fn = methods.get(method);
      if(fn == null) {
         throw new NoSuchMethodException("Unrecognized method: " + method);
      }
      return fn.invoke(delegate, method, args);
   }

   public void update(Map<String, Object> attributes) {
      delegate.update(attributes);
   }

}

