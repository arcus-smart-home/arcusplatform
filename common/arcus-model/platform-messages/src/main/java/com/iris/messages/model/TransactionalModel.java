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
package com.iris.messages.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Objects;
import com.iris.messages.event.ModelChangedEvent;

public class TransactionalModel extends SimpleModel {
   private Map<String, Object> dirtyAttributes = new HashMap<>();

	public TransactionalModel() {
		super();
	}

	public TransactionalModel(Map<String, Object> attributes) {
		super(attributes);
	}

	public TransactionalModel(@Nullable Model copy) {
		super(copy);
	}

   public boolean isDirty() {
      return !dirtyAttributes.isEmpty(); 
   }
   
   public Set<String> getDirtyAttributeNames() {
      return dirtyAttributes.keySet();
   }
   
   List<ModelChangedEvent> commit() {
   	List<ModelChangedEvent> changes = new ArrayList<>(dirtyAttributes.size());
   	if(!isDirty()) {
   		return changes;
   	}
   	
   	for(Map.Entry<String, Object> dirtyAttribute: dirtyAttributes.entrySet()) {
   		String attributeName = dirtyAttribute.getKey();
   		ModelChangedEvent event = ModelChangedEvent.create(getAddress(), attributeName, dirtyAttribute.getValue(), super.getAttribute(attributeName));
   		changes.add(event);
   	}
   	super.update(dirtyAttributes);
   	dirtyAttributes.clear();
   	return changes;
   }
   
	List<ModelChangedEvent> commit(Map<String, Object> attributes) {
   	List<ModelChangedEvent> changes = new ArrayList<>(attributes.size());
   	
   	for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
   		String attributeName = attribute.getKey();
   		ModelChangedEvent event = ModelChangedEvent.create(getAddress(), attributeName, attribute.getValue(), super.getAttribute(attributeName));
   		changes.add(event);
   		super.setAttribute(attributeName, attribute.getValue());
      	dirtyAttributes.remove(attributeName);
   	}
   	return changes;
	}

   @Override
	public Object getAttribute(String name) {
		if(dirtyAttributes.containsKey(name)) {
			return dirtyAttributes.get(name);
		}
		else {
			return super.getAttribute(name);
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.putAll(dirtyAttributes);
		return map;
	}

	@Override
	public Iterable<String> keys() {
		return toMap().keySet();
	}

	@Override
	public Iterable<Object> values() {
		return toMap().values();
	}

	@Override
   public Object setAttribute(String name, Object value) {
		Object old = super.getAttribute(name);
		if(Objects.equal(value, old)) {
			dirtyAttributes.remove(name);
		}
		else {
			dirtyAttributes.put(name, value);
		}
      return old;
   }

   @Override
   public void update(Map<String, Object> attributes) {
      for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
      	setAttribute(attribute.getKey(), attribute.getValue());
      }
   }

   @Override
   public String toString() {
      return "TransactionalModel [address=" + getAddress() + 
            ", type=" + getType() +
            ", attributes=" + toMap() +
            "]";
   }

   // NOTE hashCode / equals is inherited from SimpleModel because equivalency is not affected by dirtyAttributeNames
}

