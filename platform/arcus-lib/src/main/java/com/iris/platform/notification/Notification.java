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
package com.iris.platform.notification;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.type.EmailRecipient;

public class Notification {

    private final NotificationPriority priority;
    private final UUID placeId;
    private final UUID personId;
    private final String messageKey;
    private final Map<String, String> messageParams;
    private final String customMessage;
    private final Instant txTimestamp;		// Time that notification message was *sent*
    private Instant rxTimestamp;            // Time that notification message was *received*
    private final int timeToLive;			// Number of milliseconds after timestamp that message should expire; <0 means never expire

    private NotificationMethod method;
    private int deliveryAttempts = 0;		// Number of times the message has been attempted to be delivered
    private String deliveryEndpoint;		// Used only for audit logs; endpoint info (phone number, email, etc.) that notification was sent to
    private boolean isCustomMessage;        // True when representing a NotifyCustom method; false otherwise
    private boolean isEmailRequest;
    private final EmailRecipient emailRecipient;


    public Notification(boolean isCustomMessage, boolean isEmailRequest, NotificationPriority priority, NotificationMethod method, String placeId, String personId, String messageKey, Map<String, String> messageParams, String customMessage,
            Instant timestamp, int timeToLive, EmailRecipient emailRecipient) {
        this.isCustomMessage = isCustomMessage;
        this.isEmailRequest = isEmailRequest;
        this.priority = priority;
        this.method = method;
        this.placeId = StringUtils.isBlank(placeId) ? null : UUID.fromString(placeId);
        this.personId = personId == null ? null : UUID.fromString(personId);
        this.messageKey = messageKey;
        this.messageParams = messageParams;
        this.customMessage = customMessage;
        txTimestamp = timestamp;
        rxTimestamp = Instant.now();
        this.timeToLive = timeToLive;
        this.emailRecipient = emailRecipient;
    }

    public boolean isCustomMessage()                { return isCustomMessage; }
    public boolean isEmailRequest()                 { return isEmailRequest; }
    public NotificationMethod getMethod()           { return method; }
    public NotificationPriority getPriority()       { return priority; }
    public UUID getPlaceId() 						{ return placeId; }
    public UUID getPersonId() 						{ return personId; }
    public String getMessageKey() 					{ return messageKey; }
    public Map<String, String> getMessageParams() 	{ return messageParams; }
    public String getCustomMessage() 				{ return customMessage; }
    public Instant getTxTimestamp() 				{ return txTimestamp; }
    public Instant getRxTimestamp() 				{ return rxTimestamp; }
    public int getTimeToLive() 						{ return timeToLive; }
    public int getDeliveryAttempts() 				{ return deliveryAttempts; }
    public String getDeliveryEndpoint()				{ return deliveryEndpoint; }
    public EmailRecipient getEmailRecipient() { return emailRecipient; }

    public void setDeliveryEndpoint (String deliveryEndpoint) {
        this.deliveryEndpoint = deliveryEndpoint;
    }

    public void setMethod(NotificationMethod method) {
        this.method = method;
    }

    public String getEventIdentifier () {
       return placeId == null ? "person:" + String.valueOf(personId) : "place:" + String.valueOf(placeId);
    }

    public Instant getExpirationTime() {
        if (timeToLive < 0) {
            return Instant.MAX;
        } else {
            return txTimestamp.plus(timeToLive, ChronoUnit.SECONDS);
        }
    }

    public Duration getEnqueuedDuration() {
        return Duration.between(txTimestamp, rxTimestamp);
    }

    public Duration getInflightDuration() {
        return Duration.between(rxTimestamp, Instant.now());
    }

