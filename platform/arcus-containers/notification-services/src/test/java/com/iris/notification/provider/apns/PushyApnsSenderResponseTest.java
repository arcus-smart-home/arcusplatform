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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

/**
 * Covers how a send outcome is turned into an upstream response.
 *
 * Pushy 0.14 moved from Netty futures to CompletableFuture and made the
 * rejection reason and token invalidation timestamp Optional, so every branch
 * here was rewritten. APNS is switched off in deployment today, which is
 * exactly why this needs a test: nothing else would notice if it broke.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PushyApnsSenderResponseTest {

   private static final String TOKEN = "0000000000000000000000000000000000000000000000000000000000000000";

   @Mock
   private UpstreamNotificationResponder responder;

   @Mock
   private Notification notification;

   @Mock
   private PushNotificationResponse<SimpleApnsPushNotification> response;

   @Test
   public void testAcceptedHandsOff() {
      when(response.isAccepted()).thenReturn(true);

      PushyApnsSender.handleResponse(responder, notification, TOKEN, response, null);

      verify(responder).handleHandOff(notification);
      verify(responder, never()).handleError(notification, true, "unknown");
      verify(responder, never()).handleDeviceUnregistered(NotificationMethod.APNS, TOKEN);
   }

   @Test
   public void testRejectedReportsTheReason() {
      when(response.isAccepted()).thenReturn(false);
      when(response.getRejectionReason()).thenReturn(Optional.of("BadDeviceToken"));
      when(response.getTokenInvalidationTimestamp()).thenReturn(Optional.empty());

      PushyApnsSender.handleResponse(responder, notification, TOKEN, response, null);

      verify(responder).handleError(notification, true, "BadDeviceToken");
      verify(responder, never()).handleHandOff(notification);
   }

   @Test
   public void testRejectedWithoutAReasonStillReports() {
      // getRejectionReason() is Optional as of 0.14; an empty one must not NPE
      when(response.isAccepted()).thenReturn(false);
      when(response.getRejectionReason()).thenReturn(Optional.empty());
      when(response.getTokenInvalidationTimestamp()).thenReturn(Optional.empty());

      PushyApnsSender.handleResponse(responder, notification, TOKEN, response, null);

      verify(responder).handleError(notification, true, "unknown");
   }

   @Test
   public void testInvalidatedTokenUnregistersTheDevice() {
      when(response.isAccepted()).thenReturn(false);
      when(response.getRejectionReason()).thenReturn(Optional.of("Unregistered"));
      when(response.getTokenInvalidationTimestamp()).thenReturn(Optional.of(Instant.now()));

      PushyApnsSender.handleResponse(responder, notification, TOKEN, response, null);

      verify(responder).handleDeviceUnregistered(NotificationMethod.APNS, TOKEN);
      verify(responder, never()).handleError(notification, true, "Unregistered");
   }

   @Test
   public void testFailureToReachAppleIsRecoverable() {
      // the old Netty listener called future.getNow() without checking for
      // failure, so a send that never reached Apple went unreported
      PushyApnsSender.handleResponse(
            responder, notification, TOKEN, null, new IOException("connection reset"));

      verify(responder).handleError(notification, true, "connection reset");
      verify(responder, never()).handleHandOff(notification);
   }

   @Test
   public void testFailureDoesNotTouchTheResponse() {
      PushyApnsSender.handleResponse(
            responder, notification, TOKEN, null, new IOException("connection reset"));

      verifyNoInteractions(response);
   }
}
