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
package com.iris.oculus.modules.capability.ux;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class MultiTableSelectionListener implements ListSelectionListener {
   private Set<ListSelectionModel> delegates = new LinkedHashSet<>();
   
   public void add(ListSelectionModel m) {
      delegates.add(m);
      m.addListSelectionListener(this);
   }
   
   public void remove(ListSelectionModel m) {
      m.removeListSelectionListener(this);
      delegates.remove(m);
   }

   @Override
   public void valueChanged(ListSelectionEvent e) {
      if(e.getValueIsAdjusting()) {
         return;
      }
      
      ListSelectionModel m = (ListSelectionModel) e.getSource();
      if(!m.isSelectionEmpty()) {
         for(ListSelectionModel delegate: delegates) {
            if(delegate == m) {
               continue;
            }
            delegate.clearSelection();
         }
      }
   }
}

