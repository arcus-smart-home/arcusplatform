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
package com.iris.common.rule.simple;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;
import com.iris.common.rule.RuleContext;

public abstract class BaseRuleContext implements RuleContext {

   private Map<String,Object>dirtyVariables=new HashMap<String,Object>();

   @Override
   public boolean isDirty() {
      return dirtyVariables.size()>0;
   }

   @Override
   public void clearDirty() {
      dirtyVariables=new HashMap<String,Object>();
   }

   @Override
   public Map<String,Object> getDirtyVariables() {
      return new HashMap<String, Object>(dirtyVariables);
   }

   protected void markDirtyVariable(String name, Object value) {
      Object oldValue = getVariables().get(name);
      if (!Objects.equal(value, oldValue)) {
         dirtyVariables.put(name, oldValue);
      }
   }
}

