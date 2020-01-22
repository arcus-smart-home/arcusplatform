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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.iris.core.metricsexporter.config.IrisMetricsExporterConfig;
import com.iris.util.ThreadPoolBuilder;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IrisMetricsExporter {
   private static final Logger logger = LoggerFactory.getLogger(IrisMetricsExporter.class);

   private final ExecutorService executor;
   private final IrisMetricsExporterConfig config;

   @Inject
   public IrisMetricsExporter(
         MetricRegistry registry,
         IrisMetricsExporterConfig config
   ) {
      this.config = config;
      this.executor = Executors.newSingleThreadExecutor(
            ThreadPoolBuilder
                  .defaultFactoryBuilder()
                  .setNameFormat(String.format("metrics-exporter-%s", config.getMetricsHttpPort()))
                  .build()
      );

      CollectorRegistry.defaultRegistry.register(new IrisDropwizardExports(registry));
   }
   
   @PostConstruct
   public void start() {
      logger.info("Starting metrics server on port {}", config.getMetricsHttpPort());

      this.executor.execute(() -> {
         Server server = new Server(this.config.getMetricsHttpPort());
         ServletContextHandler context = new ServletContextHandler();
         context.setContextPath("/");
         server.setHandler(context);
         context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");

         try {
            server.start();
            server.join();
         } catch (Exception e) { // server.start throws Exception, so can't be more general here.
            logger.error("Failed to start metrics exporter", e);
         }
      });
   }
   
   @PreDestroy
   public void stop() {
      logger.info("Stopping topics exporter");
      executor.shutdownNow();
   }
}

