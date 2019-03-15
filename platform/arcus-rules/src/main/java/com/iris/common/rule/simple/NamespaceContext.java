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
package com.iris.common.rule.simple;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.ActionContext;

/**
 * Allows a set of variables to override the values from
 * the delegate context
 */
public class NamespaceContext extends DelegatingContext implements RuleContext {
   private final static String NAMESPACE_DELIMITER = ":";
   private final String namespace;
   private final String namespaceToken;

   public NamespaceContext(String namespace, RuleContext delegate) { 
      super(delegate);
      Preconditions.checkNotNull(delegate, "delegate may not be null");
      Preconditions.checkNotNull(namespace, "namespace may not be null");
      this.delegate = delegate;
      this.namespace=namespace;
      namespaceToken=NAMESPACE_DELIMITER+namespace;
   }
   

   
   private String transformVariableName(String name){
      if(namespace!=null){
         name=name+NAMESPACE_DELIMITER+namespace;
      }
      return name;
   } 

   @Override
   public Map<String, Object> getVariables() {
      Map<String, Object> variables = new HashMap<String, Object>();
      for(Map.Entry<String, Object>entry:delegate.getVariables().entrySet()){
         String key = entry.getKey();
         if(key.endsWith(namespace)) {
            key = entry.getKey().substring(0, key.length() - namespace.length() - 1);
            variables.put(key, entry.getValue());
         }
         else if(!key.contains(NAMESPACE_DELIMITER)) {
            variables.put(key, entry.getValue());
         }
      }
      return variables;
   }

   @Override
   public String toString() {
      return "NamespaceContext [namespace=" + namespace + ", namespaceToken=" + namespaceToken + "]";
   }



   @Override
   public Object getVariable(String name) {
      String overideName=transformVariableName(name);
      Object value = delegate.getVariable(overideName);
      if(value==null){
         return delegate.getVariable(name);
      }
      return value;
   }

   @Override
   public <T> T getVariable(String name, Class<T> type) {
      Preconditions.checkNotNull(name, "name may not be null");
      Preconditions.checkNotNull(type, "type may not be null");
      String overideName=transformVariableName(name);
      T value = delegate.getVariable(overideName, type);
      if(value==null){
         return delegate.getVariable(name,type);
      }
      return value;
   }

   @Override
   public Object setVariable(String name, Object value) {
      name=transformVariableName(name);
      return delegate.setVariable(name, value);
   }



   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
      result = prime * result + ((namespaceToken == null) ? 0 : namespaceToken.hashCode());
      return result;
   }



   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      NamespaceContext other = (NamespaceContext) obj;
      if (namespace == null) {
         if (other.namespace != null)
            return false;
      }else if (!namespace.equals(other.namespace))
         return false;
      if (namespaceToken == null) {
         if (other.namespaceToken != null)
            return false;
      }else if (!namespaceToken.equals(other.namespaceToken))
         return false;
      return true;
   }

   @Override
   public RuleContext override(String namespace) {
      return new NamespaceContext(namespace, this);
   }
   
   @Override
   public ActionContext override(Map<String, Object> variables) {
      return new NamespaceContext(namespace, this);
   }

}

