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
package com.iris.core.metricsexporter.config;

import com.codahale.metrics.MetricFilter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


@Singleton
public class IrisMetricsExporterConfig {
    @Inject(optional = true)
    @Named("metrics.http.port")
    protected int metricsHttpPort = 9100;

    @Inject(optional = true)
    @Named("metrics.topic.filter")
    protected MetricFilter topicFilter = MetricFilter.ALL;

    public int getMetricsHttpPort() {
        return this.metricsHttpPort;
    }

    public void setMetricsHttpPort(int port) {
        this.metricsHttpPort = port;
    }

    public MetricFilter getTopicFilter() {
        return topicFilter;
    }

    public void setTopicFilter(MetricFilter topicFilter) {
        this.topicFilter = topicFilter;
    }

}

