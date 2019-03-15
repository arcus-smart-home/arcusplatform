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

import java.util.Map;

import javax.annotation.PreDestroy;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;

@Singleton
public class SmackGcmSender implements GcmSender {

    private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);

    private SmackGcmSenderChannel activeChannel;

    private final String googleApiKey;
    private final Long googleSenderId;
    private final int keepAliveInterval;
    private final UpstreamNotificationResponder upstreamResponder;
    private final Counter smackChannelCounter = METRICS.counter("gcm.smack.channel.count");

    @Inject
    public SmackGcmSender(
          @Named("gcm.apikey") String googleApiKey,
          @Named("gcm.senderid") Long googleSenderId,
          @Named("gcm.keepAliveInterval") int keepAliveInterval,
          UpstreamNotificationResponder upstreamResponder) {
        this.upstreamResponder = upstreamResponder;
        this.googleApiKey = googleApiKey;
        this.googleSenderId = googleSenderId;
        this.keepAliveInterval = keepAliveInterval;
    }

    @PreDestroy
    public void shutdown() {
        if (activeChannel != null) {
            activeChannel.close();
        }
    }

    @Override
    public void sendMessage(Notification notification, String toDevice, Map<String, Object> payload) {
        SmackGcmSenderChannel gcmChannel = acquireActiveGcmChannel();
        gcmChannel.sendMessage(notification, toDevice, payload);
    }

    private synchronized SmackGcmSenderChannel acquireActiveGcmChannel() {
        if (activeChannel == null || activeChannel.hasClosed()) {
            activeChannel = new SmackGcmSenderChannel(googleApiKey, googleSenderId, keepAliveInterval, upstreamResponder);
            smackChannelCounter.inc();
        }

        return activeChannel;
    }
}

