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
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapModule;

public abstract class AbstractIrisHal implements IrisHalInternal {
   @Nullable
   protected File baseAgentDir;

   /////////////////////////////////////////////////////////////////////////////
   // Iris Hal Startup and Shutdown
   /////////////////////////////////////////////////////////////////////////////

   protected void startInitialServices(File base, Set<File> configs) {
      this.baseAgentDir = base;
      startStorageService(base);
      startDatabaseService();
      startConfigService(configs);
      startExecService();
   }

   protected void startExtraServices() {
      startAttributesService();
      startLifeCycleService();
      startBackupService();
      startHttpService();
      startUpnpService();
      startSceneService();
      startMetricsService();
   }

   @Override
   public void start(File base, Set<File> configs) {
      startInitialServices(base, configs);
      startExtraServices();
   }

   @Override
   public void shutdown() {
      shutdownUpnpService();
      shutdownHttpService();
      shutdownBackupService();
      shutdownLifeCycleService();
      shutdownAttributesService();
      shutdownExecService();
      shutdownConfigService();
      shutdownDatabaseService();
      shutdownStorageService();
      shutdownSceneService();
      shutdownMetricsService();

      this.baseAgentDir = null;
   }

   protected abstract void startStorageService(File base);
   protected abstract void startDatabaseService();
   protected abstract void startConfigService(Set<File> configs);
   protected abstract void startExecService();
   protected abstract void startAttributesService();
   protected abstract void startLifeCycleService();
   protected abstract void startHttpService();
   protected abstract void startUpnpService();
   protected abstract void startFourgService();
   protected abstract void startBackupService();
   protected abstract void startSceneService();
   protected abstract void startMetricsService();

   protected abstract void shutdownStorageService();
   protected abstract void shutdownDatabaseService();
   protected abstract void shutdownConfigService();
   protected abstract void shutdownExecService();
   protected abstract void shutdownAttributesService();
   protected abstract void shutdownLifeCycleService();
   protected abstract void shutdownHttpService();
   protected abstract void shutdownUpnpService();
   protected abstract void shutdownFourgService();
   protected abstract void shutdownBackupService();
   protected abstract void shutdownSceneService();
   protected abstract void shutdownMetricsService();

   /////////////////////////////////////////////////////////////////////////////
   // Application bootstrapping methods
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public Collection<? extends BootstrapModule> getBootstrapModules() {
      Collection<? extends BootstrapModule> instModules = new LinkedList<>();
      addAdditionalBootstrapModules(instModules);
      return instModules;
   }

   @Override
   public Collection<Class<? extends BootstrapModule>> getBootstrapModuleClasses() {
      Collection<Class<? extends BootstrapModule>> classModules = new LinkedList<>();
      addAdditionalBootstrapModuleClasses(classModules);
      return classModules;
   }

   @Override
   public Collection<? extends Module> getApplicationModules() {
      Collection<? extends Module> instModules = new LinkedList<>();
      addAdditionalModules(instModules);
      return instModules;
   }

   @Override
   public Collection<Class<? extends Module>> getApplicationModuleClasses() {
      Collection<Class<? extends Module>> classModules = new LinkedList<>();
      add(classModules, getRouterModuleClass());
      add(classModules, getGatewayModuleClass());
      add(classModules, getHubControllerModuleClass());
      add(classModules, getZWaveControllerModuleClass());
      add(classModules, getZigbeeControllerModuleClass());
      add(classModules, getSercommControllerModuleClass());
      add(classModules, get4gControllerModuleClass());
      add(classModules, getReflexControllerModuleClass());
      add(classModules, getAlarmControllerModuleClass());
      add(classModules, getHueControllerModuleClass());
      add(classModules, getSpyModuleClass());

      addAdditionalModuleClasses(classModules);
      return classModules;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Iris Agent Application Modules
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   protected abstract Class<? extends Module> getRouterModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getGatewayModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getHubControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getZWaveControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getZigbeeControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getSercommControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> get4gControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getReflexControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getAlarmControllerModuleClass();

   @Nullable
   protected abstract Class<? extends Module> getHueControllerModuleClass();
   
   @Nullable
   protected abstract Class<? extends Module> getSpyModuleClass();

   /////////////////////////////////////////////////////////////////////////////
   // Hooks for sub-classes
   /////////////////////////////////////////////////////////////////////////////

   protected void addAdditionalBootstrapModules(Collection<? extends BootstrapModule> modules) {
   }

   protected void addAdditionalBootstrapModuleClasses(Collection<Class<? extends BootstrapModule>> modules) {
   }

   protected void addAdditionalModules(Collection<? extends Module> modules) {
   }

   protected void addAdditionalModuleClasses(Collection<Class<? extends Module>> modules) {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   protected void addBootstrap(Collection<Class<? extends BootstrapModule>> classModules, @Nullable Class<? extends BootstrapModule> clazz) {
      if (clazz != null) {
         classModules.add(clazz);
      }
   }

   protected void add(Collection<Class<? extends Module>> classModules, @Nullable Class<? extends Module> clazz) {
      if (clazz != null) {
         classModules.add(clazz);
      }
   }

   protected void addBootstrap(Collection<BootstrapModule> modules, @Nullable BootstrapModule module) {
      if (module != null) {
         modules.add(module);
      }
   }

   protected void add(Collection<Module> modules, @Nullable Module module) {
      if (module != null) {
         modules.add(module);
      }
   }
}

