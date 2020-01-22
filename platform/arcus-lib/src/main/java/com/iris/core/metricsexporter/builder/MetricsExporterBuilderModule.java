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
package com.iris.core.metricsexporter.builder;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.IrisApplication;
import com.iris.core.metricsexporter.exporter.IrisMetricsExporter;
import com.iris.metrics.IrisMetrics;

/**
 *
 */
@Singleton
public class MetricsExporterBuilderModule extends AbstractIrisModule {

   @Inject
   public MetricsExporterBuilderModule(
         IrisApplication application // ensure IrisApplication has been loaded to set some of the info
   ) {
   }

   @Override
   protected void configure() {
      bind(MetricRegistry.class).toInstance(IrisMetrics.registry());
      bind(IrisMetricsExporter.class).asEagerSingleton();
   }

}


