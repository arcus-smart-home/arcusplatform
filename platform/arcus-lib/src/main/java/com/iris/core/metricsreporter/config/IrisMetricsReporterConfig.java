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
package com.iris.core.metricsreporter.config;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricFilter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class IrisMetricsReporterConfig {
    @Inject(optional = true)
    @Named("metrics.topic.enabled")
    protected boolean enabled = true;

    @Inject(optional = true)
    @Named("metrics.topic.rateunit")
    protected TimeUnit topicRateUnit = TimeUnit.SECONDS;

    @Inject(optional = true)
    @Named("metrics.topic.durationunit")
    protected TimeUnit topicDurationUnit = TimeUnit.MILLISECONDS;


    @Inject(optional = true)
    @Named("metrics.topic.filter")
    protected MetricFilter topicFilter = MetricFilter.ALL;

    @Inject(optional = true)
    @Named("metrics.topic.filter")
    protected String topicName= "iris-metrics-topicreporter";

    @Inject(optional = true)
    @Named("metrics.topic.reportingunittype")
    protected TimeUnit reportingUnitType = TimeUnit.SECONDS;

    @Inject(optional = true)
    @Named("metrics.topic.reportingunit")
    protected long reportingUnit = 15L;

    @Inject(optional = true)
    @Named("metrics.topic.batchsize")
    protected int batchSize = 1000;

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TimeUnit getReportingUnitType() {
        return reportingUnitType;
    }

    public void setReportingUnitType(TimeUnit reportingUnitType) {
        this.reportingUnitType = reportingUnitType;
    }

    public long getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(long reportingUnit) {
        this.reportingUnit = reportingUnit;
    }

    public TimeUnit getTopicRateUnit() {
        return topicRateUnit;
    }

    public void setTopicRateUnit(TimeUnit topicRateUnit) {
        this.topicRateUnit = topicRateUnit;
    }

    public TimeUnit getTopicDurationUnit() {
        return topicDurationUnit;
    }

    public void setTopicDurationUnit(TimeUnit topicDurationUnit) {
        this.topicDurationUnit = topicDurationUnit;
    }

    public MetricFilter getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(MetricFilter topicFilter) {
        this.topicFilter = topicFilter;
    }
    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

   /**
    * @return the batchSize
    */
   public int getBatchSize() {
      return batchSize;
   }

   /**
    * @param batchSize the batchSize to set
    */
   public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
   }

}

