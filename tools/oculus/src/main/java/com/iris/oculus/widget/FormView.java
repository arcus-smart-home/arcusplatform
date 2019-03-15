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
package com.iris.oculus.widget;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.iris.oculus.util.ComponentWrapper;

/**
 * 
 */
public class FormView implements ComponentWrapper<Component> {
   private JPanel panel;
   private JPanel buttons;

   private Set<FieldWrapper<? extends Component, ?>> fields = 
         new HashSet<>();
   
   private GridBagConstraints labelConstraints;
   private GridBagConstraints fieldConstraints;
   private GridBagConstraints wideLabelConstraints;
   private GridBagConstraints wideFieldConstraints;
   private GridBagConstraints componentConstraints;
   
   public FormView() {
      panel = new JPanel(new GridBagLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      
      buttons = new JPanel();
      buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
      buttons.add(Box.createHorizontalGlue());
      
      init();
   }
   
   private void init() {
      labelConstraints = new GridBagConstraints();
      labelConstraints.gridx = 0;
      labelConstraints.gridy = 0;
      labelConstraints.weightx = 0.0;
      labelConstraints.weighty = 0.0;
      labelConstraints.fill = GridBagConstraints.NONE;
      labelConstraints.anchor = GridBagConstraints.EAST;

      fieldConstraints = new GridBagConstraints();
      fieldConstraints.gridx = 1;
      fieldConstraints.gridy = 0;
      fieldConstraints.weightx = 1.0;
      fieldConstraints.weighty = 0.0;
      fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
      
      wideLabelConstraints = new GridBagConstraints();
      wideLabelConstraints.gridx = 0;
      wideLabelConstraints.gridy = 0;
      wideLabelConstraints.gridwidth = 2;
      wideLabelConstraints.weightx = 1.0;
      wideLabelConstraints.weighty = 0.0;
      wideLabelConstraints.fill = GridBagConstraints.HORIZONTAL;

      wideFieldConstraints = new GridBagConstraints();
      wideFieldConstraints.gridx = 0;
      wideFieldConstraints.gridy = 0;
      wideFieldConstraints.gridwidth = 2;
      wideFieldConstraints.weightx = 1.0;
      wideFieldConstraints.weighty = 1.0;
      wideFieldConstraints.fill = GridBagConstraints.BOTH;
      
      componentConstraints = (GridBagConstraints) wideFieldConstraints.clone();
      
      GridBagConstraints buttonConstraints = (GridBagConstraints) wideLabelConstraints.clone();
      buttonConstraints.gridy = 1000;
      
      panel.add(buttons, buttonConstraints);
   }
   
   public FormView addField(FieldWrapper<? extends Component, ?> field) {
      return addField(field, field.getLabel() == null ? LabelLocation.NONE : LabelLocation.LEFT);
   }
   
   public FormView addField(Component label, Component field) {
      return addField(label, field, label == null ? LabelLocation.NONE : LabelLocation.LEFT);
   }
   
   public FormView addField(FieldWrapper<? extends Component, ?> field, LabelLocation location) {
      fields.add(field);
      return addField(field.getLabel(), field.getComponent(), location);
   }
   public FormView addField(Component label, Component field, LabelLocation location) {
      if(label == null && location != LabelLocation.NONE) {
         throw new IllegalArgumentException("Must specify a label or set LabelLocation to HIDDEN");
      }
      switch(location) {
      case LEFT:
         panel.add(label, labelConstraints.clone());
         panel.add(field, fieldConstraints.clone());
         newLine();
         break;
      case RIGHT:
         panel.add(field, labelConstraints.clone());
         panel.add(label, fieldConstraints.clone());
         newLine();
         break;
      case TOP:
         panel.add(label, wideLabelConstraints.clone());
         newLine();
         panel.add(field, wideFieldConstraints.clone());
         newLine();
         break;
      case NONE:
         panel.add(field, wideFieldConstraints.clone());
         newLine();
         break;
      default:
         throw new IllegalArgumentException("Unrecognized LabelLocation " + location);
      }
      repaint();
      return this;
   }
   
   public void removeField(FieldWrapper<?, ?> field) {
      if(fields.remove(field)) {
         if(field.getLabel() != null) {
            this.panel.remove(field.getLabel());
         }
         this.panel.remove(field.getComponent());
         repaint();
      }
   }
   
   public void clearFields() {
      panel.removeAll();
      init();
   }
   
   public FormView addComponent(Component c) {
      panel.add(c, componentConstraints.clone());
      newLine();
      return this;
   }
   
   public FormView addSeparator() {
      panel.add(new JSeparator(), wideFieldConstraints.clone());
      newLine();
      return this;
   }
   
   public FormView addButton(Action action) {
      buttons.add(new JButton(action));
      return this;
   }
   
   public FormView addButton(JButton button) {
      buttons.add(button);
      return this;
   }
   
   public FormView addButton(Component c) {
      buttons.add(c);
      return this;
   }
   
   private void newLine() {
      labelConstraints.gridy++;
      fieldConstraints.gridy++;
      wideLabelConstraints.gridy++;
      wideFieldConstraints.gridy++;
      componentConstraints.gridy++;
   }

   @Override
   public Component getComponent() {
      return panel;
   }

   public Map<String, Object> getValues() {
      Map<String, Object> values = new HashMap<>(fields.size() + 1);
      for(FieldWrapper<? extends Component, ?> field: fields) {
         if(field.isEnabled()) {
            values.put(field.getName(), field.getValue());
         }
      }
      return values;
   }
   
   public void setValues(Map<String, ? extends Object> values) {
      if(values == null) {
         return;
      }
      for(FieldWrapper field: fields) {
         String name = field.getName();
         if(name == null) {
            continue;
         }
         Object value = values.get(name);
         field.setValue(value);
      }
   }
   
   public boolean setValue(String name, Object value) {
      if(name == null) {
         return false;
      }
      for(FieldWrapper field: fields) {
         if(name.equals(field.getName())) {
            field.setValue(value);
            return true;
         }
      }
      return false;
   }
 
   private void repaint() {
      this.panel.invalidate();
      Container parent = this.panel.getParent();
      if(parent != null) {
         parent.validate();
         parent.repaint();
      }
   }
   
   public enum LabelLocation {
      LEFT,
      RIGHT,
      TOP,
      NONE;
   }
}

