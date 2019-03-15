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
package com.iris.platform.rule.catalog.template;

import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

/**
 * 
 */
@SuppressWarnings("serial")
public class TransformTemplate<I, O> implements TemplatedValue<O> {
   private final Function<I, O> transform;
   private final TemplatedValue<I> delegate;
   
   public TransformTemplate(Function<I, O> transform, TemplatedValue<I> delegate) {
      this.transform = transform;
      this.delegate = delegate;
   }

   @Override
   public boolean isResolveable(Map<String, Object> variables) {
      return delegate.isResolveable(variables);
   }

   @Override
   public boolean hasContextVariables(Set<String> contextVariables) {
      return delegate.hasContextVariables(contextVariables);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.template.TemplatedValue#apply(java.util.Map)
    */
   @Override
   public O apply(Map<String, Object> variables) {
      I input = delegate.apply(variables);
      return transform.apply(input);
   }

   @Override
   public String toString() {
      return transform + " (" + delegate + ")";
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
      result = prime * result
            + ((transform == null) ? 0 : transform.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      TransformTemplate other = (TransformTemplate) obj;
      if (delegate == null) {
         if (other.delegate != null) return false;
      }
      else if (!delegate.equals(other.delegate)) return false;
      if (transform == null) {
         if (other.transform != null) return false;
      }
      else if (!transform.equals(other.transform)) return false;
      return true;
   }

}

