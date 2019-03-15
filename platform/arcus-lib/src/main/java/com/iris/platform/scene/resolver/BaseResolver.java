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
package com.iris.platform.scene.resolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.iris.common.rule.action.ActionContext;
import com.iris.messages.model.Model;
import com.iris.messages.type.ActionSelector;
import com.iris.messages.type.ActionTemplate;

public abstract class BaseResolver implements ActionResolver {
   private final String id;
   private final String name;
   private final String typeHint;
   
   public BaseResolver(String id, String name, String typeHint) {
      this.id = id;
      this.name = name;
      this.typeHint = typeHint;
   }

   @Override
   public String getId() {
      return id;
   }
   
   
   public String getName() {
      return name;
   }

   /**
    * @return the typeHint
    */
   public String getTypeHint() {
      return typeHint;
   }

   @Override
   public ActionTemplate resolve(ActionContext context) {
      
      Map<String, List<Map<String, Object>>> selectors = new HashMap<String, List<Map<String,Object>>>();
      for(Model model: context.getModels()) {
         List<ActionSelector> selector = resolve(context, model);
         if(!selector.isEmpty()) {
            selectors.put(
                  model.getAddress().getRepresentation(), 
                  selector.stream().map(ActionSelector::toMap).collect(Collectors.toList())
            );
         }
      }
      
      ActionTemplate template = new ActionTemplate();
      template.setId(id);
      template.setName(name);
      template.setTypehint(typeHint);
      template.setSatisfiable(!selectors.isEmpty());
      template.setSelectors(selectors);
      return template;
   }

   protected abstract List<ActionSelector> resolve(ActionContext context, Model model);

}

