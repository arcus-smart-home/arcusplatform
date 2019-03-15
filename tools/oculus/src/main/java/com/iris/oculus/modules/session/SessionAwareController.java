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
package com.iris.oculus.modules.session;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.iris.client.session.SessionInfo;
import com.iris.oculus.modules.session.event.PlaceChangedEvent;
import com.iris.oculus.modules.session.event.SessionAuthenticatedEvent;
import com.iris.oculus.modules.session.event.SessionEvent;
import com.iris.oculus.modules.session.event.SessionExpiredEvent;

/**
 * 
 */
public class SessionAwareController {
   private SessionController sessions;
   private String previousPlaceId = null;
   
   /**
    * 
    */
   public SessionAwareController() {
   }

   @Inject
   protected void setSessionController(SessionController sessions) {
      this.sessions = sessions;
      this.sessions.addSessionListener((e) -> this.onSessionEvent(e));
      if(this.sessions.isSessionActive()) {
         // do this on the swing thread
         SwingUtilities.invokeLater(() -> {
            this.onSessionInitialized(this.sessions.getSessionInfo());
         });
         OculusSession info = this.sessions.getSessionInfo();
         if(info.getPlaceId() != null) {
            SwingUtilities.invokeLater(() -> {
               this.onPlaceSet(info.getPlaceId());
            });
         }
      }
   }

   protected OculusSession getSessionInfo() throws IllegalStateException {
      return sessions.getSessionInfo();
   }
   
   protected String getPlaceId() throws IllegalStateException {
      return sessions.getSessionInfo().getPlaceId();
   }
   
   protected boolean isSessionActive() {
      return this.sessions.isSessionActive();
   }
   
   protected void onSessionEvent(SessionEvent event) {
      if(event instanceof PlaceChangedEvent) {
         onPlaceSet(sessions.getSessionInfo().getPlaceId());
      }
      else if(event instanceof SessionAuthenticatedEvent) {
         OculusSession info = sessions.getSessionInfo();
         onSessionInitialized(info);
         if(info.getPlaceId() != null) {
         	onPlaceSet(info.getPlaceId());
         }
      }
      else if(event instanceof SessionExpiredEvent) {
         onSessionExpired();
      }
   }
   
   private void onPlaceSet(String placeId) {
      if(previousPlaceId == null || !previousPlaceId.equals(placeId)) {
         previousPlaceId = placeId;
         onPlaceChanged(placeId);
      }
   }
   
   /**
    * Invoked when the system is initially loaded or when a user
    * logs in after the session expired.  Base implementation is
    * a no-op.
    * @param info
    */
   protected void onSessionInitialized(OculusSession info) {
      
   }
   
   /**
    * Invoked when an authenticated session switches to a new
    * place. 
    * Base implementation is a no-op.
    * @param newPlaceId
    */
   protected void onPlaceChanged(String newPlaceId) {
      
   }
   
   /**
    * Called when the user logs out or the session expires
    * for any other reason.
    * Base implementation is a no-op.
    */
   protected void onSessionExpired() {
      
   }
}

