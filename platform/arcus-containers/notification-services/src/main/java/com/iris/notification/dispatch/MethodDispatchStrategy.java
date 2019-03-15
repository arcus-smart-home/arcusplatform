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
package com.iris.notification.dispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.notification.provider.NoSuchProviderException;
import com.iris.notification.provider.NotificationProviderRegistry;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

public class MethodDispatchStrategy extends AbstractDispatcher {

   private static final Logger logger = LoggerFactory.getLogger(MethodDispatchStrategy.class);

    public MethodDispatchStrategy(NotificationAuditor audit, RetryProcessor retryProcessor, RetryManager retryManager, NotificationProviderRegistry providerRegistry) {
        super(audit, retryProcessor, retryManager, providerRegistry);
    }

    public void dispatch(Notification notification) {

        try {
            // Try to send the message to the gateway
            sendNotification(notification);
        }
        catch (DispatchException e) {
           logger.error("Failed to dispatch notification: {}:", notification, e);
            // Send the task to the retry processor for later retry attempt
            instrumentFailedDispatch(notification.getMethod(), e);
            retryProcessor.retry(notification, e);
        }
        catch (DispatchUnsupportedByUserException | NoSuchProviderException e) {
            // Unable to dispatch notification, but should not be retried
           logger.error("Failed to dispatch notification: {}:", notification, e);
            instrumentFailedDispatch(notification.getMethod(), e);
            audit.log(notification, AuditEventState.ERROR, e);
        }
        catch (RuntimeException e) {
           logger.error("Failed unexpectedly during dispatch of notification {}:", notification, e);
            // Something unexpectedly catastrophic happened; terminal state
            instrumentFailedDispatch(notification.getMethod(), e);
            audit.log(notification, AuditEventState.FAILED, e);
        }
    }
}

