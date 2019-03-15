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
package com.iris.messages.model;

import java.util.List;
import java.util.Map;

import com.iris.messages.address.Address;
import com.iris.messages.event.ModelChangedEvent;

/**
 * Allows models to be edited then committed generating the normal ValueChangeEvents.
 */
public class TransactionalModelStore extends SimpleModelStore {

	@Override
	public boolean updateModel(Address address, Map<String, Object> attributes) {
      TransactionalModel model = (TransactionalModel) getModelByAddress(address);
      if(model == null) {
         return false;
      }

      List<ModelChangedEvent> events = model.commit(attributes);
		for(ModelChangedEvent event: events) {
			fire(event);
		}
      return true;
	}

	public void commit() {
		for(Model model: getModels()) {
			TransactionalModel m = (TransactionalModel) model;
			if(m.isDirty()) {
				for(ModelChangedEvent event: m.commit()) {
					fire(event);
				}
			}
		}
	}

	@Override
	protected Model newModel(Map<String, Object> attributes) {
		return new TransactionalModel(attributes);
	}
}

