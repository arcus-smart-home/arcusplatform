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
package com.iris.oculus.modules.models;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Device;
import com.iris.client.capability.Rule;
import com.iris.client.capability.Scene;
import com.iris.client.capability.Subsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.Model;
import com.iris.client.model.RuleModel;
import com.iris.client.model.SceneModel;
import com.iris.client.model.SubsystemModel;
import com.iris.io.json.JSON;
import com.iris.oculus.modules.capability.ux.ModelTableView;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.JsonPrettyPrinter;

@Singleton
public class ModelController {
   private Map<String, Window> windows = new LinkedHashMap<String, Window>();
   private Map<String, Window> attributeWindows = new HashMap<String, Window>();

   public ModelController() {
      // TODO Auto-generated constructor stub
   }

   @PostConstruct
   public void init() {
   }
   
   public ClientFuture<String> getName(String address) {
      // TODO get this even if it isn't in the cache
      Model model = IrisClientFactory.getModelCache().get(address);
      if(model == null) {
         return Futures.succeededFuture(address);
      }
      else {
         return Futures.succeededFuture(getName(model));
      }
   }
   
   public String getName(Model model) {
      String type = model.getType();
      switch(type) {
      case Device.NAMESPACE:
         return ((DeviceModel) model).getName();
         
      case Rule.NAMESPACE:
         return ((RuleModel) model).getName();
         
      case Scene.NAMESPACE:
         return ((SceneModel) model).getName();
         
      case Subsystem.NAMESPACE:
         return ((SubsystemModel) model).getName();
      
      default:
         return model.getType() + " " + model.getId();
      }
   }
   
   public Window show(Model model) {
      Window window = windows.computeIfAbsent(model.getAddress(), (address) -> {
         ModelTableView view = new ModelTableView();
         view.bind(new DefaultSelectionModel<Model>(model));
         
         JDialog dialog = new JDialog(null, model.getAddress(), ModalityType.MODELESS);
         dialog.add(new JScrollPane(view.getComponent()));
         dialog.setSize(600, 400);
         dialog.addWindowListener(new WindowAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.WindowAdapter#windowClosed(java.awt.event.WindowEvent)
             */
            @Override
            public void windowClosed(WindowEvent e) {
               view.dispose();
            }
         });
         
         return dialog;
      });
      window.setVisible(true);
      return window;
   }
   
   public Window showAttribute(Model model, AttributeDefinition definition) {
      String label = definition.getName() + " - " + definition.getDescription();
      return doShowAttribute(model, definition.getName(), label);
   }
   
   public Window showAttribute(Model model, AttributeDefinition definition, String instance) {
      String label = definition.getName() + ":" + instance + " - " + definition.getDescription();
      return doShowAttribute(model, definition.getName() + ":" + instance, label);
   }
   
   public Window showAttribute(Model model, String attribute) {
      return doShowAttribute(model, attribute, attribute);
   }
   
   public void close(String address) {
      Window window = windows.remove(address);
      if(window != null) {
         window.dispose();
      }
   }
   
   protected Window doShowAttribute(Model model, String attributeName, String label) {
      String key = model.getAddress() + ":" + attributeName;
      Window window = attributeWindows.computeIfAbsent(key, (k) -> createAttributeDialog(model, attributeName, label) );
      window.setVisible(true);
      return window;
   }
   
   private Window createAttributeDialog(Model model, String attributeName, String label) {
      JDialog window = new JDialog((Window) null, model.getAddress() + ": " + attributeName, ModalityType.MODELESS);
      
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(new JLabel(label), BorderLayout.NORTH);
      // TODO make this a JsonField...
      JTextPane pane = new JTextPane();
      pane.setContentType("text/html");
      pane.setText(JsonPrettyPrinter.prettyPrint(JSON.toJson(model.get(attributeName))));
      
      ListenerRegistration l = model.addListener((event) -> {
         if(event.getPropertyName().equals(attributeName)) {
            // TODO set background green and slowly fade out
            pane.setText(JsonPrettyPrinter.prettyPrint(JSON.toJson(event.getNewValue())));
         }
      });
      
      panel.add(pane, BorderLayout.CENTER);
      
      window.addWindowListener(new WindowAdapter() {
         /* (non-Javadoc)
          * @see java.awt.event.WindowAdapter#windowClosed(java.awt.event.WindowEvent)
          */
         @Override
         public void windowClosed(WindowEvent e) {
            l.remove();
            attributeWindows.remove(model.getAddress() + ":"  + attributeName);
         }
      });
      
      return window;
   }
}

