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
package com.iris.core.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.type.EmailRecipient;

public class Notifications {

   private static final String PLACE_PREFIX = "place:";

   public static class EmailChanged {
      public static final String KEY = "account.email.changed";
      public static final String PARAM_OLD_EMAIL = "oldemail";
   }

   public static class PasswordChanged {
      public static final String KEY = "account.password.changed";
   }

   public static class PasswordReset {
      public static final String KEY = "account.password.reset";
      public static final String PARAM_TOKEN = "token";
   }

   public static class AccountCreated {
      public static final String KEY = "account.created";
   }
   
   public static class AccountRemoved {
      public static final String KEY = "account.removed";
      public static final String PARAM_PERSON_FIRSTNAME = "personFirstName";
      public static final String PARAM_PERSON_LASTNAME = "personLastName";
   }
   
   public static class AccountEmailVerify {
      public static final String KEY = "account.email.verify";
      public static final String PARAM_TOKEN = "emailVerificationToken";
      public static final String PARAM_PLATFORM = "platform";
   }
   
   
   public static class BillingUpdated {
      public static final String KEY = "account.billing.updated";
   }

   public static class CCUpdated {
      public static final String KEY = "account.cc.updated";
   }

   public static class MobiledeviceAdded {
      public static final String KEY = "account.mobiledevice.added";
   }
   
   public static class MobiledeviceRemoved {
      public static final String KEY = "account.mobiledevice.removed";
   }
   
   public static class MobileNumberChanged {
      public static final String KEY = "account.mobilenumber.changed";
      public static final String PARAM_OLDMOBILENUMBER = "oldmobilenumber";
   }

   public static class PinChanged {
      public static final String KEY = "account.pin.changed";
   }

   public static class PlaceAdded {
      public static final String KEY = "account.place.added";
   }

   public static class DelinquentAccount {
      public static final String KEY = "account.delinquent.downgraded";
   }
   
   public static class DelinquentAccountPro {
      public static final String KEY = "account.delinquent.downgraded.pro";
   }
   
   public static class PersonInvitedToJoin {
      public static final String KEY = "account.person.invited";
      public static final String PARAM_INVITORFIRSTNAME = "invitorFirstName";
      public static final String PARAM_INVITORLASTNAME = "invitorLastName";
      public static final String PARAM_INVITEEFIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEELASTNAME = "inviteeLastName";
      public static final String PARAM_INVITATIONTEXT = "invitationText";
      public static final String PARAM_PERSONALIZEDGREETING = "personalizedGreeting";
      public static final String PARAM_CODE = "code";
   }

   //Person removed himself from a place, notify owner
   public static class PersonLeft {
      public static final String KEY = "account.person.left";
      public static final String PARAM_SECONDARY_FIRSTNAME = "secondaryContactFirstName";
      public static final String PARAM_SECONDARY_LASTNAME = "secondaryContactLastName";
   }
   

   public static class PersonInvitedToJoinReminder {
      public static final String KEY = "account.person.invited.reminder";
      public static final String PARAM_SECONDARY_FIRSTNAME = "secondaryContactFirstName";
      public static final String PARAM_SECONDARY_LASTNAME = "secondaryContactLastName";
      public static final String PARAM_INVITE_CODE = "inviteCode";
   }
   
   public static class PersonInvitationCancelled {
      public static final String KEY = "account.person.invited.cancelled";
      public static final String PARAM_PERSON_CANCELLED_FIRSTNAME = "personCancelledFirstName";
      public static final String PARAM_PERSON_CANCELLED_LASTNAME = "personCancelledLastName";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }
   
   public static class PersonInvitationCancelledNotifyInvitee {
      public static final String KEY = "account.person.invited.cancelled.notify.invitee";
      public static final String PARAM_INVITER_FIRSTNAME = "inviterFirstName";
      public static final String PARAM_INVITER_LASTNAME = "inviterLastName";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }

