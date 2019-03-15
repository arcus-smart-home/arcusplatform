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
package com.iris.platform.metrics;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.google.common.base.Splitter;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class MetricsServerConfig {
   @Inject(optional = true) @Named("kairos.url")
   protected String url = "http://kairosops.eyeris:8080";

   @Inject(optional = true) @Named("threads")
   protected int threads = 2*Runtime.getRuntime().availableProcessors();

   @Inject(optional = true) @Named("blacklist.name")
   protected String blackListedNames = "";

   @Inject(optional = true) @Named("blacklist.prefix")
   protected String blackListedPrefixes = "";

   @Inject(optional = true) @Named("blacklist.regex")
   protected String blackListedRegexes = "";

   @Inject(optional = true) @Named("blacklist.container")
   protected String blackListedContainers = "";

   @Inject(optional = true) @Named("blacklist.service")
   protected String blackListedServices = "";

   @Inject(optional = true) @Named("blacklist.host")
   protected String blackListedHosts = "";

   @Inject(optional = true) @Named("rollup.defaultTTL")
   protected long defaultTTL = TimeUnit.DAYS.toSeconds(21);

   @Inject(optional = true) @Named("rollup.mediumTTL")
   protected long mediumTTL = TimeUnit.DAYS.toSeconds(180);

   @Inject(optional = true) @Named("rollup.highTTL")
   protected long highTTL = TimeUnit.DAYS.toSeconds(1825);

   @Inject(optional = true) @Named("rollup.mediumTTLFrequencyMinutes")
   protected long mediumTTLFrequencyMinutes = 5;

   @Inject(optional = true) @Named("rollup.highTTLFrequencyMinutes")
   protected long highTTLFrequencyMinutes = TimeUnit.HOURS.toMinutes(12);

   @Inject(optional = true) @Named("metrics.cacheMax")
   private int metricsCacheMax = 50;
   
   @Inject(optional = true) @Named("kairos.postThreadsMax")
   private int kairosPostThreadsMax = 50;
   
   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   public int getThreads() {
      return threads;
   }

   public void setThreads(int threads) {
      this.threads = threads;
   }

   public String getBlackListedNames() {
      return blackListedNames;
   }

   public void setBlackListedNames(String blackListedNames) {
      this.blackListedNames = blackListedNames;
   }

   public String getBlackListedPrefixes() {
      return blackListedPrefixes;
   }

   public void setBlackListedPrefixes(String blackListedPrefixes) {
      this.blackListedPrefixes = blackListedPrefixes;
   }

   public String getBlackListedRegexes() {
      return blackListedRegexes;
   }

   public void setBlackListedRegexes(String blackListedRegexes) {
      this.blackListedRegexes = blackListedRegexes;
   }

   public String getBlackListedContainers() {
      return blackListedContainers;
   }

   public void setBlackListedContainers(String blackListedContainers) {
      this.blackListedContainers = blackListedContainers;
   }

   public String getBlackListedServices() {
      return blackListedServices;
   }

   public void setBlackListedServices(String blackListedServices) {
      this.blackListedServices = blackListedServices;
   }

   public String getBlackListedHosts() {
      return blackListedHosts;
   }

   public void setBlackListedHosts(String blackListedHosts) {
      this.blackListedHosts = blackListedHosts;
   }

   public Predicate<JsonObject> getBlackListFilter() {
      return getBlackListedServicesFilter().and(
             getBlackListedHostsFilter()).and(
             getBlackListedNamesFilter()).and(
             getBlackListedPrefixesFilter()).and(
             getBlackListedRegexFilter()).and(
             getBlackListedContainersFilter());
   }

   public Predicate<JsonObject> getBlackListedNamesFilter() {
      return MetricFilters.nameEquals(splitOnCommas(getBlackListedNames())).negate();
   }

   public Predicate<JsonObject> getBlackListedPrefixesFilter() {
      return MetricFilters.namePrefixedBy(splitOnCommas(getBlackListedPrefixes())).negate();
   }

   public Predicate<JsonObject> getBlackListedRegexFilter() {
      return MetricFilters.nameMatchesList(splitOnCommas(getBlackListedRegexes())).negate();
   }

   public Predicate<JsonObject> getBlackListedContainersFilter() {
      return MetricFilters.fromContainer(splitOnCommas(getBlackListedContainers())).negate();
   }

   public Predicate<JsonObject> getBlackListedServicesFilter() {
      return MetricFilters.fromService(splitOnCommas(getBlackListedServices())).negate();
   }

   public Predicate<JsonObject> getBlackListedHostsFilter() {
      return MetricFilters.fromHost(splitOnCommas(getBlackListedHosts())).negate();
   }

   public long getDefaultTTL(){return defaultTTL;}

   public long getMediumTTL(){return mediumTTL;}

   public long getHighTTL(){return highTTL;}

   public long getMediumTTLFrequencyMinutes(){return mediumTTLFrequencyMinutes;}

   public long getHighTTLFrequencyMinutes(){return highTTLFrequencyMinutes;}

   private List<String> splitOnCommas(String value) {
      return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
   }
	
	public int getMetricsCacheMax() {
		return metricsCacheMax;
	}
	
	public void setMetricsCacheMax(int metricsCacheMax) {
		this.metricsCacheMax = metricsCacheMax;
	}

	public int getKairosPostThreadsMax() {
		return kairosPostThreadsMax;
	}

	public void setKairosPostThreadsMax(int kairosPostThreadsMax) {
		this.kairosPostThreadsMax = kairosPostThreadsMax;
	}
}

