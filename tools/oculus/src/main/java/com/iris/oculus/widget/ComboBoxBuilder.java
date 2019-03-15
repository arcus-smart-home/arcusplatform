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
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.client.event.Listener;
import com.iris.client.model.Model;
import com.iris.client.model.Store;
import com.iris.oculus.view.ViewComboBoxModel;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.view.ViewModels;

/**
 * @author tweidlin
 *
 */
public class ComboBoxBuilder<T> {
   private static Logger logger = LoggerFactory.getLogger(ComboBoxBuilder.class);
   
   public static <V> ComboBoxBuilder<V> from(Collection<V> options) {
      return from(ViewModels.from(options));
   }
   
   public static <M extends Model> ComboBoxBuilder<M> from(Store<M> store) {
      return from(ViewModels.from(store));
   }
   
   public static <M> ComboBoxBuilder<M> from(ViewModel<M> model) {
      ViewComboBoxModel<M> m = new ViewComboBoxModel<>();
      m.bind(model);
      return
            new ComboBoxBuilder<M>()
               .withModel(m)
               ;
   }
   
   private ComboBoxModel<T> model = new DefaultComboBoxModel<>();
   private boolean editable = true;
   private ListCellRenderer<? super T> listRenderer;
   private ComboBoxEditor editor;
   private List<Listener<? super T>> listeners;
   
   public ComboBoxBuilder<T> withModel(ComboBoxModel<T> model) {
      this.model = model;
      return this;
   }
   
   public ComboBoxBuilder<T> withValues(Collection<T> values) {
      DefaultComboBoxModel<T> m = new DefaultComboBoxModel<>(new Vector<>(values));
      return withModel(m);
   }
   
   public ComboBoxBuilder<T> withCellRenderer(ListCellRenderer<? super T> renderer) {
      this.listRenderer = renderer;
      return this;
   }
   
   public ComboBoxBuilder<T> withCellRenderer(Function<T, String> renderer) {
      withCellRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(
               JList<?> list, Object value, int index, boolean isSelected,
               boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) c).setText(renderer.apply((T) value));
            return c;
         }
      });
      return this;
   }
   
   public ComboBoxBuilder<T> withValueRenderer(Function<T, String> renderer) {
      this.editor = new ComboBoxEditor() {
         JTextField editor = new JTextField();
         
         @Override
         public void setItem(Object anObject) {
            if(anObject == null) {
               editor.setText("");
               return;
            }
            
            if(anObject instanceof String) {
               editor.setText((String) anObject);
               return;
            }
            
            try {
               editor.setText(renderer.apply((T) anObject));
            }
            catch(Exception e) {
               logger.warn("Unable to render item {}", anObject, e);
            }
         }
         
         @Override
         public void selectAll() {
            editor.selectAll();
         }
         
         @Override
         public void removeActionListener(ActionListener l) {
            editor.removeActionListener(l);
         }
         
         @Override
         public Object getItem() {
            return editor.getText();
         }
         
         @Override
         public Component getEditorComponent() {
            return editor;
         }
         
         @Override
         public void addActionListener(ActionListener l) {
            editor.addActionListener(l);
         }
      };
      return this;
   }
   
   public ComboBoxBuilder<T> withRenderer(Function<T, String> renderer) {
      withCellRenderer(renderer);
      withValueRenderer(renderer);
      return this;
   }
   
   public ComboBoxBuilder<T> withRenderer(Function<T, String> renderer, String emptyValue) {
      return withRenderer((value) -> value == null ? emptyValue : renderer.apply(value));
   }
   
   public ComboBoxBuilder<T> addListener(Listener<? super T> listener) {
      if(listeners == null) {
         listeners = new ArrayList<>();
      }
      listeners.add(listener);
      return this;
   }
   
   public ComboBoxBuilder<T> editable() {
      this.editable = true;
      return this;
   }
   
   public ComboBoxBuilder<T> noteditable() {
      this.editable = false;
      return this;
   }
   
   public JComboBox<T> create() {
      JComboBox<T> combo = new JComboBox<T>(model);
      combo.setEditable(editable);
      if(listRenderer != null) {
         combo.setRenderer(listRenderer);
      }
      if(editable && editor != null) {
         combo.setEditor(editor);
      }
      if(listeners != null) {
         final List<Listener<? super T>> l = ImmutableList.copyOf(listeners);
         combo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
               if(e.getStateChange() == ItemEvent.SELECTED) {
                  for(Listener<? super T> listener: l) {
                     listener.onEvent((T) e.getItem());
                  }
               }
            }
         });
      }
      return combo;
   }

}

