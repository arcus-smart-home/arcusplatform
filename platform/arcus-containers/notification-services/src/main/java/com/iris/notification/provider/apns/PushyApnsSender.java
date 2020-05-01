/*
 * Copyright 2020 Arcus Project
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

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.notification.NotificationServiceConfig;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.File;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PushyApnsSender implements ApnsSender {

    private static final Logger logger = LoggerFactory.getLogger(PushyApnsSender.class);

    private final ApnsClient apnsClient;
    private final UpstreamNotificationResponder upstreamResponder;
    private final String topic;

    @Inject
    public PushyApnsSender(
            NotificationServiceConfig config,
            @Named("apns.pkcs12.path") String pkcs12Path,
            @Named("apns.pkcs12.password") String keystorePassword,
            @Named("apns.production") boolean isProduction,
            @Named("apns.inactiveCloseTime") int inactiveCloseTime,
            @Named("apns.topic") String topic,
            UpstreamNotificationResponder upstreamResponder) throws Exception
    {
        this.upstreamResponder = upstreamResponder;

        this.apnsClient = new ApnsClientBuilder()
                .setApnsServer(isProduction ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setClientCredentials(new File(pkcs12Path), keystorePassword)
                .setConcurrentConnections(config.getApnsConnections())
                .build();

        this.topic = topic;

        logger.info("APNS provider has started up.");
    }

    @PreDestroy
    public void shutdown() {
        try {
            logger.info("Shutting down APNS provider.");
            apnsClient.close().await();
        } catch (InterruptedException e) {
            logger.warn("APNS provider failed to shut down gracefully.", e);
        }
    }

    @Override
    public void sendMessage(Notification notification, String token, String payload) {
        // Put the message in Pushy's queue for transmission to Apple
        PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                sendNotificationFuture = apnsClient.sendNotification(new IrisApnsPushNotification(notification, token, topic, payload));

        sendNotificationFuture.addListener((GenericFutureListener<Future<PushNotificationResponse>>) future -> {
            final PushNotificationResponse response = future.getNow();
            if (response.isAccepted()) {
                upstreamResponder.handleHandOff(notification);
            } else {
                logger.warn("Notification rejected by the APNs gateway: " +
                        response.getRejectionReason());

                if (response.getTokenInvalidationTimestamp() != null) {
                    upstreamResponder.handleDeviceUnregistered(NotificationMethod.APNS, token);
                } else {
                    upstreamResponder.handleError(notification, true, response.getRejectionReason());
                }
            }
        });
    }
}

