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

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;

public final class SmackLoggingConnectionListener implements ConnectionListener {

    private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);

    private final Counter connectedCounter = METRICS.counter("gcm.connected");
    private final Counter authenticatedCounter = METRICS.counter("gcm.authenticated");
    private final Counter reconnectionSuccessCounter = METRICS.counter("gcm.reconnection.success");
    private final Counter reconnectionFailedCounter = METRICS.counter("gcm.reconnection.failed");
    private final Counter connectionClosedOnErrorCounter = METRICS.counter("gcm.closed.error");
    private final Counter connectionClosedCounter = METRICS.counter("gcm.closed");

    @Override
    public void connected(XMPPConnection xmppConnection) {
        connectedCounter.inc();
    }

    @Override
    public void authenticated(XMPPConnection xmppConnection) {
        authenticatedCounter.inc();
    }

    @Override
    public void reconnectionSuccessful() {
        reconnectionSuccessCounter.inc();
    }

    @Override
    public void reconnectionFailed(Exception e) {
        reconnectionFailedCounter.inc();
    }

    @Override
    public void reconnectingIn(int seconds) {
        // Nothing to do
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        connectionClosedOnErrorCounter.inc();
    }

    @Override
    public void connectionClosed() {
        connectionClosedCounter.inc();
    }
}

