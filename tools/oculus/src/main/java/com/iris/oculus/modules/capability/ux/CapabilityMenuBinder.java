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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.Utils;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.MethodDefinition;
import com.iris.client.ClientEvent;
import com.iris.client.model.Model;
import com.iris.io.json.JSON;
import com.iris.oculus.Oculus;

/**
 *
 */
public class CapabilityMenuBinder {
   private static final Logger logger = LoggerFactory.getLogger(CapabilityMenuBinder.class);
   private static final Map<String, CapabilityMenuBinder> Binders =
         new HashMap<>();

   public static CapabilityMenuBinder get(CapabilityDefinition definition) {
      return get(definition, null);
   }

   public static CapabilityMenuBinder get(CapabilityDefinition definition, String instanceId) {
      Utils.assertTrue(SwingUtilities.isEventDispatchThread());
      CapabilityMenuBinder binder = Binders.get(definition.getName());
      if(binder == null) {
         binder = new CapabilityMenuBinder(definition, instanceId);
         Binders.put(definition.getName(), binder);
      }
      return binder;
   }

   private JPopupMenu menu;
   private Model model;

   public CapabilityMenuBinder(CapabilityDefinition definition) {
      this(definition, null);
   }

   public CapabilityMenuBinder(CapabilityDefinition definition, String instanceId) {
      List<Action> actions = getActionsFromDefinition(definition, model, instanceId);
      menu = new JPopupMenu("Commands");
      if(actions.isEmpty()) {
         JMenuItem item = new JMenuItem("No custom commands supported");
         item.setEnabled(false);
         menu.add(item);
      }
      else {
         for(Action action: actions) {
            menu.add(new JMenuItem(action));
         }
      }
   }

   public void bindAndShow(Model model, Component c) {
      bindAndShow(model, c, 0, 0);
   }

   public void bindAndShow(Model model, Component c, int offsetX, int offsetY) {
      this.model = model;      // TODO change this.model to be just a model.
      this.menu.show(c, offsetX, offsetY);
   }

   protected void onExecuteCommand(String command, boolean restful) {
      onExecuteCommand(command, Collections.emptyMap(), restful);
   }

   protected void onExecuteCommand(String command, Map<String, Object> arguments, boolean restful) {
   	CapabilityResponsePopUp popUp = new CapabilityResponsePopUp();
   	popUp.setCommand(command);
   	popUp.setAttributes(arguments);
      model
         .request(command, arguments, restful)
         .onSuccess(popUp::onResponse)
         .onFailure(popUp::onError)
         ;
   }

   protected void onCommandSuccess(ClientEvent event) {
      Oculus.showDialog("Completed request response", JSON.toJson(event), JOptionPane.PLAIN_MESSAGE);
      //JOptionPane.showMessageDialog(null, "Completed request response:\n" + event, "Succeeded", JOptionPane.INFORMATION_MESSAGE);
   }

   protected void onCommandFailure(Throwable error) {
      logger.warn("Error exeucting device command", error);
      JOptionPane.showMessageDialog(null, "Unable to execute command.\nDetails: " + error.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
   }

   private List<Action> getActionsFromDefinition(CapabilityDefinition definition, Model model, String instanceId) {
      List<Action> actions = new ArrayList<>();
      for(MethodDefinition method: definition.getMethods()) {
         Action action;
         if(method.getParameters().isEmpty()) {
            action = new ExecuteDeviceCommand(method, instanceId);
         }
         else {
            action = new InputPromptAction(method, instanceId);
         }
         actions.add(action);
      }

      return actions;
   }

   // TODO move these out to the controller

   private class ExecuteDeviceCommand extends AbstractAction {
      private String name;
      private boolean restful = false;

      ExecuteDeviceCommand(MethodDefinition method, String instanceId) {
         super(method.getName());
         if(instanceId != null) {
            this.name = method.getName() + ":" + instanceId;
         }
         else {
            this.name = method.getName();
         }
         restful = method.isRestful();
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         onExecuteCommand(name, restful);
      }

   }

   private class InputPromptAction extends AbstractAction {
      private MethodDefinition method;
      private String name;
      private boolean restful;

      InputPromptAction(MethodDefinition method, String instanceId) {
         super(method.getName() + "...");
         this.method = method;
         if(instanceId != null) {
            this.name = method.getName() + ":" + instanceId;
         }
         else {
            this.name = method.getName();
         }
         this.restful = method.isRestful();
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         CapabilityInputPrompt
            .prompt(method)
            .onSuccess((arguments) -> onExecuteCommand(name, arguments, restful))
            ;
      }

   }
}

