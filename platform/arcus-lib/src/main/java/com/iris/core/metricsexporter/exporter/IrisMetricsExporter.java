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
package com.iris.core.metricsexporter.exporter;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.iris.core.metricsexporter.config.IrisMetricsExporterConfig;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

public class IrisMetricsExporter {
   private static final Logger logger = LoggerFactory.getLogger(IrisMetricsExporter.class);

   private final IrisMetricsExporterConfig config;
   private HTTPServer server;

   @Inject
   public IrisMetricsExporter(
         MetricRegistry registry,
         IrisMetricsExporterConfig config
   ) {
      this.config = config;
      CollectorRegistry.defaultRegistry.register(new IrisDropwizardExports(registry));
   }

   @PostConstruct
   public void start() {
      logger.info("Starting metrics server on port {}", config.getMetricsHttpPort());
      try {
         server = new HTTPServer(config.getMetricsHttpPort());
      } catch (IOException e) {
         logger.error("Failed to start metrics exporter", e);
      }
   }

   @PreDestroy
   public void stop() {
      logger.info("Stopping metrics exporter");
      if (server != null) {
         server.stop();
      }
   }
}
