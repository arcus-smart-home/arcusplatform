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
package com.iris.oculus.modules.scene.ux;

import java.awt.Component;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComboBox;

import com.google.common.collect.ImmutableList;
import com.iris.client.bean.ActionSelector;
import com.iris.io.json.JSON;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.Fields.FieldBuilder;

public class ActionSelectorFactory {

   public static List<FieldWrapper<? extends Component, ? extends Object>> create(ActionSelector selector) {
      String type = selector.getType();
      if(ActionSelector.TYPE_GROUP.equals(type)) {
         return createGroupSelector(selector);
      }
      
      FieldBuilder<? extends Component, ? extends Object> builder = null;
      try {
         switch(type) {
         case ActionSelector.TYPE_BOOLEAN:
            builder = Fields.checkBoxBuilder();
            break;
         case ActionSelector.TYPE_LIST:
            builder = createListBuilder(selector);
            break;
         case ActionSelector.TYPE_PERCENT:
            builder = createPercentBuilder(selector);
            break;
         case ActionSelector.TYPE_DURATION:
            builder = createDurationBuilder(selector);
            break;
         case ActionSelector.TYPE_TEMPERATURE:   
         case ActionSelector.TYPE_RANGE:
             builder = createRangeBuilder(selector);
             break;  
         }
      }
      catch(Exception e) {
         Oculus.error("Invalid selector " + selector.getName(), e);
      }
      if(builder == null) {
         // TODO JSON builder
         builder = 
            Fields
               .builder(new TextField())
               .withGetter((c) -> JSON.fromJson(c.getText(), Object.class))
               .withSetter((c, o) -> c.setText(JSON.toJson(o)))
               ;
      }
      FieldWrapper<? extends Component, ? extends Object> field =
            builder
               .named(selector.getName())
               .labelled(selector.getName())
               .build();
      return ImmutableList.of(field);
   }

   private static FieldBuilder<? extends Component, ? extends Object> createPercentBuilder(ActionSelector selector) {
      return 
            Fields
               .intSpinnerBuilder()
               .withRange(0, 100)
               .withStepSize(10)
               ;
   }
   
   private static FieldBuilder<? extends Component, ? extends Object> createDurationBuilder(ActionSelector selector) {
      return 
            Fields
               .intSpinnerBuilder()
               .withRange(
            		 Optional.ofNullable(selector.getMin()).orElse(Integer.MIN_VALUE), 
            		 Optional.ofNullable(selector.getMax()).orElse(Integer.MAX_VALUE)
               )
	            /* 
			    * TMT - BY DEFAULT THE SPINNER USES AN INITIAL VALUE OF 0
			    * You must set the initial value of the spinner to be within the range selected
			    * Otherwise the spinner will be disabled. Hence the call to withValue
			    */	             
               
               .withValue(Optional.ofNullable(selector.getMin()).orElse(Integer.MIN_VALUE))
               .withStepSize(Optional.ofNullable(selector.getStep()).orElse(1000))
               ;
   }
   
   /*
    * Outside of the contextual meaning of range versus duration these two intSpinners seem redundant
    */
   private static FieldBuilder<? extends Component, ? extends Object> createRangeBuilder(ActionSelector selector) {
	      return 
	            Fields
	               .intSpinnerBuilder()
	               .withRange(
	            		   Optional.ofNullable(selector.getMin()).orElse(Integer.MIN_VALUE), 
	            		   Optional.ofNullable(selector.getMax()).orElse(Integer.MAX_VALUE)
	               )
	            /* 
			    * TMT - BY DEFAULT THE SPINNER USES AN INITIAL VALUE OF 0
			    * You must set the initial value of the spinner to be within the range selected
			    * Otherwise the spinner will be disabled. Hence the call to withValue
			    */	   
	           .withValue(Optional.ofNullable(selector.getMin()).orElse(Integer.MIN_VALUE))
               .withStepSize(Optional.ofNullable(selector.getStep()).orElse(1))
	               ;
   }   
   
