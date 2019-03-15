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
package com.iris.reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

/**
 * 
 */
public class MethodDiscoverer {
   private final Predicate<Method> predicate;
   
   /**
    * 
    */
   MethodDiscoverer(Predicate<Method> predicate) {
      this.predicate = predicate;
   }

   public List<Method> discover(Class<?> type) {
      Preconditions.checkNotNull(type);
      final List<Method> methods = new ArrayList<Method>();
      visit(type, new Predicate<Method>() {
         @Override
         public boolean apply(Method input) {
            methods.add(input);
            return true;
         }
      });
      return methods;
   }
   
   /**
    * The visitor may return false to stop visiting methods.
    * @param type
    * @param visitor
    */
   public void visit(Class<?> type, Predicate<Method> visitor) {
      Preconditions.checkNotNull(type, "type may not be null");
      Preconditions.checkNotNull(visitor, "visitor may not be null");

      for(Method m: type.getMethods()) {
         if(predicate.apply(m)) {
            if(!visitor.apply(m)) {
               return;
            }
         }
      }
   }
   
}

