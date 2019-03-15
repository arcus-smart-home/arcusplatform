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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.NotificationMethod;
import com.relayrides.pushy.apns.ExpiredToken;
import com.relayrides.pushy.apns.PushManager;
import com.relayrides.pushy.apns.util.TokenUtil;

@RunWith(MockitoJUnitRunner.class)
public class ExpiredTokenAuditorTest {
    @Mock
    private UpstreamNotificationResponder responder;

    @Mock
    private PushManager<IrisApnsPushNotification> pushManager;

    private ExpiredTokenAuditor expiredTokenAuditor;

    @Test
    public void testHandleExpiredTokens() throws Exception {

        ExpiredToken expired1 = Mockito.mock(ExpiredToken.class);
        ExpiredToken expired2 = Mockito.mock(ExpiredToken.class);

        String iosToken1 = "0000000000000000000000000000000000000000000000000000000000000000";
        String iosToken2 = "9999999999999999999999999999999999999999999999999999999999999999";

        Mockito.when(expired1.getToken()).thenReturn(TokenUtil.tokenStringToByteArray(iosToken1));
        Mockito.when(expired2.getToken()).thenReturn(TokenUtil.tokenStringToByteArray(iosToken2));

        List<ExpiredToken> expiredTokens = new ArrayList<ExpiredToken>();
        expiredTokens.add(expired1);
        expiredTokens.add(expired2);

        expiredTokenAuditor = new ExpiredTokenAuditor(responder);
        expiredTokenAuditor.handleExpiredTokens(pushManager, expiredTokens);

        Mockito.verify(responder).handleDeviceUnregistered(NotificationMethod.APNS, iosToken1);
        Mockito.verify(responder).handleDeviceUnregistered(NotificationMethod.APNS, iosToken2);
    }

}

