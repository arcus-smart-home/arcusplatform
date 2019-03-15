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
package com.iris.client.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.capability.Capability;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;

public interface Model extends Capability {

   Object get(String key);

   Object set(String key, Object value);

   boolean isDirty();

   boolean isDirty(String key);

   Map<String, Object> getChangedValues();

   void clearChanges();

    <T> boolean clearChange(String key);
    
    ClientFuture<ClientEvent> refresh();
    
    ClientFuture<ClientEvent> commit();
    
    ClientFuture<ClientEvent> request(String command);

    ClientFuture<ClientEvent> request(
         String command, Map<String, Object> attributes);
    
    ClientFuture<ClientEvent> request(
          String command, Map<String, Object> attributes, boolean restful);

    ClientFuture<ClientEvent> request(ClientRequest request);

    /**
     * Updates the given attributes from the map representation.
     * This will clear out any dirty fields and fire the
     * appropriate value change events.
     * @param attributes
     */
    void updateAttributes(Map<String, Object> attributes);

    void onDeleted();
    
    Map<String, Object> toMap();

    ListenerRegistration addListener(
         Listener<PropertyChangeEvent> listener);

    ListenerRegistration addPropertyChangeListener(
         PropertyChangeListener listener);

}

