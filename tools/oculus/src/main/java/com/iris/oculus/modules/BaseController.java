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
package com.iris.oculus.modules;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.Action;

import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.client.model.Store;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;

public abstract class BaseController<M extends Model> extends SessionAwareController {
   private Action reloadAction = Actions.build("Refresh", () -> reload());
   private DefaultSelectionModel<M> selection = new DefaultSelectionModel<>();
   private Store<M> store;
   private String namespace;

   public BaseController(Class<M> type) {
      this.store = IrisClientFactory.getStore(type);
      try {
         this.namespace = (String) type.getField("NAMESPACE").get(null);
      }
      catch(Exception e) {
         throw new IllegalArgumentException("type " + type + " is not a proper model class");
      }
   }
   
   protected abstract ClientFuture<? extends Collection<Map<String, Object>>> doLoad();
   
   public Store<M> getStore() {
      return store;
   }
   
   public SelectionModel<M> getSelection() {
      return selection;
   }
   
   public ListenerRegistration addSelectedListener(Listener<? super M> l) {
      return selection.addNullableSelectionListener(l);
   }

   public Action actionReload() {
      return reloadAction;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void reload() {
      doLoad()
         .onFailure((e) -> Oculus.warn("Error loading models of type [" + namespace + "]", e))
         .onSuccess((models) -> store.replace((List) IrisClientFactory.getModelCache().retainAll(namespace, models)))
         ;
   }
   
   @Override
   protected void onPlaceChanged(String newPlaceId) {
      store.clear();
      selection.clearSelection();
   }

   
}

