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
package com.iris.agent.hal;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.backup.BackupService;
import com.iris.agent.config.ConfigService;
import com.iris.agent.controller.hub.HubController;
import com.iris.agent.db.DbService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.gateway.Gateway;
import com.iris.agent.http.HttpService;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.metrics.MetricsService;
import com.iris.agent.router.Router;
import com.iris.agent.scene.SceneService;
import com.iris.agent.storage.StorageService;
import com.iris.agent.upnp.IrisUpnpService;

public abstract class AbstractIrisHalCommon extends AbstractIrisHal {
   /////////////////////////////////////////////////////////////////////////////
   // Iris Hal Startup and Shutdown
   /////////////////////////////////////////////////////////////////////////////

   @Override
   protected void startStorageService(File base) {
      // TODO: make the watch interval configurable???
      StorageService.start(1000, TimeUnit.MILLISECONDS);
   }

   @Override
   protected void startDatabaseService() {
      DbService.start();
   }

   @Override
   protected void startConfigService(Set<File> configs) {
      ConfigService.start(configs);
   }

   @Override
   protected void startExecService() {
      ExecService.start();
   }

   @Override
   protected void startAttributesService() {
      HubAttributesService.start();
   }

   @Override
   protected void startLifeCycleService() {
      LifeCycleService.start();
   }

   @Override
   protected void startHttpService() {
      HttpService.start();
   }

   @Override
   protected void startUpnpService() {
      IrisUpnpService.start();
   }

   @Override
   protected void startFourgService() {
      FourgService.start();
   }

   @Override
   protected void startBackupService() {
      BackupService.start();
   }

   @Override
   protected void startSceneService() {
      SceneService.start();
   }

   @Override
   protected void startMetricsService() {
      MetricsService.start();
   }

   @Override
   protected void shutdownStorageService() {
      StorageService.shutdown();
   }

   @Override
   protected void shutdownDatabaseService() {
      DbService.shutdown();
   }

   @Override
   protected void shutdownConfigService() {
      ConfigService.shutdown();
   }

   @Override
   protected void shutdownExecService() {
      ExecService.shutdown();
   }

   @Override
   protected void shutdownAttributesService() {
      HubAttributesService.shutdown();
   }

   @Override
   protected void shutdownLifeCycleService() {
      LifeCycleService.shutdown();
   }

   @Override
   protected void shutdownHttpService() {
      HttpService.shutdown();
   }

   @Override
   protected void shutdownUpnpService() {
      IrisUpnpService.shutdown();
   }

   @Override
   protected void shutdownFourgService() {
      FourgService.shutdown();
   }

   @Override
   protected void shutdownBackupService() {
      BackupService.shutdown();
   }

   @Override
   protected void shutdownSceneService() {
      SceneService.shutdown();
   }

   @Override
   protected void shutdownMetricsService() {
      MetricsService.shutdown();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Iris Agent Application Modules
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   @Override
   protected Class<? extends Module> getRouterModuleClass() {
      return DefaultRouterModule.class;
   }

   @Nullable
   @Override
   protected Class<? extends Module> getGatewayModuleClass() {
      return DefaultGatewayModule.class;
   }

   @Nullable
   @Override
   protected Class<? extends Module> getHubControllerModuleClass() {
      return DefaultHubControllerModule.class;
   }

   @Nullable
   @Override
   protected Class<? extends Module> getZWaveControllerModuleClass() {
      return DefaultZWaveControllerModule.class;
   }

   protected static class DefaultRouterModule extends AbstractModule {
      @Override
      protected void configure() {
         bind(Router.class).in(Singleton.class);
      }
   }

   protected static class DefaultGatewayModule extends AbstractModule {
      @Override
      protected void configure() {
         bind(Gateway.class).in(Singleton.class);
      }
   }

   protected static class DefaultZWaveControllerModule extends AbstractModule {
      @Override
      protected void configure() {
         //bind(ZWaveController.class).to(MockZWaveController.class).in(Singleton.class);
      }
   }

   protected static class DefaultHubControllerModule extends AbstractModule {
      @Override
      protected void configure() {
         bind(HubController.class).in(Singleton.class);
      }
   }
}

