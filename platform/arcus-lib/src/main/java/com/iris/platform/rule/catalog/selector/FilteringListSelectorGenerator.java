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

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.iris.common.rule.RuleContext;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * Generates a {@link ListSelector} from a {@code List<Option>}, but first filters the latter based on whether {@link
 * Option#getMatch()} appears in the {@code Collection<?>} attribute resolved by {@code filterCollectionTemplate}.
 * 
 * @author Dan Ignat
 */
public class FilteringListSelectorGenerator implements SelectorGenerator
{
   private final List<Option> options;

   // TODO should this be a ModelQuery?
   protected Predicate<Model> matcher;

   private TemplatedValue<Object> filterCollectionTemplate;

   public FilteringListSelectorGenerator(List<Option> options)
   {
      this.options = options == null ? Collections.<Option>emptyList() : options;
   }

   @Override
   public boolean isSatisfiable(RuleContext context)
   {
      for (Model model : context.getModels())
      {
         if (matcher.apply(model))
         {
            return true;
         }
      }

      return false;
   }

   @Override
   public Selector generate(RuleContext context)
   {
      List<Option> options = null;

      for (Model model : context.getModels())
      {
         if (matcher.apply(model))
         {
            Map<String, Object> modelMap = model.toMap();

            Collection<?> filterCollection = (Collection<?>) filterCollectionTemplate.apply(modelMap);

            options = this.options.stream().filter(o -> filterCollection.contains(o.getMatch())).collect(toList());

            break;
         }
      }

      ListSelector selector = new ListSelector();

      if (options != null)
      {
         selector.setOptions(options);
      }

      return selector;
   }

   public Predicate<Model> getMatcher()
   {
      return matcher;
   }

   public void setMatcher(Predicate<Model> matcher)
   {
      this.matcher = matcher;
   }

   public TemplatedValue<Object> getFilterCollectionTemplate()
   {
      return filterCollectionTemplate;
   }

   public void setFilterCollectionTemplate(TemplatedValue<Object> filterCollectionTemplate)
   {
      this.filterCollectionTemplate = filterCollectionTemplate;
   }
}

