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
package com.iris.client.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;

/**
 *
 */
public abstract class BaseModel implements Capability, Model {
	private ListenerList<PropertyChangeEvent> propertyChangeListeners;
   
   private IrisClient client;

	// TODO just grab these from attributes?
	private String id;
	private String type;
	private String address;
	private Map<String, Object> attributes;
	private Map<String, Object> changes;
	private int DEFAULT_TIMEOUT_MS = 10000;

	public BaseModel() {
		this(Collections.<String, Object>emptyMap());
	}

	public BaseModel(Map<String, Object> attributes) {
		this(attributes, IrisClientFactory.getClient());
	}

   public BaseModel(Map<String, Object> attributes, IrisClient client) {
      this.attributes = new HashMap<String, Object>(attributes);
      this.changes = new HashMap<String, Object>();
      this.client = client;
      
      this.id = (String) this.attributes.get(ATTR_ID);
      this.type = (String) this.attributes.get(ATTR_TYPE);
      this.address = (String) this.attributes.get(ATTR_ADDRESS);
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getType() {
      return type;
   }
   
   public void setType(String type) {
      this.type = type;
   }
   
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
   public Collection<String> getTags() {
      return (Collection<String>) this.attributes.get(ATTR_TAGS);
   }

   @Override
   public Map<String, String> getImages() {
      return (Map<String, String>) this.attributes.get(ATTR_IMAGES);
   }

   @Override
   public Collection<String> getCaps() {
      return (Collection<String>) this.attributes.get(ATTR_CAPS);
   }

   @Override
   public Map<String, Collection<String>> getInstances() {
      return (Map<String, Collection<String>>) this.attributes.get(ATTR_INSTANCES);
   }

   /* (non-Javadoc)
    * @see com.iris.client.model.Model#get(java.lang.String)
    */
	@Override
   public Object get(String key) {
		Object v = changes.get(key);
		if(v != null) {
			return v;
		}
		return attributes.get(key);
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#set(java.lang.String, java.lang.Object)
    */
	@Override
   public Object set(String key, Object value) {
		Object old = changes.put(key, value);
		if(old == null) {
			old = attributes.get(key);
		}
		firePropertyChange(key, old, value);
		return old;
	}

	/* (non-Javadoc)
    * @see com.iris.client.capability.Capability#addTags(java.util.Collection)
    */
   @Override
   public ClientFuture<AddTagsResponse> addTags(Collection<String> tags) {
      ClientFuture<ClientEvent> result = request(AddTagsRequest.NAME, ImmutableMap.<String, Object>of(AddTagsRequest.ATTR_TAGS, tags));
      return Futures.transform(result, new Function<ClientEvent, AddTagsResponse>() {
         @Override
         public AddTagsResponse apply(ClientEvent input) {
            return new AddTagsResponse(input);
         }
      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.capability.Capability#removeTags(java.util.Collection)
    */
   @Override
   public ClientFuture<RemoveTagsResponse> removeTags(Collection<String> tags) {
      ClientFuture<ClientEvent> result = request(RemoveTagsRequest.NAME, ImmutableMap.<String, Object>of(RemoveTagsRequest.ATTR_TAGS, tags));
      return Futures.transform(result, new Function<ClientEvent, RemoveTagsResponse>() {
         @Override
         public RemoveTagsResponse apply(ClientEvent input) {
            return new RemoveTagsResponse(input);
         }
      });
   }

   /**
	 * Pushes the value change to the underlying model,
	 * this does not mark the value as dirty.
	 * @param value
	 */
	protected void update(String key, Object value) {
      Object oldValue = this.attributes.put(key, value);
	   if(this.isDirty(key)) {
	      oldValue = this.changes.remove(key);
	   }
	   
		firePropertyChange(key, oldValue, value);
	}

	/**
	 * Pushes value changes to the underlying model, this
	 * does not mark the values as dirty.
	 * @param values
	 */
	protected void update(Map<String, Object> values) {
		for(Map.Entry<String, Object> value: values.entrySet()) {
			update(value.getKey(), value.getValue());
		}
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#isDirty()
    */
	@Override
   public boolean isDirty() {
		return !changes.isEmpty();
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#isDirty(java.lang.String)
    */
	@Override
   public boolean isDirty(String key) {
		return changes.containsKey(key);
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#getChangedValues()
    */
	@Override
   public Map<String, Object> getChangedValues() {
		return changes;
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#clearChanges()
    */
	@Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
	public void clearChanges() {
		if(!changes.isEmpty()) {
			Iterator<String> it = changes.keySet().iterator();
			while(it.hasNext()) {
				String key = it.next();
				firePropertyChange(key, changes.get(key), attributes.get(key));
				it.remove();
			}
		}
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#clearChange(java.lang.String)
    */
	@Override
   public <T> boolean clearChange(String key) {
		Object old = changes.remove(key);
		if(old != null) {
			firePropertyChange(key, old, attributes.get(key));
			return true;
		}
		return false;
	}

	protected IrisClient getClient() {
		return client;
	}

	@Override
   public ClientFuture<ClientEvent> refresh() {
      return 
            request(Capability.CMD_GET_ATTRIBUTES)
               .onSuccess(new Listener<ClientEvent>() {
                  @Override
                  public void onEvent(ClientEvent event) {
                     update(event.getAttributes());
                  }
               });
   }

   @Override
   public ClientFuture<ClientEvent> commit() {
      return request(Capability.CMD_SET_ATTRIBUTES, getChangedValues());
   }

   /* (non-Javadoc)
    * @see com.iris.client.model.Model#request(java.lang.String)
    */
	@Override
   public ClientFuture<ClientEvent> request(String command) {
		return request(command, Collections.<String, Object>emptyMap());
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#request(java.lang.String, java.util.Map)
    */
	@Override
   public ClientFuture<ClientEvent> request(String command, Map<String, Object> attributes) {
		return request(command, attributes, false);
	}
	
	

	@Override
	public ClientFuture<ClientEvent> request(String command, Map<String, Object> attributes, boolean restful) {
		ClientRequest request = new ClientRequest();
		request.setCommand(command);
		request.setAddress(getAddress());
		request.setAttributes(attributes);
		request.setTimeoutMs(DEFAULT_TIMEOUT_MS);
		request.setRestfulRequest(restful);
		return getClient().request(request);
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#request(com.iris.client.ClientRequest)
    */
	@Override
   public ClientFuture<ClientEvent> request(ClientRequest request) {
		request.setAddress(getAddress());
		if(request.getTimeoutMs() <= 0) {
			request.setTimeoutMs(DEFAULT_TIMEOUT_MS);
		}
		return getClient().request(request);
	}

	@Override
   public void updateAttributes(Map<String, Object> attributes) {
      update(attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.client.model.Model#toMap()
    */
	@Override
   public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>(attributes);
		map.putAll(changes);
		return map;
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#addListener(com.iris.client.event.Listener)
    */
	@Override
   public ListenerRegistration addListener(Listener<PropertyChangeEvent> listener) {
		return this.propertyChangeListeners().addListener(listener);
	}

	/* (non-Javadoc)
    * @see com.iris.client.model.Model#addPropertyChangeListener(java.beans.PropertyChangeListener)
    */
	@Override
   public ListenerRegistration addPropertyChangeListener(final PropertyChangeListener listener) {
		return this.propertyChangeListeners().addListener(new Listener<PropertyChangeEvent>() {
			@Override
			public void onEvent(PropertyChangeEvent event) {
				listener.propertyChange(event);
			}
		});
	}
	
	// TODO should this just be onEvent?
	@Override
   public void onDeleted() {
      firePropertyChange(Capability.EVENT_DELETED, this, null);
   }
   
	protected void firePropertyChange(String key, Object oldValue, Object newValue) {
		if(propertyChangeListeners == null) {
			return;
		}
		propertyChangeListeners.fireEvent(new PropertyChangeEvent(this, key, oldValue, newValue));
	}

	protected ListenerList<PropertyChangeEvent> propertyChangeListeners() {
		if(propertyChangeListeners == null) {
			propertyChangeListeners = new ListenerList<PropertyChangeEvent>();
		}
		return propertyChangeListeners;
	}

}

