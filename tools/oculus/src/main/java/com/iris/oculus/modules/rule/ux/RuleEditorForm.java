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
package com.iris.oculus.modules.rule.ux;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;

import com.iris.client.capability.Rule.UpdateContextRequest;
import com.iris.client.capability.RuleTemplate.CreateRuleRequest;
import com.iris.client.model.RuleModel;
import com.iris.client.model.RuleTemplateModel;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.FormView;

/**
 * 
 */
public class RuleEditorForm extends FormView {
   
   public static RuleEditorForm create(RuleTemplateModel template, Map<String, Map<String, Object>> selectors, boolean showTemplateId) {
      RuleEditorForm form = new RuleEditorForm();
      if (!showTemplateId) {
      	form.addComponent(new JLabel("Template"));
      }
      else {
         form.addField(
               Fields
                  .textFieldBuilder()
                  .labelled("Template")
                  .named(UpdateContextRequest.ATTR_TEMPLATE)
                  .build()
         );
      }
      form.addComponent(form.template.getComponent());
      form.addField(
            Fields
               .textFieldBuilder()
               .labelled("Name")
               .named(CreateRuleRequest.ATTR_NAME)
               .build()
      );
      // TODO this should be a sub-form
      for(Map.Entry<String, Map<String, Object>> entry: selectors.entrySet()) {
         FieldWrapper<? extends Component, ?> c = 
               SelectorFieldFactory.create("var:" + entry.getKey(), entry.getKey(), entry.getValue());
         form.addField(c);
      }
      form.template.setValue(template.getTemplate());
      return form;
   }
   
   public static RuleEditorForm edit(RuleTemplateModel template, RuleModel rule, Map<String, Map<String, Object>> selectors) {
      RuleEditorForm form = create(template, selectors, true);
      if(rule != null) {
         Map<String, Object> values = new HashMap<String, Object>();
         values.put(UpdateContextRequest.ATTR_TEMPLATE, rule.getTemplate());
         values.put(CreateRuleRequest.ATTR_NAME, rule.getName());
         if(rule.getContext() != null) {
            for(String key: rule.getContext().keySet()) {
               values.put("var:" + key, rule.getContext().get(key));
            }
         }
         form.setValues(values);
      }
      return form;
   }
   
   RuleTemplateField template;
   private RuleEditorForm() {
      template = new RuleTemplateField();
   }
}

