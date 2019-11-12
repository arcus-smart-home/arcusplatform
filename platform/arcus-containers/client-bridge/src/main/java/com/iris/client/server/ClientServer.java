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
package com.iris.client.server;

import java.util.Arrays;
import java.util.Collection;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.server.BridgeServer;
import com.iris.bridge.server.ServerRunner;
import com.iris.bridge.server.cluster.ClusterAwareServerModule;
import com.iris.client.server.healthcheck.ClientBridgeHealthCheckModule;
import com.iris.client.server.session.ClientBridgeSecurityModule;
import com.iris.core.IrisAbstractApplication;
import com.iris.core.dao.cassandra.CassandraDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.metricsexporter.builder.MetricsExporterBuilderModule;
import com.iris.core.metricsreporter.builder.MetricsTopicReporterBuilderModule;
import com.iris.platform.subsystem.SubsystemDaoModule;
import com.iris.security.authz.AuthzModule;

public class ClientServer extends BridgeServer {

   @Inject
   public ClientServer(ServerRunner runner) {
      super(runner);
   }

   @Override
   protected void start() throws Exception {
      // Initialize Shiro
      SecurityUtils.setSecurityManager(ServiceLocator.getInstance(SecurityManager.class));
      super.start();
   }

   public static void main(String... args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(
         ClientServerModule.class,
         ClusterAwareServerModule.class,
         KafkaModule.class,
         CassandraDAOModule.class,
         ClientBridgeSecurityModule.class,
         AuthzModule.class,
         MetricsTopicReporterBuilderModule.class,
         MetricsExporterBuilderModule.class,
         ClientBridgeHealthCheckModule.class,
         SubsystemDaoModule.class
      );

      IrisAbstractApplication.exec(ClientServer.class, modules, args);
   }
}

