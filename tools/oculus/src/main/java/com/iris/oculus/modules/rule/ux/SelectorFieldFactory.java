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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.iris.client.capability.WeeklySchedule;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.Fields.FieldBuilder;
import com.iris.util.IrisCollections;

/**
 * 
 */
public class SelectorFieldFactory {
   private static final String TYPE_LIST = "LIST";
   private static final String TYPE_TIME_OF_DAY = "TIME_OF_DAY";
   private static final String TYPE_DAY_OF_WEEK = "DAY_OF_WEEK";
   private static final String TYPE_DURATION = "DURATION";
   private static final String TYPE_TIME_RANGE = "TIME_RANGE";
   
   private static final Set<String> TIME_RANGES = IrisCollections.setOf(
      "0:00:00 - 4:00:00", "4:00:00 - 8:00:00", "8:00:00 - 12:00:00", "12:00:00 - 16:00:00", "16:00:00 - 20:00:00", "20:00:00 - 24:00:00"
   );


   public static FieldWrapper<? extends Component, ?> create(String name, String label, Map<String, Object> selector) {
      return
            build((String) selector.get("type"), selector)
               .named(name)
               .labelled(label)
               .build();
      
   }
   
   private static FieldBuilder<? extends Component, ?> build(String type, Map<String, Object> selector) {
      if(TYPE_LIST.equals(type)) {
         return
               Fields
                  .comboBoxBuilder()
                  .noteditable()
                  .withRenderer((o) -> {
                     if(o == null) {
                        return "[Not Satisfiable]";
                     }
                     else {
                        return (String) ((List<Object>) o).get(0);
                     }
                  })
                  .withValues(((Collection<Object>) selector.get("options")))
                  .withGetter((combo) -> {
                     Object o = combo.getSelectedItem();
                     if(o == null) {
                        return null;
                     }
                     return String.valueOf(((List<Object>) o).get(1));
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
      else if(TYPE_DAY_OF_WEEK.equals(type)) {
         return
               Fields
                  .listBuilder()
                  .withValues(Arrays.asList(
                  		WeeklySchedule.ATTR_MON, 
                  		WeeklySchedule.ATTR_TUE, 
                  		WeeklySchedule.ATTR_WED,
                  		WeeklySchedule.ATTR_THU,
                  		WeeklySchedule.ATTR_FRI,
                  		WeeklySchedule.ATTR_SAT,
                  		WeeklySchedule.ATTR_SUN
            		))
                  ;
                  
      }
      else if (TYPE_TIME_RANGE.equals(type)) {
         // TODO: Let the user really define a range instead of picking one.
         return Fields
                  .comboBoxBuilder()
                  .noteditable()
                  .withValues((Set)TIME_RANGES);
      }
      else if(TYPE_DURATION.equals(type)) {
         return
               Fields
                  .intSpinnerBuilder()
                  ;
      }
      else {
         return Fields
                  .textFieldBuilder()
                  .withToolTip("Unrecognized type, enter JSON to encode")
                  ;
      }
   }
}

