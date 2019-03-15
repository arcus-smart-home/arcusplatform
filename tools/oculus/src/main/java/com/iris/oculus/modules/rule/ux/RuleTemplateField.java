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

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.iris.client.model.RuleTemplateModel;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.widget.FieldWrapper;

/**
 * 
 */
public class RuleTemplateField extends BaseComponentWrapper<JTextArea> implements FieldWrapper<JTextArea, String> {
   private JLabel label = new JLabel("Template");
   private String value;
   
   @Override
   public String getName() {
      return RuleTemplateModel.ATTR_TEMPLATE;
   }
   
   @Override
   protected JTextArea createComponent() {
      return new JTextArea(value);
   }

   @Override
   public JLabel getLabel() {
      return label;
   }

   @Override
   public String getValue() {
      // TODO Auto-generated method stub
      return value;
   }

   @Override
   public void setValue(String value) {
      this.value = value;
      if(this.isActive()) {
         this.getComponent().setText(value);
      }
      
   }

}

