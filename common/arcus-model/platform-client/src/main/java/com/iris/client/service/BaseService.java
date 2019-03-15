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
package com.iris.client.service;

import java.util.Collections;
import java.util.Map;

import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.EmptyEvent;
import com.iris.client.ErrorEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.exception.ErrorResponseException;

/**
 *
 */
public abstract class BaseService implements Service {
   
   protected IrisClient getClient() {
      return IrisClientFactory.getClient();
   }
   
   protected ClientFuture<ClientEvent> request(String command) {
      return request(command, Collections.<String, Object>emptyMap());
   }
   
   protected ClientFuture<ClientEvent> request(String command, Map<String, Object> attributes) {
      ClientRequest request = new ClientRequest();
      request.setAddress(getAddress());
      request.setAttributes(attributes);
      return getClient().request(request);
   }
 
   protected <V> ClientFuture<V> request(ClientRequest request, final Transform<V> t) {
      final SettableClientFuture<V> future = new SettableClientFuture<V>();
      getClient()
         .request(request)
         .onSuccess(new Listener<ClientEvent>() {
            @Override
            public void onEvent(ClientEvent event) {
               try {
                  if(event == null || EmptyEvent.NAME.equals(event.getType())) {
                     future.setValue(null);
                  }
                  else if(ErrorEvent.NAME.equals(event.getType())) {
                     future.setError(new ErrorResponseException((String) event.getAttribute("code"), (String) event.getAttribute("message")));
                  }
                  else {
                     future.setValue(t.transform(event));
                  }
               }
               catch(Exception e) {
                  future.setError(e);
               }
            }
         })
         .onFailure(new Listener<Throwable>() {
            @Override
            public void onEvent(Throwable error) {
               future.setError(error);
            }
         });
      return future;
   }

   public interface Transform<V> {
      public V transform(ClientEvent event);
   }
}

