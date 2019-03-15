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
package com.iris.common.subsystem.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.reflection.MethodInvokerFactory.ArgumentResolverFactory;

/**
 * 
 */
public class SubsystemResolverFactory<R> implements ArgumentResolverFactory<SubsystemEventAndContext, R> {
   private static final Function<Object, Object> NULL = Functions.constant(null);
   static final Function<SubsystemEventAndContext, Object> GetContext = new Function<SubsystemEventAndContext, Object>() {
      @Override
      public Object apply(SubsystemEventAndContext input) {
         return input.getContext();
      }
   };
   
   @Override
   public Function<? super SubsystemEventAndContext, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
      if(parameter instanceof Class<?>) {
         Class<?> type = (Class<?>) parameter;
         if(type.isAssignableFrom(SubsystemContext.class)) {
            // TODO stricter type checking?
            return GetContext;
         }
      }
      throw new IllegalArgumentException("Unresolvable parameter type [" + parameter + "] for method [" + method + "] on class [" + method.getDeclaringClass() + "]");
   }

   @SuppressWarnings("unchecked")
   @Override
   public Function<Object, R> getResolverForReturnType(Method method) {
      if(Void.TYPE.equals(method.getReturnType())) {
         return (Function<Object, R>) NULL;
      }
      throw new IllegalArgumentException("Invalid return type for method [" + method + "], only void is allowed for event handlers");
   }


}

