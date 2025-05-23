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
package com.iris.hubcom.server;

import java.util.Arrays;
import java.util.Collection;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.iris.bridge.server.BridgeServer;
import com.iris.bridge.server.ServerRunner;
import com.iris.bridge.server.cluster.ClusterAwareServerModule;
import com.iris.bridge.server.http.health.HttpHealthCheckModule;
import com.iris.core.IrisAbstractApplication;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.dao.file.HubBlacklistDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.metricsexporter.builder.MetricsExporterBuilderModule;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;

public class HubServer extends BridgeServer {

	@Inject
	public HubServer(ServerRunner runner) {
	   super(runner);
	}

	public static void main(String[] args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
         HubServerModule.class,
         ClusterAwareServerModule.class,
         KafkaModule.class,
         HubBlacklistDAOModule.class,
         CassandraResourceBundleDAOModule.class,
         PopulationAwareFirmwareModule.class,
         MetricsTopicReporterBuilderModule.class,
			MetricsExporterBuilderModule.class,
         HttpHealthCheckModule.class
      );

      IrisAbstractApplication.exec(HubServer.class, modules, args);
	}
}

