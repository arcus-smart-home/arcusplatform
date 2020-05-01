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
package com.iris.notification.upstream;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.messages.model.MobileDevice;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.retry.RetryProcessor;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

@Singleton
public class IrisUpstreamNotificationResponder implements UpstreamNotificationResponder {

   private static final String ERROR_COUNT_SUFFIX = ".error.count";
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);
   private static final Logger logger = LoggerFactory.getLogger(IrisUpstreamNotificationResponder.class);

   @Inject
   private MobileDeviceDAO mobileDeviceDao;

   private final NotificationAuditor auditor;
   private final RetryProcessor retryProcessor;
   private final Map<NotificationMethod, Counter> errorCounters;

   @Inject
   public IrisUpstreamNotificationResponder(NotificationAuditor auditor, RetryProcessor retryProcessor) {
      this.auditor = auditor;
      this.retryProcessor = retryProcessor;

      //Preinitialize the errors counters so we can see them in our tools even when errors haven't happened yet.
      this.errorCounters = new HashMap<NotificationMethod, Counter>();
      for (NotificationMethod method : NotificationMethod.values()) {
         Counter counter = METRICS.counter(method.toString().toLowerCase() + ERROR_COUNT_SUFFIX);
         errorCounters.put(method, counter);
      }
   }

   @Override
   public void handleError(Notification notification, boolean isRecoverable, String message) {
      logger.warn("Error sending notification {}:  {}", notification, message);
      errorCounters.get(notification.getMethod()).inc();
      if (isRecoverable) {
         retryProcessor.retry(notification, new DispatchException(message));
      } else {
         auditor.log(notification, AuditEventState.ERROR, message);
      }
   }


   @Override
   public void handleHandOff(Notification notification) {
      METRICS.counter(notification.getMethod().toString().toLowerCase() + ".handoff.count").inc();
      auditor.log(notification, AuditEventState.ACCEPTED);
      instrumentSuccessfulDispatch(notification.getMethod(), notification.getInflightDuration());
   }

   /**
    * Increments the success counter associated with this notification method. Metric is named dispatch.{method}, i.e.,
    * "notifications.dispatch.apns"
    *
    * @param method The NotificationMethod by which the notification was dispatched.
    */
   protected void instrumentSuccessfulDispatch(NotificationMethod method, Duration inflightDuration) {
      String counterName = "dispatch." + method.toString().toLowerCase();
      String timerName = "dispatch." + method.toString().toLowerCase() + ".latency";

      IrisMetrics.metrics(NotificationService.SERVICE_NAME).counter(counterName).inc();
      IrisMetrics.metrics(NotificationService.SERVICE_NAME).timer(timerName).update(inflightDuration.toMillis(), TimeUnit.MILLISECONDS);
   }

   @Override
   public void handleDeliveryReceipt(Notification notification) {
      METRICS.counter(notification.getMethod().toString().toLowerCase() + ".deliveryreceipt.count").inc();
      auditor.log(notification, AuditEventState.DELIVERED);
   }

   @Override
   public void handleDeviceUnregistered(NotificationMethod method, String deviceIdentifier) {

      METRICS.counter(method.toString().toLowerCase() + ".deviceexpired.count").inc();

      // delete the device associated with this token/id

      // findWithToken will do a lookup in the mobile devices index table to
      // find the unique record matching the index
      MobileDevice device = mobileDeviceDao.findWithToken(deviceIdentifier);
      if (device != null) {
         /*
          * remove the device and the token to prevent further push notification attempts
          */
         mobileDeviceDao.delete(device);
      }
   }
}

