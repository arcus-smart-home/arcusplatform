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
package com.iris.security.authz;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.*;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.*;
import com.iris.security.apikey.ApiKeyPrincipal;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;

/**
 * Decides whether a client's message is within the scope of its session. The destination comes
 * from the client, so anything that can be checked from the session's grants is checked here:
 * the actor is the account owner for sensitive messages, the person for self methods, and has a
 * grant on the addressed place or account.
 *
 * Objects whose owner needs a lookup are checked by their service, which has to compare the
 * object's place to the message's place header. See docs/security.md.
 */
@Singleton
public class RoleAuthorizer implements Authorizer {

   private static Logger logger = LoggerFactory.getLogger(RoleAuthorizer.class);
   
   @Inject(optional = true) @Named("authz.place.required")
   private boolean placeHeaderRequired = true;
   @Inject(optional = true) @Named("authz.self.required")
   private boolean selfRequired = true;
   
   private final AuthzMetrics metrics;
   private final DisabledRiskyOperations disabledOperations;

   @Inject
   public RoleAuthorizer(AuthzMetrics metrics, DisabledRiskyOperations disabledOperations) {
      this.metrics = metrics;
      this.disabledOperations = disabledOperations;
   }

   @Override
   public boolean isAuthorized(AuthorizationContext context, String placeId, PlatformMessage message) {
      String targetPlace = extractPlaceId(placeId, message);
      logger.trace("checking authorization for {} @ {} invoked by {}", message.getMessageType(), targetPlace, message.getActor().getRepresentation());
      Address actor = message.getActor();

      if(disabledOperations.isDisabled(message)) {
         this.metrics.onUnauthorizedDisabled();
         return deny("disabled", placeId, null, message);
      }

      // things that can bypass place header
      switch(message.getMessageType()) {

      // not allowed
      case AccountCapability.IssueInvoiceRefundRequest.NAME:
      case AccountCapability.IssueCreditRequest.NAME:
      case NotificationCapability.NotifyRequest.NAME:
      case NotificationCapability.NotifyCustomRequest.NAME:
      case NotificationCapability.EmailRequest.NAME:
      case PopulationService.AddPlacesRequest.NAME:
      case PopulationService.RemovePlacesRequest.NAME:
      //// clients create accounts through /account/CreateAccount, which doesn't come through here
      case MessageConstants.MSG_CREATE_ACCOUNT:
   		this.metrics.onUnauthorizedSupport();
         return deny("supportrequest", placeId, null, message);
      
      // requires owner
      //// billing
      case AccountCapability.AddPlaceRequest.NAME:
      case AccountCapability.CreateBillingAccountRequest.NAME:
      case AccountCapability.DeleteRequest.NAME:
      case AccountCapability.ListAdjustmentsRequest.NAME:
      case AccountCapability.ListInvoicesRequest.NAME:
      case AccountCapability.SignupTransitionRequest.NAME:
      case AccountCapability.SkipPremiumTrialRequest.NAME:
      case AccountCapability.UpdateBillingInfoCCRequest.NAME:
      case AccountCapability.UpdateServicePlanRequest.NAME:
      // NOTE AccountCapability.ListPlaces is allowed by anyone, it is re-interpreted to mean ListAvailablePlaces associated with this account
      //// deprecated but I suppose account owner's are allowed
      case AccountCapability.ListDevicesRequest.NAME:
      case AccountCapability.ListHubsRequest.NAME:
      //// sensitive
      case PlaceCapability.DeleteRequest.NAME:

         if(isAccountOwner(context, targetPlace) && ownsDestinationAccount(context, message)) {
            this.metrics.onAuthorized();
            return true;
         }
         else {
            this.metrics.onUnauthorizedNonAccountHolder();
            return deny("notowner", placeId, targetPlace, message);
         }

      // open to anyone
      case PlaceService.ValidateAddressRequest.NAME:
      //// anyone can send a ping
      case MessageConstants.MSG_PING_REQUEST:
         this.metrics.onAuthorized();
         return true;

      // self methods
      // SetAttributes to a person isn't one of these, people without a login are maintained by
      // others at their place, see PersonAccess
      //// invitations bypass the place header because a new login may not have a place yet
      case PersonCapability.AcceptInvitationRequest.NAME:
      case PersonCapability.PendingInvitationsRequest.NAME:
      case PersonCapability.RejectInvitationRequest.NAME:
      case PersonCapability.AddMobileDeviceRequest.NAME:
      case PersonCapability.DeleteLoginRequest.NAME:
      case PersonCapability.GetSecurityAnswersRequest.NAME:
      case PersonCapability.ListMobileDevicesRequest.NAME:
      case PersonCapability.ListAvailablePlacesRequest.NAME:
      case PersonCapability.PromoteToAccountRequest.NAME:
      case PersonCapability.RemoveMobileDeviceRequest.NAME:
      case PersonCapability.SetSecurityAnswersRequest.NAME:
         if(selfRequired && !isSelf(context, message)) {
            this.metrics.onUnauthorizedWrongPerson();
            return deny("wrongperson", placeId, null, message);
         }
         else {
            this.metrics.onAuthorized();
            return true;
         }

      }
      
      // require a place header
      if(placeHeaderRequired && StringUtils.isEmpty(placeId)) {
         this.metrics.onUnauthorizedNoPlace();
         return deny("noplace", placeId, null, message);
      }
      
      // service requests
      if(message.getDestination().getId() == null || Address.ZERO_UUID.equals(message.getDestination().getId())) {
      	switch(message.getMessageType()) {
      	case RuleService.ListRulesRequest.NAME:
      	case SceneService.ListScenesRequest.NAME:
      	case SceneService.ListSceneTemplatesRequest.NAME:
      	case SchedulerService.ListSchedulersRequest.NAME:
      	case SubsystemService.ListSubsystemsRequest.NAME:
      	case VideoService.ListRecordingsRequest.NAME:
      	case VideoService.PageRecordingsRequest.NAME:
      	case VideoService.StartRecordingRequest.NAME:
      	case VideoService.StopRecordingRequest.NAME:
      	case VideoService.GetQuotaRequest.NAME:
      	case VideoService.DeleteAllRequest.NAME:
      		String requestedPlaceId = (String) message.getValue().getAttributes().get("placeId");
      		UUID placeUuid = parsePlaceId(requestedPlaceId);
      		if(placeUuid == null || !hasAccessToPlace(placeUuid, context)) {
               this.metrics.onUnauthorizedWrongPlace();
               return deny("wrongplace", placeId, requestedPlaceId, message);
            }
      		break;
      		
   		// FIXME determine what (if any) IPCD requests belong here
   		
   		
      	}
   	}
      
      // place settings requests
      else if(
      		PlaceCapability.NAMESPACE.equals(message.getDestination().getGroup())
		) {
         UUID placeUuid = (UUID) message.getDestination().getId();
         if(!hasAccessToPlace(placeUuid, context)) {
      		this.metrics.onUnauthorizedWrongPlace();
      		return deny("wrongplace", placeId, placeUuid, message);
         }
      }

      // account requests
      else if(AccountCapability.NAMESPACE.equals(message.getDestination().getGroup())) {
         if(!hasAccessToAccount(message.getDestination().getId(), context)) {
            this.metrics.onUnauthorizedWrongPlace();
            return deny("wrongplace", placeId, null, message);
         }
      }

      // a mobile device is addressed by the id of the person it belongs to
      else if(MobileDeviceCapability.NAMESPACE.equals(message.getDestination().getGroup())) {
         if(selfRequired && !isActor(message.getDestination().getId(), message)) {
            this.metrics.onUnauthorizedWrongPerson();
            return deny("wrongperson", placeId, null, message);
         }
      }

      // the place is in the body, so the destination checks above don't cover it. a blank place
      // means the active place
      if(PersonCapability.RemoveFromPlaceRequest.NAME.equals(message.getMessageType())) {
         String requestedPlaceId = PersonCapability.RemoveFromPlaceRequest.getPlaceId(message.getValue());
         UUID placeUuid = parsePlaceId(requestedPlaceId);
         if(placeUuid != null && !hasAccessToPlace(placeUuid, context)) {
            this.metrics.onUnauthorizedWrongPlace();
            return deny("wrongplace", placeId, requestedPlaceId, message);
         }
      }

      this.metrics.onAuthorized();
      return true;
   }

