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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.client.bean.Action;
import com.iris.client.bean.ActionSelector;
import com.iris.client.bean.ActionTemplate;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.Toast;

/**
 * 
 */
public class ActionField extends FormView implements FieldWrapper<Component, Action> {
   private static final Logger logger = LoggerFactory.getLogger(ActionField.class);
   
   public static ActionField create(ActionTemplate template) {
      return new ActionField(template);
   }
   
   public static ActionField edit(ActionTemplate template, Action value) {
      ActionField form = create(template);
      form.setValue(value);
      return form;
   }

   private ListenerList<ActionField> removeListener = new ListenerList<>();
   private JPanel label;
   private ActionTemplate template;
   private List<ActionSelectorField> fields = new ArrayList<ActionSelectorField>();
   private javax.swing.Action remove = Actions.build("Remove", this, ActionField::remove);
   
   public ActionField(ActionTemplate template) {
      this.template = template;
      this.label = new JPanel();
      this.label.setLayout(new BoxLayout(this.label, BoxLayout.X_AXIS));
      this.label.add(new JLabel(template.getName()+"(premium:"+template.getPremium()+")"));
      this.label.add(Box.createGlue());
      this.label.add(new JButton(remove));
      // TODO use typehint to load icon
      for(Map.Entry<String, List<Map<String, Object>>> e: template.getSelectors().entrySet()) {
         List<ActionSelector> selectors = e.getValue().stream().map(ActionSelector::new).collect(Collectors.toList());
         ActionSelectorField field = new ActionSelectorField(e.getKey(), selectors);
         fields.add(field);
         this.addField(field, LabelLocation.TOP);
      }
   }
   
   /* (non-Javadoc)
    * @see com.iris.oculus.widget.FieldWrapper#getName()
    */
   @Override
   public String getName() {
      return template.getId();
   }

   @Override
   public JPanel getLabel() {
      return label;
   }
   
   @Override
   public Action getValue() {
      Map<String, Map<String, Object>> context = new HashMap<String, Map<String,Object>>();
      for(ActionSelectorField field: fields) {
         if(field.isEnabled()) {
            context.put(field.getName(), field.getValue());
         }
      }
      
      Action action = new Action();
      action.setName(template.getName());
      action.setTemplate(template.getId());
      action.setContext(context);
      action.setPremium(template.getPremium());
      return action;
   }
   
   @Override
   public void setValue(Action action) {
      if(!action.getTemplate().equals(template.getId())) {
         // TODO throw an error if the templates don't match up
      }
      Map<String, Map<String, Object>> context = action.getContext();
      if(context == null) {
         context = ImmutableMap.of();
      }
      for(ActionSelectorField field: fields) {
         Map<String, Object> subvalue = action.getContext().get(field.getName());
         if(subvalue == null) {
            field.setEnabled(false);
         }
         else {
            field.setEnabled(true);
            try {
               field.setValue(subvalue);
            }
            catch(Exception e) {
               // TODO show this as a field level error
               logger.warn("Invalid value [" + subvalue + "] for field [" + field.getName() + "]", e);
               Oculus.warn("Invalid value [" + subvalue + "] for field [" + field.getName() + "]", e);
               // can't do this because its not showing yet
//               Toast.showToast(field.getComponent(), "Error setting value:" + e.getMessage());
            }
         }
      }
   }
   
   public ListenerRegistration addRemoveListener(Listener<? super ActionField> l) {
      return removeListener.addListener(l);
   }
 
   public void remove() {
      removeListener.fireEvent(this);
   }
}

