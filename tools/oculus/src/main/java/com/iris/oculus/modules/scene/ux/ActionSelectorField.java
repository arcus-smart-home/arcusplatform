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

import javax.swing.JCheckBox;

import com.iris.bootstrap.ServiceLocator;
import com.iris.client.bean.ActionSelector;
import com.iris.oculus.modules.models.ModelController;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.FormView;

/**
 * 
 */
public class ActionSelectorField extends FormView implements FieldWrapper<Component, Map<String, Object>> {
   private JCheckBox label;
   
   /**
    * 
    */
   public ActionSelectorField(String address, List<ActionSelector> selectors) {
      label = new JCheckBox(address);
      label.setSelected(true);
      label.addActionListener((e) -> onChecked());
      for(ActionSelector selector: selectors) {
         for(FieldWrapper<?, ?> wrapper: ActionSelectorFactory.create(selector)) {
            addField(wrapper);
         }
      }
      getComponent().setName(address);
      ServiceLocator
         .getInstance(ModelController.class)
         .getName(address)
         .onSuccess((name) -> label.setText(name));
   }
   
   public boolean isEnabled() {
      return label.isSelected();
   }
   
   public void setEnabled(boolean enabled) {
      label.setSelected(enabled);
   }

   @Override
   public JCheckBox getLabel() {
      return label;
   }

   @Override
   public Map<String, Object> getValue() {
      return getValues();
   }

   @Override
   public void setValue(Map<String, Object> value) {
      setValues(value);
   }

   protected void onChecked() {
      boolean enabled = label.isSelected();
      // TODO properly disable children
      getComponent().setEnabled(enabled);
   }
}

