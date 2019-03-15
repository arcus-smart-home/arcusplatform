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

import javax.net.ssl.SSLHandshakeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.relayrides.pushy.apns.FailedConnectionListener;
import com.relayrides.pushy.apns.PushManager;

public class FailedConnectionAuditor implements FailedConnectionListener<IrisApnsPushNotification> {

    private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);
    private static final Logger LOGGER = LoggerFactory.getLogger(FailedConnectionAuditor.class);

    private final Counter fatalErrorCounter = METRICS.counter("apns.fatal.error");

    @Override
    public void handleFailedConnection(final PushManager<? extends IrisApnsPushNotification> pushManager, final Throwable cause) {

        if (cause instanceof SSLHandshakeException) {

            // Increment the fatal error counter (should raise an SNMP alarm)
            fatalErrorCounter.inc();

            LOGGER.error(
                    "A fatal SSL error has occured communicating with the APNS gateway. APNS notifications are now offline. Check the certificate used to communicate with Apple's gateway and restart the notification services.",
                    cause);

            try {
                pushManager.shutdown();
            } catch (InterruptedException e) {
                LOGGER.error("APNS provider failed to shutdown during SSLHandshakeException error.");
            }
        }
    }
}

