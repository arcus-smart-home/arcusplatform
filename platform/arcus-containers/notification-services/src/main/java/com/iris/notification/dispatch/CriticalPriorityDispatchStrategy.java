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
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

public class CriticalPriorityDispatchStrategy extends AbstractDispatcher {

   private static final Logger logger = LoggerFactory.getLogger(CriticalPriorityDispatchStrategy.class);

    public CriticalPriorityDispatchStrategy(NotificationAuditor audit, RetryProcessor retryProcessor, RetryManager retryManager, NotificationProviderRegistry providerRegistry) {
        super(audit, retryProcessor, retryManager, providerRegistry);
    }

    public void dispatch(Notification notification) {

        // Is this our first attempt at delivering this notification, then clone the notification to
   	  // all types and then attempt to dispatch each
        if (notification.getMethod() == null) {

      	  // ... clone the notification and set delivery methods
      	  Notification ivrNotification = notification.copy(NotificationMethod.IVR);
      	  dispatch(ivrNotification);

      	  Notification pushNotification = notification.copy(NotificationMethod.PUSH);
      	  dispatch(pushNotification);

      	  Notification emailNotification = notification.copy(NotificationMethod.EMAIL);
      	  dispatch(emailNotification);

        } else {

	        // Send the message to the gateway...
	        try {
	            sendNotification(notification);
	        }
	        catch (DispatchUnsupportedByUserException | NoSuchProviderException e) {
	            // ... if we fail as a result of the custom not being provisioned to accept notifications using the default method,
	            // then do nothing for critical notifications -- fallback is not supported because all possible endpoints
	      	   // will be tried anyway.
	           logger.error("Failed to dispatch notification: {}:", notification, e);
	            instrumentFailedDispatch(notification.getMethod(), e);
	            audit.log(notification, AuditEventState.FAILED, e);
	        }
	        catch (DispatchException e) {
	            // Technical problem delivering the notification;
	           logger.error("Failed to dispatch notification: {}:", notification, e);
	            instrumentFailedDispatch(notification.getMethod(), e);
	            retryProcessor.retry(notification, e);
	        }
	        catch (RuntimeException e) {
	            logger.error("Failed unexpectedly during dispatch of notification {}:", notification, e);
	            // Something unexpectedly catastrophic happened; terminal state
	            instrumentFailedDispatch(notification.getMethod(), e);
	            audit.log(notification, AuditEventState.FAILED, e);
	        }
        }
    }

    private void fallback(Notification notification, Exception reason) {
        // fallback is not supported for critical messages because all possible
   	  // notification methods will be tried for this user on this notification anyway
    }

}

