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
package com.iris.netty.bus;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.UUID;

import org.apache.shiro.session.UnknownSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.netty.BridgeMdcUtil;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.bridge.server.session.SessionUtil;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability.AddPlaceResponse;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.PreferencesChangedEvent;
import com.iris.messages.service.VideoService;
import com.iris.messages.services.PlatformConstants;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.security.authz.Authorizer;
import com.iris.util.MdcContext.MdcContextReference;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class IrisNettyPlatformBusListener implements PlatformBusListener {
   private static final Logger logger = LoggerFactory.getLogger(IrisNettyPlatformBusListener.class);
   private static final Address SessionServiceAddress = Address.platformService(SessionService.NAMESPACE);
   
   public static final AttributeKey<MessageBody> CACHED_LIST_RECORDINGS_RESPONSE = AttributeKey.newInstance("ListRecordingsRequestCache");

   private final Authorizer authorizer;
   private final IrisNettyMessageUtil messageUtil;
   private final SessionRegistry sessionRegistry;

   @Inject
   public IrisNettyPlatformBusListener(Authorizer authorizer, IrisNettyMessageUtil messageUtil, SessionRegistry sessionRegistry) {
      this.authorizer = authorizer;
      this.messageUtil = messageUtil;
      this.sessionRegistry = sessionRegistry;
   }

   @Override
   public void onMessage(ClientToken ct, PlatformMessage msg) {
      if(SessionService.SessionExpiredEvent.NAME.equals(msg.getMessageType()) && SessionServiceAddress.equals(msg.getSource())) {
         String sessionId = SessionService.SessionExpiredEvent.getSessionId(msg.getValue());
         Errors.assertRequiredParam(sessionId, SessionService.SessionExpiredEvent.ATTR_SESSIONID);
         logger.debug("Received session expired event from [{}] for [{}]", msg.getSource(), sessionId);

         for(Session session: sessionRegistry.getSessions()) {
            Client client = session.getClient();
            if(client == null) {
               continue;
            }

            if(sessionId.equals(client.getSessionId())) {
               client.logout();
            }
         }
      }
      else if (PreferencesChangedEvent.NAME.equals(msg.getMessageType()) && SessionServiceAddress.equals(msg.getSource()))
      {
         Errors.assertRequiredParam(msg.getActor(), "actor");
         Errors.assertRequiredParam(msg.getPlaceId(), "placeId");

         UUID eventPersonUuid = (UUID) msg.getActor().getId();
         String eventPlaceId = msg.getPlaceId();

         logger.debug("Received preferences changed event from [{}] for person [{}], place [{}]",
            msg.getSource(), eventPersonUuid, eventPlaceId);

         ClientMessage clientMsg = ClientMessage.builder()
            .withSource(msg.getSource().getRepresentation())
            .withPayload(msg.getValue())
            .create();
         String clientMsgJson = JSON.toJson(clientMsg);

         for (Session session : sessionRegistry.getSessions())
         {
            Client client = session.getClient();
            if (client == null) continue;

            UUID personUuid = client.getPrincipalId();
            if (personUuid == null) continue;

            String placeId = session.getActivePlace();
            if (isEmpty(placeId)) continue;

            if (eventPersonUuid.equals(personUuid) && eventPlaceId.equals(placeId))
            {
               logger.debug("Sending message to client for person [{}], place [{}]: [{}]",
                  personUuid, placeId, clientMsgJson);

               session.sendMessage(clientMsgJson);
            }
         }
      }
      else if(PersonCapability.PasswordChangedEvent.NAME.equals(msg.getMessageType())) {
         bootSessionsFor(msg.getSource(), PersonCapability.PasswordChangedEvent.getSession(msg.getValue()));
      }
      // If there is no client token, then this is a broadcast message going to everybody.
      else if (ct == null) {
         Iterable<Session> sessions = sessionRegistry.getSessions();
         boolean deleted = Capability.EVENT_DELETED.equals(msg.getMessageType());
         boolean personAdded = Capability.EVENT_ADDED.equals(msg.getMessageType()) && PersonCapability.NAMESPACE.equals(msg.getSource().getGroup());
         boolean authRemoved = PersonCapability.AuthorizationRemovedEvent.NAME.equals(msg.getMessageType());
         if (deleted || authRemoved) {
            try (MdcContextReference ref = Message.captureAndInitializeContext(msg)) {
               for (Session session : sessions) {
                  if (deleted) {
                     onDeleted(session, msg);
                  }

                  if (personAdded) {
                     onPersonAdded(session, msg);
                  }

                  if (authRemoved) {
                     onAuthorizationRemoved(session, msg);
                  }
               }
            }
         }

         for (Session session : sessions) {
            boolean attemptSend = session.getActivePlace() != null && Objects.equals(session.getActivePlace(), msg.getPlaceId());
            String clientMsg = attemptSend ? filter(session,msg) : null;

            if (clientMsg != null) {
               try(MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(session)) {
                  if (clientMsg != null) {
                     session.sendMessage(clientMsg);
                  }
               }
            }
         }
      } else {
         Session session = sessionRegistry.getSession(ct);
         if (session != null) {
            String clientMsg = filter(session,msg);
            if (clientMsg != null) {
               try(MdcContextReference ref = BridgeMdcUtil.captureAndInitializeContext(session)) {
                  if(AddPlaceResponse.NAME.equals(msg.getMessageType())) {
                     // if a place has been successfully added, the caller should have access to it
                     session.getAuthorizationContext(true);
                  }
                  session.sendMessage(clientMsg);
               }
            }
         }
      }
   }

   private void bootSessionsFor(Address personAddr, String excludeSession) {
      if(personAddr.getId() == null || !(personAddr.getId() instanceof UUID)) {
         return;
      }

      UUID personId = (UUID) personAddr.getId();

      Iterable<Session> sessions = sessionRegistry.getSessions();
      sessions.forEach(s -> {
         if(
            !Objects.equals(s.getClient().getSessionId(), excludeSession) &&
            Objects.equals(s.getClient().getPrincipalId(), personId)
         ) {
            s.getClient().logout();
         }
      });
   }

   // TODO:  more efficient way to do this
   private void onDeleted(Session session, PlatformMessage msg) {
      if(PlatformConstants.SERVICE_PLACES.equals(msg.getSource().getGroup())) {
         clearRemovedActivePlace(session, (UUID) msg.getSource().getId());
      } else if(PlatformConstants.SERVICE_PEOPLE.equals(msg.getSource().getGroup())) {
         bootRemovedPerson(session, msg);
      }
   }

   private void onPersonAdded(Session session, PlatformMessage msg) {
      UUID personId = (UUID) msg.getSource().getId();
      if(Objects.equals(session.getAuthorizationContext().getPrincipal().getUserId(), personId)) {
         logger.debug("Updated auth context [{}] because person was added to a new place", session.getClientToken());
         session.getAuthorizationContext(true);
      }
   }

   private void onAuthorizationRemoved(Session session, PlatformMessage msg) {
      UUID personId = (UUID) msg.getSource().getId();
      if(Objects.equals(session.getAuthorizationContext().getPrincipal().getUserId(), personId)) {
         MessageBody body = msg.getValue();
         String placeId = PersonCapability.AuthorizationRemovedEvent.getPlaceId(body);
         if(Objects.equals(session.getActivePlace(), placeId)) {
            logger.debug("Clearing active place for client session [{}] because authorization was removed", session.getClientToken());
            SessionUtil.clearPlace(session);
            session.getAuthorizationContext(true);
            writePlaceCleared(session, placeId, "Access to place removed");
         }
      }
   }

   private void writePlaceCleared(Session session, String placeId, String reason) {
      MessageBody body = SessionService.ActivePlaceClearedEvent.builder()
            .withReason(reason)
            .withPlaceId(placeId)
            .build();
      ClientMessage msg =ClientMessage.builder()
               .withPayload(body)
               .withSource(Address.platformService(SessionService.NAMESPACE).getRepresentation())
               .create();
      session.sendMessage(JSON.toJson(msg));
   }

   private void clearRemovedActivePlace(Session session, UUID placeId) {
      if(Objects.equals(session.getActivePlace(), placeId.toString())) {
         logger.debug("Clearing active place for client session [{}] because the place was deleted", session.getClientToken());
         SessionUtil.clearPlace(session);         
         writePlaceCleared(session, placeId.toString(), "Place removed");
      }
   }

   private void bootRemovedPerson(Session session, PlatformMessage msg) {
      UUID personId = (UUID) msg.getSource().getId();
      if(Objects.equals(session.getAuthorizationContext().getPrincipal().getUserId(), personId)) {
         MessageBody body = msg.getValue();
         if(Boolean.TRUE.equals(body.getAttributes().get("bootSession"))) {
            logger.debug("Destroying client session [{}] because the logged in person has been deleted", session.getClientToken());
            session.disconnect(Constants.SESSION_EXPIRED_STATUS);
            session.destroy();
         }
      }
   }

   private String filter(Session session, PlatformMessage msg) {
      try {
         PlatformMessage filtered = authorizer.filter(session.getAuthorizationContext(), session.getActivePlace(), msg);
         if(filtered != null) {
            
            Address source = msg.getSource();
            if (source != null && MessageConstants.SERVICE.equals(source.getNamespace()) && VideoService.NAMESPACE.equals(source.getGroup())) {
               Channel ch = session.getChannel();
               Attribute<MessageBody> attr = (ch != null) ? ch.attr(CACHED_LIST_RECORDINGS_RESPONSE) : null;
               if (attr != null) {
                  switch (filtered.getMessageType()) {
                  case VideoService.ListRecordingsResponse.NAME:
                     attr.set(msg.getValue());
                     break;

                  case VideoService.DeleteAllResponse.NAME:
                  case Capability.EVENT_ADDED:
                  case Capability.EVENT_DELETED:
                  case Capability.EVENT_VALUE_CHANGE:
                     attr.remove();
                     break;

                  default:
                     break;
                  }
               }
            }

            return JSON.toJson(messageUtil.convertPlatformToClient(filtered));
         } else {
            logger.debug("Dropped message [{}]:  msg place {} != session place {}", msg, msg.getPlaceId(), session.getActivePlace());
         }
      }
      catch (UnknownSessionException use){
         logger.debug("Session expired by shiro:  [{}] and session [{}]", msg, session.getClientToken(), use);
         session.disconnect(Constants.SESSION_EXPIRED_STATUS);
         session.destroy();
      }  
      catch (IllegalStateException ise){
    	  if (session != null) {
    		  logger.debug("Attempted to send message to disconnected client: [{}] and session [{}]", msg, session.getClientToken(), ise);
    		  session.destroy();
    	  } else {
    		  logger.debug("Attempted to send message to disconnected client: [{}]", msg, ise);
    	  }
      }
      catch(Exception e) {
         logger.debug("Unable to dispatch message [{}] to session [{}]", msg, session.getClientToken(), e);
      }

      return null;
   }
}