   private static FieldBuilder<? extends Component, ? extends Object> createListBuilder(ActionSelector selector) {
      return
         Fields
            .comboBoxBuilder()
            .noteditable()
            .withValues((Collection<Object>) selector.getValue())
            .withRenderer((o) -> {
               if(o == null) {
                  return "[Not Satisfiable]";
               }
               else {
                  return (String) ((List<Object>) o).get(0);
               }
            })
            .withGetter((combo) -> {
               Object o = combo.getSelectedItem();
               if(o == null) {
                  return null;
               }
               return ((List<Object>) o).get(1);
            })
            .withSetter((combo, value) -> {
               if(value == null) {
                  combo.setSelectedIndex(-1);
                  return;
               }
               int size = combo.getModel().getSize();
               for(int i=0; i<size; i++) {
                  Object o = combo.getModel().getElementAt(i);
                  Object v = (((List<Object>) o).get(1));
                  if(Objects.equals(value, v)) {
                     combo.setSelectedIndex(i);
                     return;
                  }
               }
               combo.setSelectedIndex(-1);
            });
   }

   private static List<FieldWrapper<? extends Component, ? extends Object>> createGroupSelector(ActionSelector selector) {
      List<FieldWrapper<? extends Component, ? extends Object>> allFields = new ArrayList<>();
      allFields.add(null);
      
      Collection<List<Object>> values = (Collection<List<Object>>) selector.getValue();
      Map<String, List<FieldWrapper<?, ?>>> subvalues = new HashMap<>();
      for(List<Object> tuple: values) {
         List<Map<String, Object>> subselectors = (List<Map<String, Object>>) tuple.get(1);
         if(subselectors != null && !subselectors.isEmpty()) {
            List<FieldWrapper<?, ?>> subfields = new ArrayList<>();
            for(Map<String, Object> subselector: subselectors) {
               List<FieldWrapper<?, ?>> component = create(new ActionSelector(subselector));
               subfields.addAll(component);
               allFields.addAll(component);
            }
            subvalues.put((String) tuple.get(0), subfields);
         }
      }
      
      allFields.set(0, createGroupSelector(selector, subvalues));
      
      return allFields;
   }
   
   private static FieldWrapper<? extends Component, ? extends Object> createGroupSelector(ActionSelector selector, Map<String, List<FieldWrapper<?, ?>>> subvalues) {
      FieldWrapper<JComboBox<Object>, Object> field =
         Fields
            .comboBoxBuilder()
            .noteditable()
            .withValues((Collection<Object>) selector.getValue())
            .withRenderer((o) -> {
               if(o == null) {
                  return "[Not Satisfiable]";
               }
               else {
                  return (String) ((List<Object>) o).get(0);
               }
            })
            .withGetter((combo) -> {
               Object o = combo.getSelectedItem();
               if(o == null) {
                  return null;
               }
               return ((List<Object>) o).get(0);
            })
            .withSetter((combo, value) -> {
               enable(value, subvalues);
               if(value == null) {
                  combo.setSelectedIndex(-1);
                  return;
               }
               int size = combo.getModel().getSize();
               for(int i=0; i<size; i++) {
                  Object o = combo.getModel().getElementAt(i);
                  Object v = (((List<Object>) o).get(0));
                  if(Objects.equals(value, v)) {
                     combo.setSelectedIndex(i);
                     return;
                  }
               }
               combo.setSelectedIndex(-1);
            })
            .named(selector.getName())
            .labelled(selector.getName())
            .build();
      field.getComponent().addItemListener((e) -> {
         if(e.getStateChange() == ItemEvent.SELECTED) {
            String value = ((List<String>) e.getItem()).get(0);
            enable(value, subvalues);
         }
         else {
            enable(null, subvalues);
         }
      });
      return field;
   }

   private static void enable(Object value, Map<String, List<FieldWrapper<?, ?>>> subvalues) {
      for(String key: subvalues.keySet()) {
         if(key.equals(value)) {
            enableAll(subvalues.get(key));
         }
         else {
            disableAll(subvalues.get(key));
         }
      }
   }

   private static void enableAll(List<FieldWrapper<?, ?>> list) {
      for(FieldWrapper<?, ?> wrapper: list) {
         wrapper.setEnabled(true);
      }
   }

   private static void disableAll(List<FieldWrapper<?, ?>> list) {
      for(FieldWrapper<?, ?> wrapper: list) {
         wrapper.setEnabled(false);
      }
   }

}

