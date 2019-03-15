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
package com.iris.platform.subsystem.impl;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModelStore;

public class PlatformSubsystemModelStore extends SimpleModelStore {
   private static final Logger log = LoggerFactory.getLogger(PlatformSubsystemModelStore.class);

	@Override
	protected void updateEventAdded(PlatformMessage message) {
		if (SubsystemCapability.NAMESPACE.equals(message.getValue().getAttributes().get(Capability.ATTR_TYPE))) {
         log.trace("skipping value change for subsystem");
         return;
		}
		
		super.updateEventAdded(message);
	}

	@Override
   protected void updateEventValueChange(PlatformMessage message, Address source, Model model) {
      if (SubsystemCapability.NAMESPACE.equals(model.getType())) {
         log.trace("skipping value change for subsystem");
         return;
      }

      super.updateEventValueChange(message, source, model);
   }

   // allow models to be added directly so that they may be ModelEntity and we don't
   // have a separate copy for the DAO
   
   public void addModels(Collection<Model> models) {
      if (models != null) {
         for(Model model : models) {
            addModel(model);
         }
      }
   }
   
   public Model addModel(Model model) {
   	return super.addModel(model);
   }

   void fireModelEvent(ModelEvent event) {
      fire(event);
   }
}

