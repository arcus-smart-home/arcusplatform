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
/**
 *
 */
package com.iris.platform.history;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 *
 */
public class HistoryAppenderConfig {

   @Inject(optional=true) @Named("history.threads.max")
   private int maxThreads = 20;
   @Inject(optional=true) @Named("history.threads.keepalive")
   private int threadKeepAliveMs = 300000;
   @Inject(optional=true) @Named("history.namecache.concurrency")
   private int nameCacheConcurrency = 16;
   @Inject(optional=true) @Named("history.namecache.maxsize")
   private int nameCacheMaxSize = 10000;
   @Inject(optional=true) @Named("history.namecache.expireminutes")
   private int nameCacheExpireMinutes = 24 * 60;

   @Inject(optional=true) @Named("history.critical.place.ttlhours")
   private int criticalPlaceTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.detailed.place.ttlhours")
   private int detailedPlaceTtlHours = 31 * 24;
   @Inject(optional=true) @Named("history.detailed.person.ttlhours")
   private int detailedPersonTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.detailed.rule.ttlhours")
   private int detailedRuleTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.detailed.device.ttlhours")
   private int detailedDeviceTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.detailed.hub.ttlhours")
   private int detailedHubTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.detailed.subsys.ttlhours")
   private int detailedSubsysTtlHours = 15 * 24;
   @Inject(optional=true) @Named("history.activity.subsys.ttlhours")
   private int activitySubsysTtlHours = 31 * 24;
   @Inject(optional=true) @Named("history.detailed.alarm.ttlhours")
   private int detailedAlarmTtlHours = 15 * 24;

   @Inject(optional=true) @Named("history.activity.bucket.sizesec")
   private int activityBucketSizeSec = 5;
   
   /**
    * @return the maxThreads
    */
   public int getMaxThreads() {
      return maxThreads;
   }

   /**
    * @return the threadKeepAliveMs
    */
   public int getThreadKeepAliveMs() {
      return threadKeepAliveMs;
   }

   /**
    * @return the nameCacheConcurrency
    */
   public int getNameCacheConcurrency() {
      return nameCacheConcurrency;
   }

   /**
    * @return the nameCacheMaxSize
    */
   public int getNameCacheMaxSize() {
      return nameCacheMaxSize;
   }

   /**
    * @return the expiration time in minutes for a name cache entry
    */
   public int getNameCacheExpireMinutes() {
      return nameCacheExpireMinutes;
   }

   /**
    * @return the criticalPlaceTtlHours
    */
   public int getCriticalPlaceTtlHours() {
      return criticalPlaceTtlHours;
   }

   /**
    * @return the detailedPlaceTtlHours
    */
   public int getDetailedPlaceTtlHours() {
      return detailedPlaceTtlHours;
   }

   /**
    * @return the detailedPersonTtlHours
    */
   public int getDetailedPersonTtlHours() {
      return detailedPersonTtlHours;
   }

   /**
    * @return the detailedRuleTtlHours
    */
   public int getDetailedRuleTtlHours() {
      return detailedRuleTtlHours;
   }

   /**
    * @return the detailedDeviceTtlHours
    */
   public int getDetailedDeviceTtlHours() {
      return detailedDeviceTtlHours;
   }

   /**
    * @return the detailedHubTtlHours
    */
   public int getDetailedHubTtlHours() {
      return detailedHubTtlHours;
   }
   
   /**
    * @return the detailedDeviceTtlHours
    */
   public int getDetailedSubsysTtlHours() {
      return detailedSubsysTtlHours;
   }
   
   /**
    * @return the activitySubsystemTtlHours
    */
   public int getActivitySubsysTtlHours() {
   	return activitySubsysTtlHours;
   }

	public int getActivityBucketSizeSec() {
		return activityBucketSizeSec;
	}

	public void setActivityBucketSizeSec(int activityBucketSizeSec) {
		this.activityBucketSizeSec = activityBucketSizeSec;
	}

   public int getDetailedAlarmTtlHours() {
      return detailedAlarmTtlHours;
   }

   public void setDetailedAlarmTtlHours(int detailedAlarmTtlHours) {
      this.detailedAlarmTtlHours = detailedAlarmTtlHours;
   }
}

