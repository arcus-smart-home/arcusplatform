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
package com.iris.notification;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class NotificationServiceConfig {
   @Inject(optional=true) 
   @Named("notificationservice.threads.max")
   private int maxThreads = 100;

   @Inject(optional=true) 
   @Named("notificationservice.threads.retry.max")
   private int maxRetryThreads = 1;

   @Inject(optional=true) 
   @Named("notificationservice.apns.threads") 
   private int apnsThreads = Runtime.getRuntime().availableProcessors();

   @Inject(optional=true) 
   @Named("notificationservice.apns.connections") 
   private int apnsConnections = 1;

   @Inject(optional = true)
   @Named("notificationservice.threads.keepalive")
   private int threadKeepAliveMs = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

   public int getMaxThreads() {
      return maxThreads;
   }

   public void setMaxThreads(int maxThreads) {
      this.maxThreads = maxThreads;
   }

   public int getMaxRetryThreads() {
      return maxRetryThreads;
   }

   public void setMaxRetryThreads(int maxRetryThreads) {
      this.maxRetryThreads = maxRetryThreads;
   }

   public int getApnsThreads() {
      return apnsThreads;
   }

   public void setApnsThreads(int apnsThreads) {
      this.apnsThreads = apnsThreads;
   }

   public int getApnsConnections() {
      return apnsConnections;
   }

   public void setApnsConnections(int apnsConnections) {
      this.apnsConnections = apnsConnections;
   }

   public int getThreadKeepAliveMs() {
      return threadKeepAliveMs;
   }

   public void setThreadKeepAliveMs(int threadKeepAliveMs) {
      this.threadKeepAliveMs = threadKeepAliveMs;
   }
}

