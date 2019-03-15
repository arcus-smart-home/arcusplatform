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
package com.iris.oculus.util;

import com.google.common.base.Optional;
import com.iris.client.capability.Capability;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;

/**
 * 
 */
public class DefaultSelectionModel<M> implements SelectionModel<M> {
   private Optional<M> selected;
   private ListenerRegistration modelListener = null;
   private ListenerList<Optional<M>> listeners = new ListenerList<>();
   
   public DefaultSelectionModel() {
      selected = Optional.absent();
   }
   
   public DefaultSelectionModel(M model) {
      selected = Optional.of(model);
   }
   
   /* (non-Javadoc)
    * @see com.iris.oculus.util.SelectionModel#hasSelection()
    */
   @Override
   public boolean hasSelection() {
      return selected.isPresent();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.util.SelectionModel#setSelection(java.lang.Object)
    */
   @Override
   public void setSelection(M item) {
      if(modelListener != null) {
         modelListener.remove();
         modelListener = null;
      }
      this.selected = Optional.fromNullable(item);
      if(item != null && item instanceof Model) {
         modelListener = ((Model) item).addPropertyChangeListener((event) -> {
            if(Capability.EVENT_DELETED.equals(event.getPropertyName())) {
               clearSelection();
            }
         });
      }
      fireSelectionChanged();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.util.SelectionModel#clearSelection()
    */
   @Override
   public void clearSelection() {
      this.selected = Optional.absent();
      if(modelListener != null) {
         modelListener.remove();
         modelListener = null;
      }
      fireSelectionChanged();
   }

   @Override
   public Optional<M> getSelectedItem() {
      return selected;
   }
   
   @Override
   public ListenerRegistration addSelectionListener(Listener<Optional<M>> listener) {
      return listeners.addListener(listener);
   }

   public ListenerRegistration addNullableSelectionListener(Listener<? super M> listener) {
      return listeners.addListener((reference) -> {;
         if(reference.isPresent()) {
            listener.onEvent(reference.get());
         }
         else {
            listener.onEvent(null);
         }
      });
   }

   protected void fireSelectionChanged() {
      listeners.fireEvent(selected);
   }
   
}