   private UUID parsePlaceId(String placeId) {
      try {
         return StringUtils.isEmpty(placeId) ? null : UUID.fromString(placeId);
      }
      catch(Exception e) {
         throw new ErrorEventException(Errors.invalidParam("placeId"), e);
      }
   }

   // reason matches the bridge.authz.unauthorized.<reason> counter so log lines and metrics line up
   private boolean deny(String reason, String activePlace, Object requestedPlace, PlatformMessage message) {
      logger.warn("authz denied reason={} type={} actor={} destination={} activePlace={} requestedPlace={}",
            reason, clean(message.getMessageType()), message.getActor(), clean(message.getDestination()), activePlace, clean(requestedPlace));
      return false;
   }

   // type, destination and requested place come from the client, so strip CR/LF to keep a request from forging log lines
   private static String clean(Object value) {
      return value == null ? null : StringUtils.replaceChars(value.toString(), "\r\n", "__");
   }

   private boolean isSelf(AuthorizationContext context, PlatformMessage message) {
      if(message.getActor() == null) {
         return false;
      }
      if(message.getActor().equals(message.getDestination())) {
         return true;
      }
      // an API key's session reports its creator as the person, and clients list places through
      // them. ApiPlatformBusListener narrows the response to the key's own place.
      return PersonCapability.ListAvailablePlacesRequest.NAME.equals(message.getMessageType())
            && context.getPrincipal() instanceof ApiKeyPrincipal
            && Objects.equal(((ApiKeyPrincipal) context.getPrincipal()).getPersonId(), message.getDestination().getId());
   }

