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
package com.iris.platform.rule.catalog.selector;

import java.util.Collections;
import java.util.List;

import com.iris.common.rule.RuleContext;

public class ConstantListSelectorGenerator implements SelectorGenerator {
   private final List<Option> options;

   public ConstantListSelectorGenerator(List<Option> options) {
      this.options = options == null ? Collections.<Option>emptyList() : options;
   }

   @Override
   public boolean isSatisfiable(RuleContext context) {
      return true;
   }

   @Override
   public Selector generate(RuleContext context) {
      ListSelector selector = new ListSelector();
      selector.setOptions(options);
      return selector;
   }


}