   public static class PersonInvitedToJoinNotifyOwner {
      public static final String KEY = "account.person.invited.notify.owner";
      public static final String PARAM_INVITER_FIRSTNAME = "inviterFirstName";
      public static final String PARAM_INVITER_LASTNAME = "inviterLastName";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }

   public static class PersonAcceptedToJoinNotifyOwner {
      public static final String KEY = "account.person.accepted.notify.owner";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }

   public static class PersonAcceptedToJoinNotifyInviter {
      public static final String KEY = "account.person.accepted.notify.inviter";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }

   public static class PersonDeclinedToJoinNotifyInviter {
      public static final String KEY = "account.person.declined.notify.inviter";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }
   
   public static class PersonDeclinedToJoinNotifyOwner {
      public static final String KEY = "account.person.declined.notify.owner";
      public static final String PARAM_INVITEE_FIRSTNAME = "inviteeFirstName";
      public static final String PARAM_INVITEE_LASTNAME = "inviteeLastName";
   }

   public static class HobbitAdded {
      public static final String KEY = "account.hobbit.added";
      public static final String PARAM_INVITER_FIRSTNAME = "inviterFirstName";
      public static final String PARAM_INVITER_LASTNAME = "inviterLastName";
   }

   //Person removed himself from a place
   public static class FullAccessPersonRemoved {
	   public static final String KEY = "account.person.removed";
   }
   
   //Owner or full access person removed a person's access from a place, notification sent to actor
   //TODO - need template
   public static class PersonRemovedByOther {
	   public static final String KEY = "account.person.removed.by.other";
	   public static final String PARAM_REMOVED_FIRSTNAME = "removedFirstName";
	   public static final String PARAM_REMOVED_LASTNAME = "removedLastName";
   }
   
   //TODO - need template
   public static class PersonRemovedByOtherNotifyOwner {
	   public static final String KEY = "account.person.removed.by.other.notify.owner";
	   public static final String PARAM_REMOVED_FIRSTNAME = "removedFirstName";
	   public static final String PARAM_REMOVED_LASTNAME = "removedLastName";
	   public static final String PARAM_ACTOR_FIRSTNAME = "actorFirstName";
	   public static final String PARAM_ACTOR_LASTNAME = "actorLastName";
   }
   
   public static class PlaceRemovedNotifyOwner {
	   public static final String KEY = "account.place.removed";
	   public static final String PARAM_PLACE_NAME = "placeName";  //Normally we do not need to pass in the place information, but in this case, the place entity is already removed.
   }
   
   public static class PlaceRemovedNotifyPerson {
	   public static final String KEY = "account.place.removed.to.person";
	   public static final String PARAM_PLACE_NAME = "placeName";  //Normally we do not need to pass in the place information, but in this case, the place entity is already removed.
	   public static final String PARAM_ACTOR_FIRSTNAME = "actorFirstName";  //Normally we do not need to pass in the place information, but in this case, the place entity is already removed.
	   public static final String PARAM_ACTOR_LASTNAME = "actorLastName";  //Normally we do not need to pass in the place information, but in this case, the place entity is already removed.
   }
   
   public static class SecurityAlertTriggered {
      public static final String KEY = "security.alert";
      public static final String PARAM_DEVICE_NAME = "deviceName";
      public static final String PARAM_DEVICE_TYPE = "deviceType";
   }
   
   
   public static class DeviceBatteryLow {
	  public static final String KEY = "hub.power.mains-2";
      public static final String PARAM_DEVICE_NAME = "deviceName";
      public static final String PARAM_BATTERY_TYPE = "batteryType";
   }
   
   public static class TouchFailed {
   	public static final String KEY = "logout.touch.failed"; 
   	public static final String PARAM_OS_TYPE = "osType";
   	public static final String PARAM_OS_VERSION = "osVersion";
   	public static final String PARAM_DEVICE_VENDOR = "deviceVendor";
   	public static final String PARAM_DEVICE_MODEL = "deviceModel";
   }
   
   public static Builder builder() { return new Builder(); }

