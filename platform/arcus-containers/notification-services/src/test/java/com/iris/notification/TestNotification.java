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
package com.iris.notification;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.messages.PlatformMessage;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.NotificationPriority;

@RunWith(MockitoJUnitRunner.class)
public class TestNotification {

    private Notification uut;

    private final boolean isCustomMessage = true;
    private final boolean isEmailMessage = false;
    private final NotificationPriority priority = NotificationPriority.HIGH;
    private final NotificationMethod method = NotificationMethod.APNS;
    private final String customMessage = "custom-message";
    private final String deliveryEndpoint = "delivery-endpoint";
    private final String messageKey = "message-key";
    private final UUID personId = UUID.randomUUID();
    private final UUID placeId = UUID.randomUUID();
    private final Instant timestamp = Instant.now();
    private final int timeToLive = 0;

    @Mock
    private Map<String, String> messageParams;

    @Mock
    private PlatformMessage message;

    @Before
    public void setup () {
        uut = new Notification(isCustomMessage, isEmailMessage, priority, method, placeId.toString(), personId.toString(), messageKey, messageParams, customMessage, timestamp, timeToLive, null);
    }

    @Test
    public void testNotification() throws Exception {
        assertEquals(isCustomMessage, uut.isCustomMessage());
        assertEquals(customMessage, uut.getCustomMessage());
        assertEquals(method, uut.getMethod());
        assertEquals(priority, uut.getPriority());
        assertEquals(placeId, uut.getPlaceId());
        assertEquals(personId, uut.getPersonId());
        assertEquals(messageKey, uut.getMessageKey());
        assertEquals(messageParams, uut.getMessageParams());
        assertEquals(timestamp, uut.getTxTimestamp());
        assertEquals(timeToLive, uut.getTimeToLive());
    }

    @Test
    public void testIncrementDeliveryAttempts() throws Exception {
        assertEquals(0, uut.getDeliveryAttempts());
        uut.incrementDeliveryAttempts();
        assertEquals(1, uut.getDeliveryAttempts());
    }

    @Test
    public void testDeliveryEndpoint () {
        assertEquals(null, uut.getDeliveryEndpoint());
        uut.setDeliveryEndpoint(deliveryEndpoint);
        assertEquals(deliveryEndpoint, uut.getDeliveryEndpoint());
    }
}

