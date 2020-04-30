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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.platform.notification.Notification;

public class NoopApnsSender implements ApnsSender {
    private static final Logger logger = LoggerFactory.getLogger(NoopApnsSender.class);

    @Override
    public void sendMessage(final Notification notification, final String token, final String payload) {
       logger.warn("noop apns sender dropping notification: {} - {}", notification, payload);
    }
}

