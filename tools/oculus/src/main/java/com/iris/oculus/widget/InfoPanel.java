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
package com.iris.oculus.widget;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;

import com.iris.Utils;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;

public abstract class InfoPanel<T> extends BaseComponentWrapper<JComponent> { 
   private TableModel<T> model;
   private boolean scrollable = true;
   
   public InfoPanel() {
      model = createStoreTableModel();
   }
   
   public InfoPanel(ViewModel<T> view) {
      this();
      model.bind(view);
   }

   public boolean isScrollable() {
      return scrollable;
   }
   
   public void setScrollable(boolean scrollable) {
      Utils.assertFalse(isActive(), "Can't change the scroll-ability while the component is rendered, dispose and re-add to change");
      this.scrollable = true;
   }
   
   @Override
   protected JComponent createComponent() {
      Table<T> table = new Table<>(model);
      table.getModel().addTableModelListener((event) -> {
         if(event.getType() == TableModelEvent.INSERT) {
            int viewRow = table.convertRowIndexToView(event.getLastRow());
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
         }
      });

      JTextPane message = new JTextPane();
      message.setEditable(false);
      message.setContentType("text/html");
      
      JSplitPane contents = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      contents.add(new JScrollPane(table), JSplitPane.TOP);
      contents.add(new JScrollPane(message), JSplitPane.BOTTOM);
      
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            // wait for it to finish
            if(e.getValueIsAdjusting()) {
               return;
            }
            
            int selectedRow = table.getSelectedRow();
            T value = selectedRow > -1 && selectedRow < model.getRowCount() ? model.getValue(selectedRow) : null;
            if(value != null) {
               message.setText(getMessageValue(value));
               message.setCaretPosition(0);
            }
            contents.revalidate();
         }
      });
      return contents;
   }

   @Override
   protected boolean disposeComponent(JComponent component) {
      model.unbind();
      component = null;
      return true;
   }
   
   protected abstract TableModel<T> createStoreTableModel();
   
   protected abstract String getMessageValue(T value);
}

