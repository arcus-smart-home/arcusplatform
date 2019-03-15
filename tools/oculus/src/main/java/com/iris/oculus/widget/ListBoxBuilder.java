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
import java.util.Collection;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import com.iris.client.model.Model;
import com.iris.client.model.Store;
import com.iris.oculus.view.StoreViewModel;
import com.iris.oculus.view.ViewListModel;

/**
 *
 */
public class ListBoxBuilder<T> {

   public static <M extends Model> ListBoxBuilder<M> fromStore(Store<M> store) {
      ViewListModel<M> m = new ViewListModel<>();
      m.bind(new StoreViewModel<>(store));
      return
            new ListBoxBuilder<M>()
               .withModel(m)
               ;
   }
   
   private ListModel<T> model = new DefaultListModel<>();
   private int selectionMode = -1;
   private ListCellRenderer<? super T> renderer;
   
   public ListBoxBuilder<T> withModel(ListModel<T> model) {
      this.model = model;
      return this;
   }
   
   public ListBoxBuilder<T> withValues(Collection<T> values) {
      DefaultListModel<T> m = new DefaultListModel<>();
      for(T value: values) {
         m.addElement(value);
      }
      return withModel(m);
   }
   
   public ListBoxBuilder<T> withRenderer(ListCellRenderer<? super T> renderer) {
      this.renderer = renderer;
      return this;
   }
   
   public ListBoxBuilder<T> withRenderer(Function<T, String> renderer) {
      return withRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(
               JList<?> list, Object value, int index, boolean isSelected,
               boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) c).setText(renderer.apply((T) value));
            return c;
         }
      });
   }
   
   public ListBoxBuilder<T> withRenderer(Function<T, String> renderer, String emptyValue) {
      return withRenderer((value) -> value == null ? emptyValue : renderer.apply(value));
   }
   
   public ListBoxBuilder<T> singleSelectionMode() {
      this.selectionMode = ListSelectionModel.SINGLE_SELECTION;
      return this;
   }
   
   public ListBoxBuilder<T> singleIntervalSelectionMode() {
      this.selectionMode = ListSelectionModel.SINGLE_INTERVAL_SELECTION;
      return this;
   }
   
   public ListBoxBuilder<T> multipleIntervalSelectionMode() {
      this.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
      return this;
   }
   
   public JList<T> create() {
      JList<T> list = new JList<T>(model);
      if(renderer != null) {
         list.setCellRenderer(renderer);
      }
      if(selectionMode > -1) {
         list.setSelectionMode(selectionMode);
      }
      return list;
   }

}

