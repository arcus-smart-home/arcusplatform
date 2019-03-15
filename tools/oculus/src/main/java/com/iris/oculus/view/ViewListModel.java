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
package com.iris.oculus.view;

import javax.swing.AbstractListModel;

import com.iris.client.event.ListenerRegistration;
import com.iris.oculus.view.ViewModelEvent.ViewModelAddedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelChangedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelRemovedEvent;

/**
 *
 */
public class ViewListModel<T> extends AbstractListModel<T> {
   private ViewModel<T> model;
   private ListenerRegistration registration;
   
   public ViewListModel() {
      
   }
   
   public void bind(ViewModel<T> model) {
      if(this.registration != null) {
         this.registration.remove();
         this.model = null;
      }
      if(model != null) {
         this.model = model;
         this.registration = this.model.addViewListener((event) -> onViewEvent(event));
      }
   }
   
   public void unbind() {
      bind(null);
   }

   @Override
   public int getSize() {
      return model != null ? model.size() : 0;
   }

   @Override
   public T getElementAt(int index) {
      return model != null ? model.get(index) : null;
   }
   
   protected void onViewEvent(ViewModelEvent event) {
      if(event instanceof ViewModelChangedEvent) {
         fireContentsChanged(ViewListModel.this, 0, getSize());
      }
      else if(event instanceof ViewModelAddedEvent) {
         fireIntervalAdded(ViewListModel.this, event.getStart(), event.getEnd());
      }
      else if(event instanceof ViewModelChangedEvent) {
         fireContentsChanged(ViewListModel.this, event.getStart(), event.getEnd());
      }
      else if(event instanceof ViewModelRemovedEvent) {
         fireIntervalRemoved(ViewListModel.this, event.getStart(), event.getEnd());
      }
      
   }
}

