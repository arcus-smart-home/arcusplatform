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

import java.util.Collection;

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.NotificationMethod;
import com.relayrides.pushy.apns.ExpiredToken;
import com.relayrides.pushy.apns.ExpiredTokenListener;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.util.TokenUtil;

public class ExpiredTokenAuditor implements ExpiredTokenListener<IrisApnsPushNotification> {

    private final UpstreamNotificationResponder responder;

    public ExpiredTokenAuditor(UpstreamNotificationResponder responder) {
        this.responder = responder;
    }

    @Override
    public void handleExpiredTokens(final PushManager<? extends IrisApnsPushNotification> pushManager, final Collection<ExpiredToken> expiredTokens) {

        for (final ExpiredToken expiredToken : expiredTokens) {
            responder.handleDeviceUnregistered(NotificationMethod.APNS, TokenUtil.tokenBytesToString(expiredToken.getToken()));
        }
    }

}

