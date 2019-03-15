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
package com.iris.notification.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;

@Singleton
public class LogProvider implements NotificationProvider {

    @Inject private NotificationMessageRenderer messageRenderer;

    private static final Logger LOGGER = LoggerFactory.getLogger(LogProvider.class);

    @Override
    public void notifyCustomer(Notification notification) throws DispatchException {
        // Squirrel away a "fake" delivery endpoint
        notification.setDeliveryEndpoint("log:" + LOGGER.toString());

        String message = messageRenderer.renderMessage(notification, NotificationMethod.LOG,null, null);
        LOGGER.info(message);
    }

}

