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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.UUID;

/**
 * Currently only checks that the actor is the account owner for a select number of messages that
 * would impact billing.
 */
@Singleton
public class RoleAuthorizer implements Authorizer {

   private static Logger logger = LoggerFactory.getLogger(RoleAuthorizer.class);
   
   @Inject(optional = true) @Named("authz.place.required")
   private boolean placeHeaderRequired = true;
   @Inject(optional = true) @Named("authz.self.required")
   private boolean selfRequired = true;
   
   private final AuthzMetrics metrics;
   
   @Inject
   public RoleAuthorizer(AuthzMetrics metrics) {
      this.metrics = metrics;
   }

   @Override
   public boolean isAuthorized(AuthorizationContext context, String placeId, PlatformMessage message) {
      String targetPlace = extractPlaceId(placeId, message);
      logger.trace("checking authorization for {} @ {} invoked by {}", message.getMessageType(), targetPlace, message.getActor().getRepresentation());
      Address actor = message.getActor();

      // things that can bypass place header
      switch(message.getMessageType()) {
      
      // not allowed
      case AccountCapability.IssueInvoiceRefundRequest.NAME:
      case AccountCapability.IssueCreditRequest.NAME:
      case NotificationCapability.NotifyRequest.NAME:
      case NotificationCapability.NotifyCustomRequest.NAME:
      case NotificationCapability.EmailRequest.NAME:
   		this.metrics.onUnauthorizedSupport();
         return false;
      
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

         if(isAccountOwner(context, targetPlace)) {
            this.metrics.onAuthorized();
            return true;
         }
         else {
            this.metrics.onUnauthorizedNonAccountHolder();
            return false;
         }
         
      // open to anyone
      //// invitations can be handled by anyone who has the token & the email
      case PersonCapability.AcceptInvitationRequest.NAME:
      case PersonCapability.PendingInvitationsRequest.NAME: 
      case PersonCapability.RejectInvitationRequest.NAME:
      case PlaceService.ValidateAddressRequest.NAME:
      //// anyone can send a ping
      case MessageConstants.MSG_PING_REQUEST:
         this.metrics.onAuthorized();
         return true;
         
      // self methods
      // FIXME should this include SetAttributes to person?
      case PersonCapability.AddMobileDeviceRequest.NAME:
      case PersonCapability.DeleteLoginRequest.NAME:
      case PersonCapability.GetSecurityAnswersRequest.NAME:
      case PersonCapability.ListMobileDevicesRequest.NAME:
      case PersonCapability.ListAvailablePlacesRequest.NAME:
      case PersonCapability.PromoteToAccountRequest.NAME:
      case PersonCapability.RemoveMobileDeviceRequest.NAME:
      case PersonCapability.SetSecurityAnswersRequest.NAME:
         if(selfRequired && !isSelf(message)) {
            this.metrics.onUnauthorizedWrongPerson();
            return false;
         }
         else {
            this.metrics.onAuthorized();
            return true;
         }

      }
      
      // require a place header
      if(placeHeaderRequired && StringUtils.isEmpty(placeId)) {
         this.metrics.onUnauthorizedNoPlace();
         return false;
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
      		UUID placeUuid = null;
      		try{
         		placeUuid = StringUtils.isEmpty(requestedPlaceId) ? null : UUID.fromString(requestedPlaceId);         		
      		}catch(Exception e) {
      		   throw new ErrorEventException(Errors.invalidParam("placeId"), e);
      		}
      		if(placeUuid == null || !hasAccessToPlace(placeUuid, context)) {
               this.metrics.onUnauthorizedWrongPlace();
               return false;
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
      		return false;
         }
      }
      
      this.metrics.onAuthorized();
      return true;
   }

   private boolean isSelf(PlatformMessage message) {
      if(message.getActor() == null) {
         return false;
      }
      return message.getActor().equals(message.getDestination());
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

