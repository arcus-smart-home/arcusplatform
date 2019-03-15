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

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;

import com.iris.oculus.Oculus;
import com.iris.oculus.widget.table.ColumnModel.TableCellEditor;
import com.iris.oculus.widget.table.ColumnModel.TableCellRenderer;

/**
 * @author tweidlin
 *
 */
public class Table<T> extends JTable {

   public Table(List<ColumnModel<T>> columns) {
      this(new TableModel<T>(columns, Collections.<T>emptyList()));
   }
   
   public Table(TableModel<T> model) {
      super(model);
      init(model.getColumns());
   }
   
   private void init(List<ColumnModel<T>> columns) {
      int i = 0;
      for(ColumnModel<?> cm: columns) {
         getColumnModel().getColumn(i).setHeaderValue(cm.getName());
         TableCellRenderer renderer = cm.getRenderer();
         if(renderer != null) {
            getColumnModel().getColumn(i).setCellRenderer(new CellRenderer(renderer));
         }
         TableCellEditor editor = cm.getEditor();
         if(editor != null) {
            getColumnModel().getColumn(i).setCellEditor(new CellEditor(editor));
         }
         i++;
      }
   }

   @Override
   public TableModel<T> getModel() {
      return (TableModel<T>) super.getModel();
   }
   
   private class CellRenderer extends DefaultTableCellRenderer {
      private TableCellRenderer renderer;

      CellRenderer(TableCellRenderer renderer) {
         this.renderer = renderer;
      }
      
      @Override
      public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
         Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         renderer.render(c, value, isSelected, hasFocus);
         return c;
      }
      
   }
   
   private class CellEditor extends DefaultCellEditor {
      private TableCellEditor editor;
      
      public CellEditor(TableCellEditor editor) {
         super(new JTextField());
         this.getComponent().setName("Table.editor");
         this.editor = editor;
      }

      @Override
      public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column
      ) {
         Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
         c.setFont(table.getFont());
         editor.prepare(c, value);
         return c;
      }

      @Override
      public boolean stopCellEditing() {
         try {
            return super.stopCellEditing();
         }
         catch(Exception e) {
            JComponent c = (JComponent) getComponent();
            c.setBorder(new LineBorder(Color.RED));
            Oculus.warn("Invalid value for cell: " + e.getMessage() + "\nClick 'esc' to stop editing", e);
            return false;
         }
      }
      
   }
   
}

