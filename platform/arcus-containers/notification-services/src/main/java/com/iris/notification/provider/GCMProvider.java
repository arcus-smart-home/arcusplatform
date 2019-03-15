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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.provider.gcm.GcmSender;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.provider.NotificationProviderUtil;

@Singleton
public class GCMProvider extends AbstractPushNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(GCMProvider.class);

    private static final String ADDRESS_PARAM_KEY = "_address";
    private static final String TARGET_PARAM_KEY = "_target";
   
    @Inject
    private GcmSender sender;

    @Inject
    private NotificationMessageRenderer messageRenderer;

    @Inject
    private RetryProcessor retryProcessor;
    
    @Inject
    private PersonDAO personDao;
    
    @Inject
    private PlaceDAO placeDao;
    
    @Inject
    private AccountDAO accountDao;

    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    public void notifyCustomer(Notification notification) throws DispatchException, DispatchUnsupportedByUserException {
        // TODO: Message attribute and format should be consistent with needs of Android app developers
        //payload.put("notification-message", messageRenderer.renderMessage(notification, NotificationMethod.GCM,null));
        //payload.put("notification", messageRenderer.renderMessage(notification, NotificationMethod.GCM,null));
       
        // Collect recipient information
    	Map<String, BaseEntity<?, ?>> additionalEntityParams = NotificationProviderUtil.addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, notification);
        Person recipient = NotificationProviderUtil.getPersonFromParams(additionalEntityParams);
        if (recipient == null) {
           throw new DispatchUnsupportedByUserException("No person found with id: " + notification.getPersonId());
        }
       
        // If delivery endpoint has been set, then this notification represents a message to a single physical device...
        if (notification.getDeliveryEndpoint() != null) {
           Map<String, Object> payload = new HashMap<String, Object>();
           Map notificationData = JSON.fromJson(messageRenderer.renderMessage(notification, NotificationMethod.GCM, recipient, additionalEntityParams), Map.class);
           
           // Add data for deep-link messages when such data is present
           if (notification.getMessageParams() != null && notification.getMessageParams().containsKey(ADDRESS_PARAM_KEY)) {
        	   notificationData.put("address", notification.getMessageParams().get(ADDRESS_PARAM_KEY));
           }
           if (notification.getMessageParams() != null && notification.getMessageParams().containsKey(TARGET_PARAM_KEY)) {
        	   notificationData.put("target", notification.getMessageParams().get(TARGET_PARAM_KEY));
           }
           if (additionalEntityParams.containsKey(NotificationProviderUtil.PLACE_KEY)) {
        	   notificationData.put("place_id", String.valueOf(((Place) additionalEntityParams.get(NotificationProviderUtil.PLACE_KEY)).getId()));
        	   notificationData.put("place_name", ((Place) additionalEntityParams.get(NotificationProviderUtil.PLACE_KEY)).getName());
           }       
           
           payload.put("data", notificationData);           
           logger.debug("sending gcm notification to {}", notification.getDeliveryEndpoint());
           sender.sendMessage(notification, notification.getDeliveryEndpoint(), payload);
        }

        // ... otherwise, we should send the message to each device associated with this person
        else {
            for (String thisDevice : getDeviceTokensForPerson(notification.getPersonId(), MobileOS.ANDROID)) {
                // Create a child dispatch process for each message delivered to each device
                retryProcessor.split(notification, NotificationMethod.GCM, thisDevice);
            }
        }
    }
}

