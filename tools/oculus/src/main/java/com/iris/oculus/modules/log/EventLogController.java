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
package com.iris.oculus.modules.log;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.iris.client.ClientMessage;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.session.SessionEvent;
import com.iris.io.json.JSON;
import com.iris.oculus.modules.log.model.EventLogModel;
import com.iris.oculus.modules.log.ux.EventLogPopup;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;

@Singleton
public class EventLogController {
   // TODO limit size
   // TODO dump to file
   private SimpleViewModel<EventLogModel> events = new SimpleViewModel<>();
   private EventLogPopup logs = new EventLogPopup(events);

   @Inject
   public EventLogController(IrisClient client) {
      client.addMessageListener((message) -> onMessage(message));
      client.addRequestListener((request) -> onRequest(request));
      client.addSessionListener((event) -> onSessionEvent(event));
   }
   
   public ViewModel<EventLogModel> getEventLogs() {
      return events;
   }
   
   public void showEventLogs() {
      logs.show();
   }
   
   protected void add(EventLogModel model) {
      events.add(model);
   }
   
   protected void onMessage(ClientMessage message) {
      EventLogModel model = new EventLogModel();
      model.setType(message.getType());
      model.setAddress(message.getSource());
      // TODO get the real timestamp header
      model.setTimestamp(new Date());
      model.setCorrelationId(message.getCorrelationId());
      model.setContent(JSON.toJson(message.getEvent().getAttributes()));
      
      add(model);
   }

   protected void onRequest(ClientRequest request) {
      EventLogModel model = new EventLogModel();
      model.setType(request.getCommand());
      model.setAddress(request.getAddress());
      // TODO get the real timestamp header
      model.setTimestamp(new Date());
      model.setCorrelationId("<unknown>");
      model.setContent(JSON.toJson(request.getAttributes()));
      
      add(model);
   }

   protected void onSessionEvent(SessionEvent event) {
      EventLogModel model = new EventLogModel();
      model.setType(event.getClass().getSimpleName());
      model.setAddress("<session>");
      // TODO get the real timestamp header
      model.setTimestamp(new Date());
      model.setContent(JSON.toJson(event));
      
      add(model);
   }
}

