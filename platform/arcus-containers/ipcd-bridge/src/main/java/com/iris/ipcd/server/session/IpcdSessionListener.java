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
package com.iris.ipcd.server.session;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoCommand;

@Singleton
public class IpcdSessionListener implements SessionListener {
   private final static Logger logger = LoggerFactory.getLogger(IpcdSessionListener.class);

   @Override
   public void onSessionCreated(Session session) {
      logger.debug("onSessionCreated Called with [{}]", session);
      // When an Ipcd Session is created we know nothing about the device so send a GetDeviceInfo request to
      // gather data.
      String txnid = UUID.randomUUID().toString();
      GetDeviceInfoCommand getDeviceInfo = new GetDeviceInfoCommand();
      getDeviceInfo.setTxnid(txnid);
      IpcdSocketSession ipcdSession = (IpcdSocketSession)session;
      ipcdSession.addTxnid(txnid); // The session needs to know to intercept this message.  
      logger.debug("exit onSessionCreated [{}]", session);
   }

   @Override
   public void onSessionDestroyed(Session session) {
      // no op
   }
}

