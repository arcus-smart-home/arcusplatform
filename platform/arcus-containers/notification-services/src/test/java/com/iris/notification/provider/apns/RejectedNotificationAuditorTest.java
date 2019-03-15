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
package com.iris.notification.provider.apns;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.RejectedNotificationReason;
import com.relayrides.pushy.apns.util.TokenUtil;

@RunWith(MockitoJUnitRunner.class)
public class RejectedNotificationAuditorTest {
    @Mock
    private UpstreamNotificationResponder errorResponder;

    @Mock
    private PushManager<IrisApnsPushNotification> pushManager;

    @Mock
    private IrisApnsPushNotification notification;

    @InjectMocks
    private RejectedNotificationAuditor rejectedNotificationAuditor;
    
    @Test
    public void shouldHandleTerminalError() throws Exception {

        RejectedNotificationReason reason = RejectedNotificationReason.INVALID_PAYLOAD_SIZE;
        Notification expectedNotification = Mockito.mock(Notification.class);
        Mockito.when(notification.getNotification()).thenReturn(expectedNotification);

        rejectedNotificationAuditor.handleRejectedNotification(pushManager, notification, reason);

        Mockito.verify(errorResponder).handleError(expectedNotification, false, reason.toString());
    }

    @Test
    public void shouldHandleNonTerminalError() throws Exception {

        RejectedNotificationReason reason = RejectedNotificationReason.PROCESSING_ERROR;
        Notification expectedNotification = Mockito.mock(Notification.class);
        Mockito.when(notification.getNotification()).thenReturn(expectedNotification);

        rejectedNotificationAuditor.handleRejectedNotification(pushManager, notification, reason);

        Mockito.verify(errorResponder).handleError(expectedNotification, true, reason.toString());
    }

    @Test
    public void shouldHandleInvalidTokenError() throws Exception {
        RejectedNotificationReason reason = RejectedNotificationReason.INVALID_TOKEN;
        Mockito.when(notification.getToken()).thenReturn(new byte[]{Byte.MIN_VALUE});
        
        rejectedNotificationAuditor.handleRejectedNotification(pushManager, notification, reason);
        Mockito.verify(errorResponder).handleDeviceUnregistered(NotificationMethod.APNS, TokenUtil.tokenBytesToString(notification.getToken()));
    }
}

