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
package com.iris.platform.rule.catalog.selector;

import com.google.common.base.Preconditions;

/**
 * 
 */
public class SimpleSelector implements Selector {
   private final SelectorType type;

   /**
    * 
    */
   public SimpleSelector(SelectorType type) {
      Preconditions.checkArgument(type != null, "must specify a type");
      Preconditions.checkArgument(type != SelectorType.LIST, "must use a ListSelector for list type");
      this.type = type;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.selector.Selector#getType()
    */
   @Override
   public SelectorType getType() {
      return type;
   }

}

