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
package com.iris.notification.provider;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.provider.apns.ApnsSender;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.iris.platform.notification.provider.NotificationProviderUtil;
import com.iris.util.TypeMarker;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;

@Singleton
public class ApnsProvider extends AbstractPushNotificationProvider implements NotificationProvider {

	private static final String ADDRESS_PARAM_KEY = "_address";
	private static final String TARGET_PARAM_KEY = "_target";
	
    @Inject
    private NotificationMessageRenderer messageRenderer;

    @Inject
    private ApnsSender sender;

    @Inject
    private NotificationAuditor auditor;

    @Inject
    private RetryProcessor retryProcessor;
    
    @Inject
    private PersonDAO personDao;
    
    @Inject
    private PlaceDAO placeDao;
    
    @Inject
    private AccountDAO accountDao;

    @Override
    public void notifyCustomer(Notification notification) throws DispatchUnsupportedByUserException {
        
    	 // Collect recipient information
    	Map<String, BaseEntity<?, ?>> additionaEntityParams = NotificationProviderUtil.addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, notification);
        Person recipient = NotificationProviderUtil.getPersonFromParams(additionaEntityParams);
        if (recipient == null) {
            throw new DispatchUnsupportedByUserException("No person found with id: " + notification.getPersonId());
        }
        
    	// If delivery endpoint has been set, then this notification represents a message to a single physical device...
       if (notification.getDeliveryEndpoint() != null) {
          // Render message and build APNS payload
          String notificationMessage = messageRenderer.renderMessage(notification, NotificationMethod.APNS, recipient, additionaEntityParams);
          final String payload = generatePayload(notification, notificationMessage, additionaEntityParams);
          sendNotificationToDevice(notification, notification.getDeliveryEndpoint(), payload);
       }

        // ... otherwise, we should send the message to each device associated with this person
        else {
            for (String thisToken : getDeviceTokensForPerson(notification.getPersonId(), MobileOS.IOS)) {
                retryProcessor.split(notification, NotificationMethod.APNS, thisToken);
            }
        }
    }

    private String generatePayload(Notification notification, String notificationMessage, Map<String,BaseEntity<?,?>> additionalEntityParams) {
       Map<String,String> msg = JSON.fromJson(notificationMessage, TypeMarker.mapOf(String.class));
       ApnsPayloadBuilder builder = new ApnsPayloadBuilder()
          .setAlertBody(msg.get("body"))
          .setAlertTitle(msg.get("title"))       
          .setSound(ApnsPayloadBuilder.DEFAULT_SOUND_FILENAME)
          .setContentAvailable(true);
          
       // Add data for deep-link messages when such data is present
       if (notification.getMessageParams() != null && notification.getMessageParams().containsKey(ADDRESS_PARAM_KEY)) {
    	   builder.addCustomProperty("address", notification.getMessageParams().get(ADDRESS_PARAM_KEY));
       }
       if (notification.getMessageParams() != null && notification.getMessageParams().containsKey(TARGET_PARAM_KEY)) {
    	   builder.addCustomProperty("target", notification.getMessageParams().get(TARGET_PARAM_KEY));
       }
       if (additionalEntityParams.containsKey(NotificationProviderUtil.PLACE_KEY)) {
    	   builder.addCustomProperty("place_id", String.valueOf(((Place) additionalEntityParams.get(NotificationProviderUtil.PLACE_KEY)).getId()));
    	   builder.addCustomProperty("place_name", ((Place) additionalEntityParams.get(NotificationProviderUtil.PLACE_KEY)).getName());
       }       
          
       return builder.buildWithDefaultMaximumLength();
    }

    private void sendNotificationToDevice(Notification notification, String deviceToken, String payload) {
       sender.sendMessage(notification, removeSpaces(deviceToken), payload);
    }

    private String removeSpaces(String token) {
       return token.replaceAll("\\s+", "");
   }
}

