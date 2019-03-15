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
package com.iris.oculus.modules.scene.ux;

import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JLabel;

import com.iris.client.bean.Action;
import com.iris.client.bean.ActionTemplate;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.FormView;

/**
 * 
 */
public class ActionsField extends FormView implements FieldWrapper<Component, List<Map<String, Object>>> {
   private JLabel label = new JLabel("Actions");
   private Map<String, ActionTemplate> templates;
   
   
   /**
    * 
    */
   public ActionsField(String name, List<ActionTemplate> templates) {
      this.templates = templates.stream().collect(Collectors.toMap(ActionTemplate::getId, Function.identity()));
      this.getComponent().setName(name);
   }

   @Override
   public Component getLabel() {
      return label;
   }

   @Override
   public void setValue(List<Map<String, Object>> value) {
      this.clearFields();
      if(value == null) {
         return;
      }
      
      value
         .stream()
         .map(Action::new)
         .forEach((action) -> {
            ActionTemplate template = templates.get(action.getTemplate());
            // TODO if null add a JSON field? 
            if(template != null) {
               addAction(template, action);
            }
         });
   }

   public void addAction(ActionTemplate template) {
      ActionField field = ActionField.create(template);
      addAction(template, field);
   }

   public void addAction(ActionTemplate template, Action action) {
      ActionField field = ActionField.edit(template, action);
      addAction(template, field);
   }

   @Override
   public List<Map<String, Object>> getValue() {
      return 
            getValues()
               .values()
               .stream()
               .filter((o) -> o != null)
               .map((o) -> ((Action) o).toMap())
               .collect(Collectors.toList());
               
   }

   private void addAction(ActionTemplate template, ActionField field) {
      field.addSeparator();
      addField(field, LabelLocation.TOP);
      field.addRemoveListener((f) -> this.removeField(f));
   }

}

