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
package com.iris.oculus.widget.table;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.swing.table.AbstractTableModel;

import com.iris.client.event.ListenerRegistration;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.view.ViewModelEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelAddedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelChangedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelRemovedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelUpdatedEvent;

public class TableModel<T> extends AbstractTableModel {
   private final List<ColumnModel<T>> columns;
   private ViewModel<T> model;
   private ListenerRegistration registration;

   TableModel(List<ColumnModel<T>> columns, Collection<T> initial) {
      this.columns = columns;
      this.model = new SimpleViewModel<>(initial);
   }

   TableModel(List<ColumnModel<T>> columns, ViewModel<T> model) {
      this.columns = columns;
      this.model = model;
      if(model != null) {
         registration = this.model.addViewListener((event) -> onViewEvent(event));
      }
   }

   public void bind(ViewModel<T> model) {
      if(Objects.equals(this.model, model)) {
         return;
      }

      if(registration != null) {
         registration.remove();
         this.model = null;
      }
      if(model != null) {
         this.model = model;
         registration = model.addViewListener((event) -> onViewEvent(event));
      }
      fireTableDataChanged();
   }

   public void unbind() {
      bind(null);
   }

   public ColumnModel<T> getColumn(int column) {
      return columns.get(column);
   }

   public List<ColumnModel<T>> getColumns() {
      return columns;
   }

   public T getValue(int rowIndex) {
      return model.get(rowIndex);
   }

   @Override
   public int getRowCount() {
      return model.size();
   }

   @Override
   public int getColumnCount() {
      return columns.size();
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      ColumnModel<T> column = getColumn(columnIndex);
      T value = getValue(rowIndex);
      if (value == null) return null;
      return column.getValue(value);
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      ColumnModel<T> column = getColumn(columnIndex);
      T value = getValue(rowIndex);
      return column.isEditable(value);
   }

   @Override
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      ColumnModel<T> column = getColumn(columnIndex);
      T value = getValue(rowIndex);
      column.setValue(value, aValue);
   }

   public void fireTableCellUpdated(String columnName, T value) {
      int rowIdx = model.indexOf(value);
      int columnIdx = findColumn(columnName);
      if(rowIdx == -1) {
         this.fireTableDataChanged();
      }
      else if(columnIdx == -1) {
         this.fireTableRowsUpdated(rowIdx, rowIdx);
      }
      else {
         this.fireTableCellUpdated(rowIdx, columnIdx);
      }
   }

   protected void onViewEvent(ViewModelEvent event) {
      if(event instanceof ViewModelChangedEvent) {
         fireTableDataChanged();
      }
      else if(event instanceof ViewModelAddedEvent) {
         fireTableRowsInserted(event.getStart(), event.getEnd());
      }
      else if(event instanceof ViewModelUpdatedEvent) {
         fireTableRowsUpdated(event.getStart(), event.getEnd());
      }
      else if(event instanceof ViewModelRemovedEvent) {
         fireTableRowsDeleted(event.getStart(), event.getEnd());
      }
   }

}


