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
package com.iris.oculus.modules.status;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.impl.MessageConstants;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.status.MessageEvent.Level;

/**
 * @author tweidlin
 *
 */
@Singleton
public class StatusController {
   private static final Logger logger = LoggerFactory.getLogger("com.iris.oculus.Application");
   
   private IrisClient client;
   private int responseTimeoutMs = 5000;
   private ListenerList<MessageEvent> handler = new ListenerList<MessageEvent>();
   private Set<Level> enabled;
   
   @Inject
   public StatusController(IrisClient client) {
      this.client = client;
   }
   
   @PostConstruct
   public void init() {
      String [] levels = Oculus.getPreference("loglevel", "INFO,WARN,ERROR").split("\\,");
      enabled = EnumSet.noneOf(Level.class);
      for(String level: levels) {
         try {
            enabled.add(Level.valueOf(level));
         }
         catch(IllegalArgumentException e) {
            // ignore
         }
      }
   }
   
   // TODO be able to ping other platform services
   
   public ClientFuture<ClientEvent> pingPlatformService() {
      ClientRequest request = new ClientRequest();
      request.setAddress("SERV:status:"); //PlatformDestination.STATUS_DESTINATION);
//      request.setAddress(PlatformDestination.STATUS_DESTINATION);
      request.setCommand(MessageConstants.MSG_PING_REQUEST);
      request.setTimeoutMs(responseTimeoutMs);
      return client.request(request);
   }
   
   public ClientFuture<ClientEvent> pingDriverService() {
      ClientRequest request = new ClientRequest();
      request.setAddress("DRV::00000000-0000-0000-0000-000000000000");
      request.setCommand(MessageConstants.MSG_PING_REQUEST);
      request.setTimeoutMs(responseTimeoutMs);
      return client.request(request);
   }
   
   // TODO just expose an application Logger?
   
   public void trace(String message) {
      logger.trace(message);
      fireMessageEvent(Level.TRACE, message);
   }
   
   public void debug(String message) {
      logger.debug(message);
      fireMessageEvent(Level.DEBUG, message);
   }
   
   public void debug(String message, Throwable cause) {
      logger.debug(message, cause);
      fireMessageEvent(Level.DEBUG, message, cause);
   }

   public void info(String message) {
      logger.info(message);
      fireMessageEvent(Level.INFO, message);
   }
   
   public void info(String message, Throwable e) {
      logger.info(message, e);
      fireMessageEvent(Level.INFO, message, e);
   }
   
   public void info(String message, Action a) {
      logger.info(message);
      fireMessageEvent(Level.INFO, message, a);
   }
   
   public void warn(String message) {
      logger.warn(message);
      fireMessageEvent(Level.WARN, message);
   }
   
   public void warn(String message, Throwable e) {
      logger.warn(message, e);
      fireMessageEvent(Level.WARN, message, e);
   }
   
   public void warn(String message, Action a) {
      logger.warn(message);
      fireMessageEvent(Level.INFO, message, a);
   }
   
   public void error(String message) {
      logger.error(message);
      fireMessageEvent(Level.ERROR, message);
   }
   
   public void error(String message, Throwable e) {
      logger.error(message, e);
      fireMessageEvent(Level.ERROR, message, e);
   }
   
   public void error(String message, Action a) {
      logger.error(message);
      fireMessageEvent(Level.INFO, message, a);
   }
   
   protected void fireMessageEvent(Level level, String message) {
      if(!enabled.contains(level)) {
         return;
      }
      handler.fireEvent(new MessageEvent(level, message));
   }

   protected void fireMessageEvent(Level level, String message, Action a) {
      if(!enabled.contains(level)) {
         return;
      }
      handler.fireEvent(new MessageEvent(level, message, a));
   }

   protected void fireMessageEvent(Level level, String message, Throwable e) {
      if(!enabled.contains(level)) {
         return;
      }
      handler.fireEvent(new MessageEvent(level, message, new ShowExceptionAction(e)));
   }

   public ListenerRegistration addMessageListener(Listener<? super MessageEvent> listener) {
      return this.handler.addListener(listener);
   }

}

