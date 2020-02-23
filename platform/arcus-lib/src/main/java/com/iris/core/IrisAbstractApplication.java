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
package com.iris.core;

import java.security.Security;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.platform.partition.Partitioner;
import com.iris.util.Subscription;

public abstract class IrisAbstractApplication {
   private static final Logger logger = LoggerFactory.getLogger(IrisAbstractApplication.class);

   @Inject @Named(IrisApplicationModule.NAME_APPLICATION_NAME)
   private String applicationName = "<unknown>";
   @Inject @Named(IrisApplicationModule.NAME_APPLICATION_VERSION)
   private String applicationVersion = "<unknown>";
   @Inject @Named(IrisApplicationModule.NAME_APPLICATION_DIR)
   private String applicationDir = "";

   public static class Arguments {
      @Parameter(
         names = { "-c", "--config" },
         description = "Absolute path to the configuration file.  Optional",
         arity = 1)
      private String configFile;

      //@Parameter(
      //   names = {"-m", "--modules" },
      //   description = "List of modules to wire together to create the application.  Optional and if not provided classpath scanning will be used",
      //   variableArity = true)
      //private String[] modules = new String[0];
   }

   /**
    * @return the applicationName
    */
   public String getApplicationName() {
      return applicationName;
   }

   /**
    * @return the applicationVersion
    */
   public String getApplicationVersion() {
      return applicationVersion;
   }

   /**
    * @return the applicationDir
    */
   public String getApplicationDir() {
      return applicationDir;
   }

   protected abstract void start() throws Exception;
   
   protected void awaitClusterId() throws InterruptedException {
      final CountDownLatch clusterIdLatch = new CountDownLatch(1);
      Subscription s =
         ServiceLocator
            // don't inject -- not all services have a Partitioner and that's OK
            .getInstance(Partitioner.class)
            // HACK listen for partitions to be assigned because we can't dynamically
            //      add a cluster id listener
            .addPartitionListener((event) -> { 
               if(!event.getPartitions().isEmpty()) {
                  clusterIdLatch.countDown();
               }
            });
      if(!clusterIdLatch.await(0, TimeUnit.MILLISECONDS)) {
         logger.info("Waiting for cluster id before starting");
         clusterIdLatch.await();
      }
      s.remove();
   }

   public static void exec(Class<? extends IrisAbstractApplication> appClazz, Collection<Class<? extends Module>> modules, String... args) {
      Arguments arguments = new Arguments();
      JCommander jc = new JCommander(arguments);
      jc.setAcceptUnknownOptions(true);
      try {
         jc.parse(args);
      }
      catch(RuntimeException e) {
         System.out.println("Error parsing arguments: " + e.getMessage());
         jc.usage();
         System.exit(-1);
      }

      exec(appClazz, modules, arguments);
   }


   public static void exec(Class<? extends IrisAbstractApplication> appClazz, Collection<Class<? extends Module>> modules, Arguments arguments) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();

      try {
         Bootstrap.Builder builder = Bootstrap.builder();
         if(arguments.configFile != null) {
            builder.withConfigPaths(arguments.configFile);
         }
         builder.withConstants(mapFromArgs(arguments));
         builder.withBootstrapModules(new IrisApplicationModule());
         builder.withModuleClasses(modules);
         
         //if(arguments.modules != null) {
         //   builder.withModuleClassnames(arguments.modules);
         //}

         Injector injector = builder.build().bootstrap();
         ServiceLocator.init(GuiceServiceLocator.create(injector));
         Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
               System.out.println("Shutting down...");
               ServiceLocator.destroy();
            }
         }));

         // XXX: is this the right place?
         Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

         // If a security manager is present then initialize it
         List<? extends SecurityManager> secManager = ServiceLocator.getInstancesOf(SecurityManager.class);
         if (secManager != null && !secManager.isEmpty()) {
            if (secManager.size() > 1) {
               logger.warn("more than one security manager is installed, using first: {}", secManager);
            }

            logger.info("installing configured security manager");
            SecurityUtils.setSecurityManager(secManager.get(0));
         }

         // Startup the application if its present
         IrisAbstractApplication app = null;
         if(appClazz != null) {
            app = ServiceLocator.getInstance(appClazz);
         }

         if (app != null) {
            logger.info(
                  "starting configured application:\n\t{} v{} [application directory: {}]",
                  app.getApplicationName(),
                  app.getApplicationVersion(),
                  app.getApplicationDir()
            );

            if (app.getApplicationName().equals(IrisApplicationModule.DEFAULT_APPLICATION_NAME)) {
               logger.error("Application cannot start without a name");
               System.exit(1);
            }
            StartupListener.publishStarted();
            app.start();
         }
      } catch(Exception e) {
         System.err.println(e.getMessage() + "\n");
         e.printStackTrace(System.err);
         logger.error("Application failed to start", e);
         System.exit(1);
      }
   }
   
   private static Map<String,String> mapFromArgs(Arguments args) {
   	Map<String, String> properties = new HashMap<String,String>();
   	try {
   		properties = BeanUtils.describe(args);
   		if (properties.containsKey("class")) {
   			properties.remove("class");
   		}
   	} catch (Exception e) {}
   	return ImmutableMap.copyOf(properties);
   }
}

