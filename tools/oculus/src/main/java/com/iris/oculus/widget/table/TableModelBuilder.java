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
package com.iris.oculus.widget.table;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.iris.client.model.Model;
import com.iris.client.model.Store;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.StoreViewModel;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.widget.table.ColumnModel.SetValue;
import com.iris.oculus.widget.table.ColumnModel.TableCellEditor;
import com.iris.oculus.widget.table.ColumnModel.TableCellRenderer;
import com.iris.oculus.widget.table.ColumnModel.ValueRenderer;

/**
 * @author tweidlin
 *
 */
public class TableModelBuilder<T> {
   public static <T> TableModelBuilder<T> builder() {
      return new TableModelBuilder<T>(new SimpleViewModel<>());
   }
   
   public static <T> TableModelBuilder<T> builder(Collection<T> models) {
      return new TableModelBuilder<T>(new SimpleViewModel<>(models));
   }
   
   public static <M extends Model> TableModelBuilder<M> builder(Store<M> store) {
      StoreViewModel<M> view = new StoreViewModel<>();
      view.bind(store);
      return new TableModelBuilder<M>(view);
   }
   
   public static <T> TableModelBuilder<T> builder(ViewModel<T> view) {
      return new TableModelBuilder<T>(view);
   }
   
   private List<ColumnModel<T>> columns = new ArrayList<ColumnModel<T>>();
   private ViewModel<T> model;
   
   private TableModelBuilder(ViewModel<T> model) {
      this.model = model;
   }
   
   public ColumnBuilder columnBuilder() {
      return new ColumnBuilder();
   }
   
   public TableModel<T> build() {
      return new TableModel<>(new ArrayList<>(columns), model);
   }
   
   public class ColumnBuilder {
      private String name;
      private Function<T, Object> get;
      private SetValue<T> set;
      private Function<T, Boolean> editable;
      
      private TableCellRenderer renderer;
      private TableCellEditor editor;
      
      public ColumnBuilder withName(String name) {
         this.name = name;
         return this;
      }
      
      public ColumnBuilder withGetter(Function<T, Object> get) {
         this.get = get;
         return this;
      }
      
      public ColumnBuilder withSetter(SetValue<T> set) {
         this.set = set;
         return this;
      }
      
      public ColumnBuilder isEditable(Function<T, Boolean> editable) {
         this.editable = editable;
         return this;
      }
      
      public ColumnBuilder withRenderer(ValueRenderer<Component, Object> renderer) {
         return withRenderer((c, v, hasFocus, isSelected) -> renderer.render(c, v));
      }
      
      public ColumnBuilder withRenderer(TableCellRenderer renderer) {
         this.renderer = renderer;
         return this;
      }
      
      public ColumnBuilder withEditor(TableCellEditor editor) {
         this.editor = editor;
         return this;
      }
      
      public TableModelBuilder<T> add() {
         if(editable != null) {
            if(set == null) {
               throw new IllegalArgumentException("If editable is set, setValue must be set as well");
            }
         }
         else {
            editable = set != null ? (o) -> true : (o) -> false;
         }
         columns.add(new ColumnModel<T>(name, get, set, editable, this.renderer, this.editor));
         return TableModelBuilder.this;
      }
   }
   
}

