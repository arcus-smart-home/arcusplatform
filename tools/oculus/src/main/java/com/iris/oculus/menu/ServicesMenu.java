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
package com.iris.oculus.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.util.Addresses;
import com.iris.oculus.modules.capability.CapabilityController;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * 
 */
public class ServicesMenu extends BaseComponentWrapper<JMenu> {
   private String label = "Services";
   private DefinitionRegistry registry;
   private CapabilityController controller;
   
   @Inject
   public ServicesMenu(DefinitionRegistry registry, CapabilityController controller) {
      this.registry = registry;
      this.controller = controller;
   }
   
   
   @Override
   protected JMenu createComponent() {
      JMenu menu = new JMenu(label);
      List<ServiceDefinition> services = new ArrayList<>(registry.getServices());
      Collections.sort(services, Comparator.comparing(ServiceDefinition::getName));
      for(ServiceDefinition definition: services) {
      	menu.add(new ServiceDefinitionMenu(definition));
      }
      return menu;
   }

   private class ServiceDefinitionMenu extends JMenu {
		private static final long serialVersionUID = 1L;

		public ServiceDefinitionMenu(ServiceDefinition definition) {
   		setText(String.format("%s (%s)", definition.getName(), definition.getNamespace()));
   		addActions(definition);
   	}
   	
   	public void addActions(ServiceDefinition definition) {
         List<Action> actions = getActionsFromDefinition(definition);
         if(actions.isEmpty()) {
            JMenuItem item = new JMenuItem("No custom commands supported");
            item.setEnabled(false);
            add(item);
         }
         else {
            for(Action action: actions) {
               add(new JMenuItem(action));
            }
         }
   	}
   }

	public List<Action> getActionsFromDefinition(ServiceDefinition definition) {
      List<Action> actions = new ArrayList<>();
      for(MethodDefinition method: definition.getMethods()) {
         Action action = controller.getServiceAction(definition.getNamespace(), method.getName());
         actions.add(action);
      }

      return actions;
	}
}

