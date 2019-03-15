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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Function;

/**
 * A {@link Function} wrapper for a {@link Method} where
 * the input is used as the {@code this} pointer.
 */
public class MethodInvoker<I, R> implements Function<I, R> {
   private final Method method;
   private final Function<? super I, Object> thisProvider;
   private final Function<? super I, Object []> argumentProvider;
   private final Function<Object, R> returnTypeTransform;
   private final Function<Throwable, R> exceptionHandler;

   MethodInvoker(
         Method method,
         Function<? super I, Object> thisProvider,
         Function<? super I, Object []> argumentProvider,
         Function<Object, R> returnTypeTransform,
         Function<Throwable, R> exceptionHandler
   ) {
      this.method = method;
      if(!method.isAccessible()) {
         method.setAccessible(true);
      }
      this.thisProvider = thisProvider;
      this.returnTypeTransform = returnTypeTransform;
      this.argumentProvider = argumentProvider;
      this.exceptionHandler = exceptionHandler;
   }

   @Override
   public R apply(I input) {
      try {
         Object ths = thisProvider.apply(input);
         Object [] arguments = argumentProvider.apply(input);
         Object value = method.invoke(ths, arguments);
         return returnTypeTransform.apply(value);
      }
      catch(InvocationTargetException e) {
         return exceptionHandler.apply(e.getCause());
      }
      catch(Throwable cause) {
         return exceptionHandler.apply(cause);
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "MethodInvoker " + thisProvider + "." + method.getName() + "( " + argumentProvider + "): " + returnTypeTransform;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((argumentProvider == null) ? 0 : argumentProvider.hashCode());
      result = prime * result
            + ((exceptionHandler == null) ? 0 : exceptionHandler.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
      result = prime
            * result
            + ((returnTypeTransform == null) ? 0 : returnTypeTransform
                  .hashCode());
      result = prime * result
            + ((thisProvider == null) ? 0 : thisProvider.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MethodInvoker other = (MethodInvoker) obj;
      if (argumentProvider == null) {
         if (other.argumentProvider != null) return false;
      }
      else if (!argumentProvider.equals(other.argumentProvider)) return false;
      if (exceptionHandler == null) {
         if (other.exceptionHandler != null) return false;
      }
      else if (!exceptionHandler.equals(other.exceptionHandler)) return false;
      if (method == null) {
         if (other.method != null) return false;
      }
      else if (!method.equals(other.method)) return false;
      if (returnTypeTransform == null) {
         if (other.returnTypeTransform != null) return false;
      }
      else if (!returnTypeTransform.equals(other.returnTypeTransform)) return false;
      if (thisProvider == null) {
         if (other.thisProvider != null) return false;
      }
      else if (!thisProvider.equals(other.thisProvider)) return false;
      return true;
   }

}

