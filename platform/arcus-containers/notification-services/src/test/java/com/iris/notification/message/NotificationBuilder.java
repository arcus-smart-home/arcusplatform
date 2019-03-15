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
package com.iris.notification.message;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.iris.messages.type.EmailRecipient;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.NotificationPriority;

public class NotificationBuilder {

    private boolean isCustomMessage = false;
    private boolean isEmailRequest = false;
    private NotificationMethod method = NotificationMethod.LOG;
    private NotificationPriority priority = null;
    private UUID placeId = UUID.randomUUID();
    private UUID personId = UUID.randomUUID();
    private String messageKey = "message-key";
    private Map<String, String> messageParams = new HashMap<String, String>();
    private String customMessage = "";

    private Instant timestamp = Instant.now();
    private int timeToLive = 0;
    private EmailRecipient emailRecipient;

    public Notification build() {
        return new Notification(isCustomMessage, isEmailRequest, priority, method, placeId.toString(), personId.toString(), messageKey, messageParams, customMessage, timestamp, timeToLive, emailRecipient);
    }

    public NotificationBuilder withPersonId(UUID personId) {
        this.personId = personId;
        return this;
    }
    
    public NotificationBuilder withPlaceId(UUID placeId) {
        this.placeId = placeId;
        return this;
    }

    public NotificationBuilder withPriority(NotificationPriority priority) {
        this.priority = priority;
        return this;
    }

    public NotificationBuilder withMethod(NotificationMethod method) {
        this.method = method;
        return this;
    }

    public NotificationBuilder withMessageKey(String messageKey) {
        this.messageKey = messageKey;
        return this;
    }

    public NotificationBuilder withMessageParam(String name, String value) {
        messageParams.put(name, value);
        return this;
    }

    public NotificationBuilder withMessageParams(Map<String, String> params) {
        messageParams.putAll(params);
        return this;
    }

    public NotificationBuilder withCustomMessage(String customMessage) {
        isCustomMessage = true;
        this.customMessage = customMessage;
        return this;
    }

    public NotificationBuilder withPriority(String priority) {
        this.priority = NotificationPriority.valueOf(priority.toUpperCase());
        return this;
    }

    public NotificationBuilder withEmailRecipient(EmailRecipient emailRecipient) {
       this.emailRecipient = emailRecipient;
       isEmailRequest = true;
       return this;
    }
}

