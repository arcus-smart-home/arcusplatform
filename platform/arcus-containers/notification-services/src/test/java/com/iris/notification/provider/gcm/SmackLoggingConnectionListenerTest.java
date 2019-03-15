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
package com.iris.notification.provider.gcm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;

public class SmackLoggingConnectionListenerTest {

    private SmackLoggingConnectionListener uut = new SmackLoggingConnectionListener();
    private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);

    @Test
    public void testConnected() throws Exception {
        uut.connected(null);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.connected")).getCount());
    }

    @Test
    public void testAuthenticated() throws Exception {
        uut.authenticated(null);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.authenticated")).getCount());
    }

    @Test
    public void testReconnectionSuccessful() throws Exception {
        uut.reconnectionSuccessful();
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.reconnection.success")).getCount());
    }

    @Test
    public void testReconnectionFailed() throws Exception {
        uut.reconnectionFailed(null);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.reconnection.failed")).getCount());
    }

    @Test
    public void testConnectionClosedOnError() throws Exception {
        uut.connectionClosedOnError(null);
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.closed.error")).getCount());
    }

    @Test
    public void testConnectionClosed() throws Exception {
        uut.connectionClosed();
        assertEquals(1L, ((Counter) METRICS.getMetrics().get("notifications.gcm.closed")).getCount());
    }

}

