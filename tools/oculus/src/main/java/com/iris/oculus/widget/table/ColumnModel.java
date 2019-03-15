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
import java.util.function.Function;

/**
 *
 */
public class ColumnModel<T> {
   private String name;
   private Function<T, Object> get;
   private SetValue<T> set;
   private Function<T, Boolean> editable;
   private TableCellRenderer renderer;
   private TableCellEditor editor;
   
   public ColumnModel(
         String name, 
         Function<T, Object> get, 
         SetValue<T> set,
         Function<T, Boolean> editable, 
         TableCellRenderer renderer,
         TableCellEditor editor
   ) {
      super();
      this.name = name;
      this.get = get;
      this.set = set;
      this.editable = editable;
      this.renderer = renderer;
      this.editor = editor;
   }

   public String getName() {
      return name;
   }
   
   public Object getValue(T i) {
      return get.apply(i);
   }
   
   public void setValue(T o, Object value) {
      set.set(o, value);
   }
   
   public boolean isEditable(T i) {
      return Boolean.TRUE.equals(editable.apply(i));
   }
   
   public TableCellRenderer getRenderer() {
      return renderer;
   }
   
   public TableCellEditor getEditor() {
      return editor;
   }
   
   @FunctionalInterface
   public static interface SetValue<T> {
      public void set(T destination, Object value);
   }
   
   @FunctionalInterface
   public static interface ValueRenderer<C extends Component, T> {
      public void render(C component, T value);
   }
   
   @FunctionalInterface
   public static interface TableCellRenderer {
      public void render(Component destination, Object value, boolean isSelected, boolean hasFocus);
   }

   @FunctionalInterface
   public static interface TableCellEditor {
      public void prepare(Component editor, Object value);
   }
}

