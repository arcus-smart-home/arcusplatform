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
package com.iris.notification.retry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Counter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.notification.NotificationService;
import com.iris.notification.NotificationServiceConfig;
import com.iris.notification.dispatch.DispatchTask;
import com.iris.notification.dispatch.Dispatcher;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.audit.AuditEventState;
import com.iris.platform.notification.audit.NotificationAuditor;

/**
 * A note about the threading model:
 *
 * All of the notification dispatching is suppose to happen on a single thread pool. When
 * a notification is initially received from the message bus the dispatch is immediately
 * attempted on this thread pool, but if the notification needs to be split or retried then
 * that code will call into one of the methods in this class.
 *
 * Since we don't want to deadlock, this class cannot immediately place the retry attempt
 * or split notification back into the same thread pool. Instead a much smaller thread
 * pool with an infinite sized queue is used with the tasks on this thread pool simply
 * scheduling a dispatch on the notification dispatching thread pool.
 */
@Singleton
public class ScheduledRetryProcessor implements RetryProcessor {
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(NotificationService.SERVICE_NAME);
   private final Counter retryCounter = METRICS.counter("retry.count");

   protected final RetryManager retryManager;
   protected final NotificationAuditor auditor;
   private final Dispatcher dispatcher;

   private final ScheduledExecutorService scheduler;
   private final ExecutorService executor;

   @Inject
   public ScheduledRetryProcessor(
      NotificationServiceConfig config,
      RetryManager retryManager,
      NotificationAuditor auditor,
      Dispatcher dispatcher,
	   @Named("notifications.executor") ExecutorService executor
      ) {
      this.retryManager = retryManager;
      this.auditor = auditor;
      this.dispatcher = dispatcher;
      this.executor = executor;

      ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("notifications-retry-%d").build();
      this.scheduler = Executors.newScheduledThreadPool(config.getMaxRetryThreads(), factory);
   }

    @Override
    public void retry(Notification notification, Exception reason) {
        auditor.log(notification, AuditEventState.RETRY, reason);
        retryCounter.inc();

        int delayUntilRetry = retryManager.secondsBeforeRetry(notification);
        scheduler.schedule(() -> executor.submit(new DispatchTask(notification, dispatcher)), delayUntilRetry, TimeUnit.SECONDS);
    }

    /**
     * Spawns a child "process" for the given notification.
     *
     * Given a notification, this method duplicates it and sets the copied notification's method and device endpoint values to
     * those provided, then dispatches it in a separate thread (with no delay). Some notifications are "logical" that is, they
     * represent more than one physical messages being delivered. There are two types of logical notifications:
     *
     * 1. Notifications whose method is PUSH which indicate that a message should be delivered to all mobile devices owned by the
     * person, both Android and iOS.
     *
     * 2. Notifications whose method is GCM or APNS which indicate that a message should be delivered to all Android or iOS
     * devices, respectively, owned by the person.
     */
    @Override
    public void split(Notification notification, NotificationMethod method, String deviceEndpoint) {
        Notification splitNotification = notification.copy(method, deviceEndpoint);
        scheduler.submit(() -> executor.submit(new DispatchTask(splitNotification, dispatcher)));
    }
}