   private boolean isActor(Object personId, PlatformMessage message) {
      return message.getActor() != null
            && PersonCapability.NAMESPACE.equals(message.getActor().getGroup())
            && Objects.equal(message.getActor().getId(), personId);
   }

   private String extractPlaceId(String sessionPlaceId, PlatformMessage msg) {
      switch(msg.getMessageType()) {
      case PlaceCapability.DeleteRequest.NAME:
         return String.valueOf(msg.getDestination().getId());
      case AccountCapability.UpdateServicePlanRequest.NAME:
         return AccountCapability.UpdateServicePlanRequest.getPlaceID(msg.getValue());
      default:
         return sessionPlaceId;
      }
   }

   private boolean isAccountOwner(AuthorizationContext context, String placeId) {
      return context.getGrants().stream().anyMatch((g) -> {
         return Objects.equal(g.getPlaceId().toString(), placeId) && g.isAccountOwner();
      });
   }
   
   // the owner checks above look at the active place, which on its own says nothing about an
   // account that was addressed directly
   private boolean ownsDestinationAccount(AuthorizationContext context, PlatformMessage message) {
      Address destination = message.getDestination();
      if(!AccountCapability.NAMESPACE.equals(destination.getGroup())) {
         return true;
      }
      return context.getGrants().stream().anyMatch((g) -> {
         return g.isAccountOwner() && Objects.equal(g.getAccountId(), destination.getId());
      });
   }

   private boolean hasAccessToAccount(Object accountId, AuthorizationContext context) {
      return context.getGrants().stream().anyMatch((g) -> Objects.equal(g.getAccountId(), accountId));
   }

   private boolean hasAccessToPlace(UUID placeId, AuthorizationContext context) {
   	Collection<?> permissions = context.getNonInstancePermissions(placeId);
   	if(permissions != null && !permissions.isEmpty()) {
   		return true;
   	}
   	permissions = context.getInstancePermissions(placeId);
   	if(permissions != null && !permissions.isEmpty()) {
   		return true;
   	}
   	
 		return false;
   }

   @Override
   public PlatformMessage filter(AuthorizationContext context, String placeId, PlatformMessage message) {
      logger.trace("Skipping filtering [{}] for [{}]", message, context.getSubjectString());
      return message;
   }
}

