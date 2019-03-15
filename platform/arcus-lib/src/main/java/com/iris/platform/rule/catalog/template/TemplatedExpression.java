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
package com.iris.platform.rule.catalog.template;

import java.util.Map;

import com.google.common.base.Preconditions;

public class TemplatedExpression {
   private String expression;
   private volatile TemplatedValue<Object> template;

   public TemplatedExpression() {
      
   }
   
   public TemplatedExpression(String expression) {
      this.expression = expression;
   }
   
   /**
    * @return the expression
    */
   public String getExpression() {
      return expression;
   }
   
   /**
    * @param expression the expression to set
    */
   public void setExpression(String expression) {
      this.expression = expression;
      this.template = null;
   }
   
   public TemplatedExpression bind(Map<String, Object> values) {
      String boundExpression = TemplatedValue.text(expression).apply(values);
      return new TemplatedExpression(boundExpression);
   }
   
   /**
    * @return the template
    */
   public TemplatedValue<Object> toTemplate() {
      Preconditions.checkArgument(this.expression != null, "must set an expression first");
      if(this.template == null) {
         this.template = TemplatedValue.parse(expression); 
      }
      return this.template;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((expression == null) ? 0 : expression.hashCode());
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
      TemplatedExpression other = (TemplatedExpression) obj;
      if (expression == null) {
         if (other.expression != null) return false;
      }
      else if (!expression.equals(other.expression)) return false;
      return true;
   }

   public String toString() {
      return TemplatedExpression.class.getSimpleName() + " [" + expression + "]";
   }
}

