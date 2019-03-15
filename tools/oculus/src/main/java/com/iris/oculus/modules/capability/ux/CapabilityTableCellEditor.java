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
package com.iris.oculus.modules.capability.ux;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.AttributeType;
import com.iris.oculus.Oculus;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;

/**
 * 
 */
public class CapabilityTableCellEditor extends DefaultCellEditor {
   private TableCellEditor current;
   private Map<AttributeType, TableCellEditor> cache = new HashMap<>();

   public CapabilityTableCellEditor() {
      super(new JTextField());
      this.getComponent().setName("Table.editor");
   }

   protected TableCellEditor getCurrent() {
      if(current == null) {
         throw new IllegalStateException("No editor currently active");
      }
      return current;
   }
   
   protected Object renderValue(Object value) {
      return value;
   }
   
   protected Object parseValue(Object model) {
      return model;
   }
   
   @Override
   public Object getCellEditorValue() {
      return parseValue(getCurrent().getCellEditorValue());
   }

//   @Override
//   public boolean isCellEditable(EventObject anEvent) {
//      return getCurrent().isCellEditable(anEvent);
//   }
//
//   @Override
//   public boolean shouldSelectCell(EventObject anEvent) {
//      return getCurrent().shouldSelectCell(anEvent);
//   }
//
   @Override
   public boolean stopCellEditing() {
      if(current == null) {
         return true;
      }
      try {
         boolean stopped = current.stopCellEditing();
         if(stopped) {
            current = null;
         }
         return stopped;
      }
      catch(Exception e) {
         JComponent c = (JComponent) getComponent();
         c.setBorder(new LineBorder(Color.RED));
         Oculus.warn("Invalid value for cell: " + e.getMessage() + "\nClick 'esc' to stop editing", e);
         return false;
      }
   }

   @Override
   public void cancelCellEditing() {
      if(current != null) {
         current.cancelCellEditing();
      }
      current = null;
   }

   @Override
   public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      AttributeDefinition definition = (AttributeDefinition) value;
      AttributeType type = definition.getType();
      current = cache.get(type);
      if(current == null) {
         current = createNew(type);
         cache.put(type, current);
      }
      Component c = current.getTableCellEditorComponent(table, renderValue(value), isSelected, row, column);
      c.setFont(table.getFont());
      return c;
   }

   private TableCellEditor createNew(AttributeType type) {
      FieldWrapper<? extends Component, ?> wrapper =
            Fields
               .attributeTypeBuilder(type, false) // checkbox input is HORRIBLE in the table
               .labelled("editor")
               .build();
      FieldCellEditor<?> editor = FieldCellEditor.create(wrapper);
      editor.addCellEditorListener(new CellEditorListener() {
         
         @Override
         public void editingStopped(ChangeEvent e) {
            CapabilityTableCellEditor.this.fireEditingStopped();
         }
         
         @Override
         public void editingCanceled(ChangeEvent e) {
            CapabilityTableCellEditor.this.fireEditingCanceled();
         }
      });
      return editor;
   }
   
   private static class FieldCellEditor<V> extends DefaultCellEditor {
      FieldWrapper<? extends Component, V> wrapper; 

      public static <V> FieldCellEditor<V> create(FieldWrapper<? extends Component, V> wrapper) {
         Component c = wrapper.getComponent();
         if(c instanceof JTextField) {
            return new FieldCellEditor<V>((JTextField) c, wrapper);
         }
         else if(c instanceof JCheckBox) {
            return new FieldCellEditor<V>((JCheckBox) c, wrapper);
         }
         else if(c instanceof JComboBox) {
            return new FieldCellEditor<V>((JComboBox<?>) c, wrapper);
         }
         else {
            // oops
            throw new IllegalArgumentException("Unsupported AttributeType");
         }
      }
      
      public FieldCellEditor(JCheckBox checkBox, FieldWrapper<? extends Component, V> wrapper) {
         super(checkBox);
         this.wrapper = wrapper;
      }
      
      public FieldCellEditor(JTextField textField, FieldWrapper<? extends Component, V> wrapper) {
         super(textField);
         this.wrapper = wrapper;
      }
      
      public FieldCellEditor(JComboBox<?> comboBox, FieldWrapper<? extends Component, V> wrapper) {
         super(comboBox);
         this.wrapper = wrapper;
      }

      @Override
      public Object getCellEditorValue() {
         return wrapper.getValue();
      }

      @Override
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
         Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
         wrapper.setValue((V) value);
         return c;
      }
      
   }

}

