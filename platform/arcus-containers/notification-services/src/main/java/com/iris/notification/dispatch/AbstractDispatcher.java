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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.provider.NoSuchProviderException;
import com.iris.notification.provider.NotificationProvider;
import com.iris.notification.provider.NotificationProviderRegistry;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

public abstract class AbstractDispatcher {

    protected final NotificationAuditor audit;
    protected final RetryProcessor retryProcessor;
    protected final RetryManager retryManager;
    protected final NotificationProviderRegistry providerRegistry;

    public AbstractDispatcher(NotificationAuditor audit, RetryProcessor retryProcessor, RetryManager retryManager, NotificationProviderRegistry providerRegistry) {
        this.audit = audit;
        this.retryManager = retryManager;
        this.retryProcessor = retryProcessor;
        this.providerRegistry = providerRegistry;
    }

    protected void sendNotification(Notification notification) throws DispatchException, DispatchUnsupportedByUserException, NoSuchProviderException {
        
        NotificationMethod method = notification.getMethod();

        if (method == NotificationMethod.PUSH) {
           //Checking for the existence of at least one mobile device before splitting this message.  We can use any PUSH provider that extends AbstractPushNotificationProvider
           NotificationProvider provider = providerRegistry.getInstanceForProvider(NotificationMethod.APNS);
           if(!provider.supportedByUser(notification)){
              throw new DispatchUnsupportedByUserException("No mobile devices associated with person id: " + notification.getPersonId());
           }

              // "PUSH" is a logical method that means notify by GCM and APNS... split this notification into two; one for each push
            // notification type. (These child notifications will further be split to represent each device owned by the person.)
            retryProcessor.split(notification, NotificationMethod.GCM, null);
            retryProcessor.split(notification, NotificationMethod.APNS, null);
        }

        else {
            // Get an instance of the provider client
            NotificationProvider provider = providerRegistry.getInstanceForProvider(method);

            // Ready to send to provider gateway; increment the number of delivery
            // attempts and update the audit log
            notification.incrementDeliveryAttempts();
            audit.log(notification, AuditEventState.SENT);

            // Send the notification to the provider
            provider.notifyCustomer(notification);
        }
    }

    /**
     * Increments the success counter associated with this notification method. Metric is named dispatch.{method}, i.e.,
     * "notifications.dispatch.apns"
     *
     * @param method
     *            The NotificationMethod by which the notification was dispatched.
     */
    protected void instrumentSuccessfulDispatch(NotificationMethod method, Duration inflightDuration) {
        String counterName = "dispatch." + method.toString().toLowerCase();
        String timerName = "dispatch." + method.toString().toLowerCase() + ".latency";

        IrisMetrics.metrics(NotificationService.SERVICE_NAME).counter(counterName).inc();
        IrisMetrics.metrics(NotificationService.SERVICE_NAME).timer(timerName).update(inflightDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Increments the failure counter associated with this notification method for a given result. Metric is named
     * dispatch.{method}.{result.getSimpleName}, i.e., "notifications.dispatch.apns.nullpointerexception"
     *
     * @param method
     *            The NotificationMethod by which the notification was trying to be dispatched.
     * @param result
     *            The exception that produced the error condition.
     */
    protected void instrumentFailedDispatch(NotificationMethod method, Exception result) {
        String counterName = "dispatch." + method.toString().toLowerCase() + "." + result.getClass().getSimpleName().toLowerCase();
        IrisMetrics.metrics(NotificationService.SERVICE_NAME).counter(counterName).inc();
    }
}

