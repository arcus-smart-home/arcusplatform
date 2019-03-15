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
package com.iris.ipcd.delivery;

import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.bridge.server.session.SessionUtil;
import com.iris.core.dao.PlaceDAO;
import com.iris.ipcd.session.IpcdSession;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.FactoryResetCommand;

@Singleton
public class DefaultIpcdDeliveryStrategy implements IpcdDeliveryStrategy {

   private final SessionRegistry sessionRegistry;
   private final BridgeMetrics metrics;
   private final PlaceDAO placeDao;

   @Inject
   public DefaultIpcdDeliveryStrategy(SessionRegistry sessionRegistry, BridgeMetrics metrics, PlaceDAO placeDao) {
      this.sessionRegistry = sessionRegistry;
      this.metrics = metrics;
      this.placeDao = placeDao;
   }

   @Override
   public void onIpcdMessage(ClientToken ct) {
      // intential no op
   }

   @Override
   public boolean deliverToDevice(ClientToken ct, String msgPlace, IpcdMessage message) {
      IpcdSession session = (IpcdSession) sessionRegistry.getSession(ct);
      if(session == null) {
         return false;
      }
      boolean isFactoryReset = message instanceof FactoryResetCommand;
      updateActivePlaceIf(session, msgPlace, isFactoryReset);
      session.sendMessage(message);
      handleFactoryResetIf(session, isFactoryReset);
      metrics.incProtocolMsgSentCounter();
      return true;
   }

   private void updateActivePlaceIf(IpcdSession session, String msgPlace, boolean isFactoryReset) {
      if(isFactoryReset) {
         return;
      }
      String activePlace = session.getActivePlace();
      if(activePlace == null) {
         SessionUtil.setPlace(msgPlace, session);
      } else if(!Objects.equals(activePlace, msgPlace)) {
         throw new IllegalStateException("session's active place " + activePlace + " does not match protocol message's " + msgPlace);
      }
   }

   private void handleFactoryResetIf(IpcdSession session, boolean isFactoryReset) {
      if(isFactoryReset) {
         session.onFactoryReset();
      }
   }
}

