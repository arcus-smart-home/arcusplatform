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

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.NotificationMethod;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.RejectedNotificationListener;
import com.relayrides.pushy.apns.RejectedNotificationReason;
import com.relayrides.pushy.apns.util.TokenUtil;

public class RejectedNotificationAuditor implements RejectedNotificationListener<IrisApnsPushNotification> {

    private final UpstreamNotificationResponder errorResponder;

    public RejectedNotificationAuditor(UpstreamNotificationResponder errorResponder) {
        this.errorResponder = errorResponder;
    }

    @Override
    public void handleRejectedNotification(final PushManager<? extends IrisApnsPushNotification> pushManager, final IrisApnsPushNotification notification, final RejectedNotificationReason reason) {
        ApnsErrorCode errorCode = ApnsErrorCode.valueOf(reason.toString());
        // invalid token often means incorrect cert, incorrect token for the environment or uninstall/reinstall
        // of the app, we'll treat it as an unregistered device so subsequent notifications for the person don't
        // continue to try this token
        if(errorCode == ApnsErrorCode.INVALID_TOKEN) {
           errorResponder.handleDeviceUnregistered(NotificationMethod.APNS, TokenUtil.tokenBytesToString(notification.getToken()));
        } else {
           errorResponder.handleError(notification.getNotification(), !ApnsErrorCode.isTerminalError(reason.toString()), reason.toString());
        }
    }
}

