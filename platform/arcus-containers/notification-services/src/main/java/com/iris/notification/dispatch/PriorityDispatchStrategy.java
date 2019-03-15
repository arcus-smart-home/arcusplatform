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
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.NotificationPriority;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

public class PriorityDispatchStrategy extends AbstractDispatcher {
	private static final Logger logger = LoggerFactory.getLogger(PriorityDispatchStrategy.class);

    public PriorityDispatchStrategy(NotificationAuditor audit, RetryProcessor retryProcessor, RetryManager retryManager, NotificationProviderRegistry providerRegistry) {
        super(audit, retryProcessor, retryManager, providerRegistry);
    }

    public void dispatch(Notification notification) {

        // Is this our first attempt at delivering this notification...
        if (notification.getMethod() == null) {
            // ... if so, set initial dispatch method
            NotificationMethod initialMethod = getInitialNotificationMethod(notification.getPriority());
            notification.setMethod(initialMethod);
        }

        // Send the message to the gateway...
        try {
            sendNotification(notification);
        }
        catch (DispatchUnsupportedByUserException | NoSuchProviderException e) {
           logger.error("Failed to dispatch notification: {}:", notification, e);

            // ... if we fail as a result of the custom not being provisioned to accept notifications using the default method,
            // then fall back on the next available dispatch method for this priority
            instrumentFailedDispatch(notification.getMethod(), e);
            fallback(notification, e);
        }
        catch (DispatchException e) {
           logger.error("Failed to dispatch notification: {}:", notification, e);
            // Technical problem delivering the notification;
            instrumentFailedDispatch(notification.getMethod(), e);
            retryProcessor.retry(notification, e);
        }
        catch (RuntimeException e) {
            // Something unexpectedly catastrophic happened; terminal state
            instrumentFailedDispatch(notification.getMethod(), e);
            logger.error("Failed unexpectedly during dispatch of notification {}:", notification, e);
            audit.log(notification, AuditEventState.FAILED, e);
        }
    }

    private void fallback(Notification notification, Exception reason) {

        try {
            // Try to fallback on next available notification method
            NotificationMethod fallbackMethod = getFallbackMethod(notification.getMethod());
            notification.setMethod(fallbackMethod);

            retryProcessor.retry(notification, reason);
        }

        // No more methods to fallback on... terminate in FAILED state
        catch (IllegalStateException e) {
            audit.log(notification, AuditEventState.FAILED, e);
        }
    }

    /**
     * Determines the first method of notification that should be used for a given priority; as defined by the Notification
     * Services design (on Confluence).
     *
     * @param priority
     * @return
     */
    private NotificationMethod getInitialNotificationMethod(NotificationPriority priority) {
    	return priority.getDefaultMethod();        
    }

    private NotificationMethod getFallbackMethod(NotificationMethod method) {

        switch (method) {
        case IVR:
            return NotificationMethod.PUSH;
        case PUSH:
        case APNS:
        case GCM:
            return NotificationMethod.EMAIL;
        default:
            throw new IllegalStateException("Unable to deliver notification using any available fallback method.");
        }
    }

}

