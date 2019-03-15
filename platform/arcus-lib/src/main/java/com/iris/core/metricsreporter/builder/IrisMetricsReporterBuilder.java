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
package com.iris.core.metricsreporter.builder;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.iris.core.messaging.kafka.KafkaOpsConfig;
import com.iris.core.metricsreporter.config.IrisMetricsReporterConfig;
import com.iris.core.metricsreporter.reporter.IrisMetricsTopicReporter;

/**
 *
 */
public interface IrisMetricsReporterBuilder {
    public IrisMetricsTopicReporter build();
    public void create (MetricRegistry registry);
    public void convertRatesTo(TimeUnit rateUnit);
    public void convertDurationsTo(TimeUnit durationUnit);
    public void filter(MetricFilter filter);
    public void setConfig(KafkaOpsConfig config);
    public void setReporterConfig (IrisMetricsReporterConfig config);
    public IrisMetricsReporterConfig getReporterConfig ();
}

