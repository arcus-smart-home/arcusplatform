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
package com.iris.hubcom.bus;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.server.netty.BridgeMdcUtil;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.hubcom.authz.HubMessageFilter;
import com.iris.hubcom.server.session.HubSession;
import com.iris.io.Serializer;
import com.iris.io.json.JSON;
import com.iris.messages.HubMessage;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CellBackupSubsystemCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.util.MdcContext.MdcContextReference;

public class HubPlatformBusListener implements PlatformBusListener {
	private final static Logger logger = LoggerFactory.getLogger(HubPlatformBusListener.class);

	private final static int BANNED_DISCONNECT_CODE = 4655;

	private final HubMessageFilter filter;
	private final SessionRegistry sessionRegistry;
	private final Serializer<PlatformMessage> platformSerializer;
	private final Serializer<HubMessage> hubSerializer;

	@Inject
	public HubPlatformBusListener(HubMessageFilter filter, SessionRegistry sessionRegistry) {
	   this.filter = filter;
	   this.sessionRegistry = sessionRegistry;
	   this.platformSerializer = JSON.createSerializer(PlatformMessage.class);
	   this.hubSerializer = JSON.createSerializer(HubMessage.class);
	}

	@Override
	public void onMessage(ClientToken ct, PlatformMessage msg) {
		Session session = sessionRegistry.getSession(ct);
		try(MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(session, msg)) {
   		// these are handled by the platform
   		if(HubCapability.DeleteRequest.NAME.equals(msg.getMessageType())) {
   		   return;
   		}
   
   		if(session == null) {
   	      // these are owned by another bridge or hub-service
   		   return;
   		}
   
   		switch(msg.getMessageType()) {
   		case CellBackupSubsystemCapability.CellAccessBannedEvent.NAME:
   		case CellBackupSubsystemCapability.CellAccessUnbannedEvent.NAME:
   		   if(Objects.equals(HubNetworkCapability.TYPE_3G, ((HubSession) session).getConnectionType())) {
   		      session.disconnect(BANNED_DISCONNECT_CODE);
   		      session.destroy();
   		   }
   		   return;
   		default:
   		   /* no op */
   		}
   
   		boolean accepted = filter.acceptFromPlatform((HubSession) session, msg);
   		if(!accepted) {
   		   // increment a counter;
   		   return;
   		}
   
   		byte[] payload = platformSerializer.serialize(msg);
   		byte[] message = hubSerializer.serialize(HubMessage.createPlatform(payload));
   		session.sendMessage(message);
		}
	}

}

