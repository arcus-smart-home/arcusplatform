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
package com.iris.oculus.modules.session.ux;

import java.awt.BorderLayout;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Place;
import com.iris.client.connection.ConnectionState;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.Listeners;
import com.iris.client.session.SessionActivePlaceSetEvent;
import com.iris.client.session.SessionAuthenticatedEvent;
import com.iris.client.session.SessionEvent;
import com.iris.client.session.SessionExpiredEvent;
import com.iris.client.session.SessionInfo;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.BaseComponentWrapper;

public class StatusBar extends BaseComponentWrapper<JPanel> {
   private static final Logger logger = LoggerFactory.getLogger(StatusBar.class);
   
   private IrisClient client;
   private ListenerRegistration connectionListener = Listeners.unregistered();
   private ListenerRegistration sessionListener = Listeners.unregistered();
   private JPanel contents = new JPanel();
   private JLabel activePlace = new JLabel();
   private JLabel connection = new JLabel();
   private JLabel session = new JLabel();

   @Override
   protected JPanel createComponent() {
      contents.setLayout(new BorderLayout());
      activePlace.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      session.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));
      connection.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(1, 3, 1, 3)));

      contents.add(activePlace, BorderLayout.WEST);
      contents.add(connection, BorderLayout.CENTER);
      contents.add(session, BorderLayout.EAST);
      return contents;
   }

   public void bind(IrisClient client) {
      this.client = client;
      connectionListener = Listeners.unregister(connectionListener);
      sessionListener = Listeners.unregister(sessionListener);
      if(client != null) {
         setConnectionState(client.getConnectionState());
         setSessionInfo(client.getSessionInfo());
         setActivePlace(client.getActivePlace());
         connectionListener = client.addConnectionListener((event) -> setConnectionState(event.getState()));
         sessionListener = client.addSessionListener((event) -> onSessionEvent(event));
      }
//      client.addSessionListener((event) -> setConnectionState(event.getState()));
   }
   
   protected void setConnectionState(ConnectionState state) {
      switch(state) {
      case CLOSED:
         connection.setText("Connection closed, please re-connect");
         break;
      case CONNECTING:
         connection.setText(String.format("Connecting to %s....", client.getConnectionURL()));
         break;
      case CONNECTED:
         // TODO add since now()
         connection.setText(String.format("Connected to %s", client.getConnectionURL()));
         break;
      case DISCONNECTED:
         connection.setText("Lost connection, retrying shortly...");
         break;
      default:
         logger.warn("Unrecognized status [{}]", state);
      }
      contents.validate();
   }
   
   protected void setSessionInfo(SessionInfo info) {
      if(info == null) {
         session.setText("Unauthenticated");
      }
      else if(info.getUsername().equals("Unknown")) {
         session.setText("Logged In");
      }
      else {
         session.setText(info.getUsername());
      }
      contents.validate();
   }
   
   protected void setActivePlace(UUID placeId) {
      if(placeId == null) {
         activePlace.setText("No Active Place");
      }
      else {
         activePlace.setText("Loading Place...");
         ClientRequest request = new ClientRequest();
         request.setAddress("SERV:place:" + placeId.toString());
         request.setCommand(Capability.CMD_GET_ATTRIBUTES);
         client.request(request)
            .onSuccess((response) -> activePlace.setText((String) response.getAttribute(Place.ATTR_NAME)))
            .onFailure((e) -> {
               Oculus.warn("Unable to load place details", e);
               activePlace.setText("Unknown Place");
            })
            ;
      }
   }

   private void onSessionEvent(SessionEvent event) {
      setSessionInfo(client.getSessionInfo());
      if(event instanceof SessionAuthenticatedEvent) {
         
      }
      else if(event instanceof SessionExpiredEvent) {
         
      }
      else if(event instanceof SessionActivePlaceSetEvent) {
         setActivePlace(((SessionActivePlaceSetEvent) event).getPlaceId());
      }
      else {
         logger.warn("Unrecognized SessionEvent [" + event + "]");
      }
   }
}

