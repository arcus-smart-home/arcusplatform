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
package com.iris.voice.proactive;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class ProactiveReportingConfig {

   @Inject(optional = true)
   @Named("voice.proactive.reporting.max.threads")
   private int reportingMaxThreads = 20;

   @Inject(optional = true)
   @Named("voice.proactive.reporting.thread.keep.alive.ms")
   private long reportingThreadKeepAliveMs = TimeUnit.MINUTES.toMillis(5);

   public int getReportingMaxThreads() {
      return reportingMaxThreads;
   }

   public void setReportingMaxThreads(int reportingMaxThreads) {
      this.reportingMaxThreads = reportingMaxThreads;
   }

   public long getReportingThreadKeepAliveMs() {
      return reportingThreadKeepAliveMs;
   }

   public void setReportingThreadKeepAliveMs(long reportingThreadKeepAliveMs) {
      this.reportingThreadKeepAliveMs = reportingThreadKeepAliveMs;
   }
}