    public void incrementDeliveryAttempts() {
        deliveryAttempts++;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String toJsonString() {
        return JSON.toJson(this);
    }

    public static Notification fromJsonString(String json) {
        return JSON.fromJson(json, Notification.class);
    }

    public static Notification fromPlatformMessage(PlatformMessage message) {

        boolean isCustomMessage;
        boolean isEmailRequest;
        String placeId = null;
        String personId = null;
        String msgKey = null;
        Map<String, String> msgParams = null;
        String msg = null;
        NotificationMethod dispatchMethod = null;
        NotificationPriority priority = null;
        EmailRecipient emailRecipient = null;

        MessageBody body = message.getValue();

        if(NotificationCapability.NotifyRequest.NAME.equals(body.getMessageType())){
            isCustomMessage = false;
            isEmailRequest = false;
            personId = NotificationCapability.NotifyRequest.getPersonId(body);
            placeId = NotificationCapability.NotifyRequest.getPlaceId(body);
            msgKey = NotificationCapability.NotifyRequest.getMsgKey(body);
            msgParams = NotificationCapability.NotifyRequest.getMsgParams(body);
            priority = NotificationPriority.valueOf(NotificationCapability.NotifyRequest.getPriority(body).toUpperCase());
        }
        else if(NotificationCapability.NotifyCustomRequest.NAME.equals(body.getMessageType())){
            isCustomMessage = true;
            isEmailRequest = false;
            personId = NotificationCapability.NotifyCustomRequest.getPersonId(body);
            placeId = NotificationCapability.NotifyCustomRequest.getPlaceId(body);
            msg =  NotificationCapability.NotifyCustomRequest.getMsg(body);
            dispatchMethod =  NotificationMethod.valueOf(NotificationCapability.NotifyCustomRequest.getDispatchMethod(body).toUpperCase());
        }
        else if(NotificationCapability.EmailRequest.NAME.equals(body.getMessageType())) {
           isCustomMessage = false;
           isEmailRequest = true;
           emailRecipient = new EmailRecipient(NotificationCapability.EmailRequest.getRecipient(body));
           placeId = NotificationCapability.EmailRequest.getPlaceId(body);
           dispatchMethod = NotificationMethod.EMAIL;
           msgKey = NotificationCapability.NotifyRequest.getMsgKey(body);
           msgParams = NotificationCapability.NotifyRequest.getMsgParams(body);
        }else{
            throw new IllegalArgumentException("Unknown Notification Type");
        }

        Instant timestamp = message.getTimestamp().toInstant();
        int ttl = message.getTimeToLive();

        return new Notification(isCustomMessage, isEmailRequest, priority, dispatchMethod, placeId, personId, msgKey, msgParams, msg, timestamp, ttl, emailRecipient);
    }

    /**
     * Creates a copy of this notification but one whose notification method and delivery endpoint are set to the provided values.
     * This method is useful for spawning notifications of method 'PUSH' into child notifications of 'GCM' and 'APNS' and for
     * spawning notifications that may have multiple physical endpoint (for example, an APNS notification to a custom with
     * multiple iOS devices) into child notifications representing each device.
     *
     * @param method
     *            The NotificationMethod that should be assigned to the copied Notification object.
     * @param endpoint
     *            The endpoint that should be assigned to the copied Notification object. The meaning and acceptable value of
     *            endpoint is method/provider specific.
     * @return A copy of this Notification object.
     */
    public Notification copy(NotificationMethod method, String endpoint) {
        Notification copy = new Notification(isCustomMessage, isEmailRequest, priority, method, placeId == null ? null : placeId.toString(), personId == null ? null : personId.toString(), messageKey, messageParams, customMessage, txTimestamp, timeToLive, emailRecipient);

        copy.rxTimestamp = rxTimestamp;
        copy.deliveryAttempts = deliveryAttempts;
        copy.deliveryEndpoint = endpoint;

        return copy;
    }

    /**
     * Creates a copy of this notification but one whose notification method and delivery endpoint are set to the provided values.
     * This method is useful for spawning notifications of method 'PUSH' into child notifications of 'GCM' and 'APNS' and for
     * spawning notifications that may have multiple physical endpoint (for example, an APNS notification to a custom with
     * multiple iOS devices) into child notifications representing each device.
     *
     * @param method
     *            The NotificationMethod that should be assigned to the copied Notification object.
     * @param endpoint
     *            The endpoint that should be assigned to the copied Notification object. The meaning and acceptable value of
     *            endpoint is method/provider specific.
     * @return A copy of this Notification object.
     */
    public Notification copy(NotificationMethod method) {
        Notification copy = new Notification(isCustomMessage, isEmailRequest, priority, method, placeId == null ? null : placeId.toString(), personId.toString(), messageKey, messageParams, customMessage, txTimestamp, timeToLive, emailRecipient);
        return copy;
    }

}

