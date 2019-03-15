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

import static org.junit.Assert.assertEquals;

import javax.net.ssl.SSLHandshakeException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.relayrides.pushy.apns.PushManager;

@RunWith(MockitoJUnitRunner.class)
public class FailedConnectionAuditorTest {

    @Mock
    private PushManager<IrisApnsPushNotification> pushManager;

    @Test
    public void shouldIncrementFatalErrorCounterForSslError() {
        FailedConnectionAuditor uut = new FailedConnectionAuditor();
        uut.handleFailedConnection(pushManager, new SSLHandshakeException("expected-message"));

        assertEquals(Counter.class, IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get("notifications.apns.fatal.error").getClass());
        Counter counter = (Counter) IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get("notifications.apns.fatal.error");

        assertEquals(1L, counter.getCount());

        // No good way to reset this value so as not to effect future tests
        counter.dec();
    }

    @Test
    public void shouldNotIncrementFatalErrorCounterForOtherErrors() {
        FailedConnectionAuditor uut = new FailedConnectionAuditor();
        uut.handleFailedConnection(pushManager, new Exception());

        assertEquals(Counter.class, IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get("notifications.apns.fatal.error").getClass());
        Counter counter = (Counter) IrisMetrics.metrics(NotificationService.SERVICE_NAME).getMetrics().get("notifications.apns.fatal.error");

        assertEquals(0L, counter.getCount());
    }

}