   public static class Builder {
      private UUID personId;
      private UUID placeId;
      private String population;
      private String msgKey;
      private Map<String, String> params = new HashMap<>();
      private String priority;
      private Address source;
      private Integer ttl;

      private Builder() {}

      public Builder withPersonId(UUID personId) {
         this.personId = personId;
         return this;
      }

      public Builder withPlaceId(UUID placeId) {
         this.placeId = placeId;
         return this;
      }
      
      public Builder withPopulation(String population) {
         this.population = population;
         return this;
      }

      public Builder withMsgKey(String msgKey) {
         this.msgKey = msgKey;
         return this;
      }

      public Builder withPriority(String priority) {
         this.priority = priority;
         return this;
      }

      public Builder withSource(Address source) {
         this.source = source;
         return this;
      }

      public Builder withTimeToLive(int ttl) {
         this.ttl = ttl;
         return this;
      }

      public Builder addMsgParam(String key, String value) {
         params.put(key, value);
         return this;
      }

      public Builder addMsgParams(Map<String, String> params) {
         this.params.putAll(params);
         return this;
      }


      public PlatformMessage create() {
         Preconditions.checkState(personId != null, "A notification must have a person id.");
         Preconditions.checkState(!StringUtils.isEmpty(msgKey), "A notification must have a message key.");
         Preconditions.checkState(!StringUtils.isEmpty(priority), "A notification must have a priority.");
         Preconditions.checkState(source != null, "A notification must have a source.");

         NotificationCapability.NotifyRequest.Builder builder = NotificationCapability.NotifyRequest.builder()
               .withMsgKey(msgKey)
               .withPersonId(personId.toString())
               .withPriority(priority);

         if (placeId != null) {
            builder.withPlaceId(placeId.toString());
         }

         if (!params.isEmpty()) {
            builder.withMsgParams(params);
         }

         MessageBody notify = builder.build();

         PlatformMessage.Builder msgBuilder = PlatformMessage.buildMessage(notify,
               source,
               Address.platformService(NotificationCapability.NAMESPACE))
               .withCorrelationId(UUID.randomUUID().toString());

         if( ttl != null){
            msgBuilder.withTimeToLive(ttl);
         }
         
         if(StringUtils.isNotBlank(population)) {
         	msgBuilder.withPopulation(population);
         }

         PlatformMessage msg = msgBuilder.create();
         return msg;
      }
   }
   public static UUID getPlaceId(String notificationId){
      String removePlacePrefix=notificationId.replace(PLACE_PREFIX,"");
      return UUID.fromString(removePlacePrefix);
   }
   
   public static void sendEmailNotification(PlatformMessageBus bus, String personId, String placeId, String population, String msgKey, Map<String, String> params, Address source) {
		Builder builder = Notifications.builder()
              .withSource(source)
              .withPersonId(UUID.fromString(personId))
              .withPlaceId(UUID.fromString(placeId))
              .withPopulation(population)
              .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
              .withMsgKey(msgKey);
		if(params != null && params.size()>0) {
			builder.addMsgParams(params);
		}
		PlatformMessage msg1 = builder.create();               
	    bus.send(msg1);
	}
   
   public static void sendEmailNotification(PlatformMessageBus bus, EmailRecipient recipient, String placeId, String population, String msgKey, Map<String, String> params, Address source) {

	      MessageBody body = NotificationCapability.EmailRequest.builder()
	            .withRecipient(recipient.toMap())
	            .withMsgKey(msgKey)
	            .withMsgParams(params)
	            .withPlaceId(placeId)
	            .build();

	      PlatformMessage msg= PlatformMessage.buildMessage(
	            body,
	            source,
	            Address.platformService(NotificationCapability.NAMESPACE))
	            .withCorrelationId(UUID.randomUUID().toString())
	            .withPlaceId(placeId)
	            .withPopulation(population)
	            .create();

	      bus.send(msg);
	}
   
   
   public static String ensureNotNull(String value) {
	   if(value != null) {
		   return value;
	   }else {
		   return "";
	   }
   }
}

