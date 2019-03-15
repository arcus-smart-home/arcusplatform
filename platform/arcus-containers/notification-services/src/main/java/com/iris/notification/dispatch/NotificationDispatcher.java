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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.notification.provider.NotificationProviderRegistry;
import com.iris.notification.retry.RetryManager;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationPriority;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;
import com.netflix.governator.annotations.WarmUp;

@Singleton
public class NotificationDispatcher implements Dispatcher {

    @Inject
    protected RetryManager retryManager;

    @Inject
    protected RetryProcessor retryProcessor;

    @Inject
    protected NotificationAuditor audit;

    @Inject
    protected NotificationProviderRegistry providerRegistry;

    private MethodDispatchStrategy methodDispatchStrategy;
    private PriorityDispatchStrategy priorityDispatchStrategy;
    private CriticalPriorityDispatchStrategy criticalPriorityDispatchStrategy;

    @WarmUp
    public void warmup() {
        methodDispatchStrategy = new MethodDispatchStrategy(audit, retryProcessor, retryManager, providerRegistry);
        priorityDispatchStrategy = new PriorityDispatchStrategy(audit, retryProcessor, retryManager, providerRegistry);
        criticalPriorityDispatchStrategy = new CriticalPriorityDispatchStrategy(audit, retryProcessor, retryManager, providerRegistry);
    }

    @Override
    public void dispatch(Notification notification) {

        // Task has expired; log failure and exit
        if (retryManager.hasExpired(notification)) {
            audit.log(notification, AuditEventState.FAILED);
            return;
        }

        // Select and invoke a dispatch strategy for this notification
        if (notification.getPriority() == null) {
            methodDispatchStrategy.dispatch(notification);
        } else {
      	  	if (notification.getPriority() == NotificationPriority.CRITICAL) {
      	  		criticalPriorityDispatchStrategy.dispatch(notification);	
      	  	} else {
      	  		priorityDispatchStrategy.dispatch(notification);
      	  	}
        }
    }

}

