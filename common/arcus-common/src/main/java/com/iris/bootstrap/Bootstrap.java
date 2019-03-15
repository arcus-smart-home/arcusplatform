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
package com.iris.bootstrap;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Bootstrap {

   private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

   public static final String PROP_MODULES = "bootstrap.modules";

   public static Builder builder() {
      return new Builder();
   }

   public static class Builder {

      private final Set<BootstrapModule> bootstrapModules = new HashSet<>();
      private final Set<Class<? extends Module>> moduleClasses = new HashSet<>();
      private final Set<Module> modules = new HashSet<>();
      private final Set<File> configs = new HashSet<>();
      private final Map<String,String> constants = new HashMap<String,String>();

      public Builder withBootstrapModules(BootstrapModule... modules) {
         if(modules != null) {
            return withBootstrapModules(Arrays.asList(modules));
         }
         return this;
      }

      public Builder withBootstrapModules(Collection<BootstrapModule> bootstrapModules) {
         if(bootstrapModules  != null) {
            this.bootstrapModules.addAll(bootstrapModules);
         }
         return this;
      }

      public Builder withModuleClassnames(String... classes) throws ClassNotFoundException {
         if(classes != null) {
            return withModuleClassnames(Arrays.asList(classes));
         }
         return this;
      }

      public Builder withModuleClassnames(Collection<String> classes) throws ClassNotFoundException {
         if(classes != null) {
            Set<Class<? extends Module>> moduleClasses = classnamesToClasses(classes);
            this.moduleClasses.addAll(moduleClasses);
         }

         return this;
      }

      @SuppressWarnings("unchecked")
      public Builder withModuleClasses(Class<? extends Module>... classes) {
         if(classes != null) {
            return withModuleClasses(Arrays.asList(classes));
         }
         return this;
      }

      public Builder withModuleClasses(Collection<Class<? extends Module>> classes) {
         if(classes != null) {
            moduleClasses.addAll(classes);
         }
         return this;
      }

      public Builder withModules(Module... modules) {
         if(modules != null) {
            return withModules(Arrays.asList(modules));
         }
         return this;
      }

      public Builder withModules(Collection<Module> modules) {
         if(modules != null) {
            this.modules.addAll(modules);
         }
         return this;
      }

      public Builder withConfigPaths(String... configFiles) {
         if(configFiles != null) {
            return withConfigPaths(Arrays.asList(configFiles));
         }
         return this;
      }

      public Builder withConfigPaths(Collection<String> configFiles) {
         if(configFiles != null) {
            List<File> files = new LinkedList<File>();
            for(String configFile : configFiles) {
               File file = new File(configFile);
               if(file.exists() && file.canRead()) {
                  files.add(file);
               } else {
                  LOGGER.warn("File {} could not be read and will be ignored", configFile);
               }
            }
            return withConfigFiles(files);
         }
         return this;
      }
      
      public Builder withConstants(Map<String,String> constants) {
      	if (constants != null) { 
      		this.constants.putAll(constants);
      	}
      	return this;
      }

      public Builder withConfigFiles(File... configFiles) {
         if(configFiles != null) {
            return withConfigFiles(Arrays.asList(configFiles));
         }
         return this;
      }

      public Builder withConfigFiles(Collection<File> configFiles) {
         if(configFiles != null) {
            configs.addAll(configFiles);
         }
         return this;
      }

      public Bootstrap build() {
         Bootstrap bootstrap = new Bootstrap();
         bootstrap.boostrapModules.addAll(bootstrapModules);
         bootstrap.moduleClasses.addAll(moduleClasses);
         bootstrap.modules.addAll(modules);
         bootstrap.configs.addAll(configs);
         bootstrap.constants.putAll(constants);
         return bootstrap;
      }
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private static Set<Class<? extends Module>> classnamesToClasses(Collection<String> classes) throws ClassNotFoundException {
      Set<Class<? extends Module>> moduleClasses = new HashSet<>();
      for(String className : classes) {
         if (className.contains(",")) {
            String[] currentClassNames = className.split(",");
            for (String aClassName : currentClassNames) {
               Class clazz = Class.forName(aClassName);
               if(Module.class.isAssignableFrom(clazz)) {
                  moduleClasses.add(clazz);
               } else {
                  LOGGER.warn("Additional class {} is not an implementation of Module and will be skipped", className);
               }
            }
         } else {
            Class clazz = Class.forName(className);
            if(Module.class.isAssignableFrom(clazz)) {
               moduleClasses.add(clazz);
            } else {
               LOGGER.warn("Additional class {} is not an implementation of Module and will be skipped", className);
            }
         }
      }
      return moduleClasses;
   }


   private final Set<BootstrapModule> boostrapModules = new HashSet<>();
   private final Set<Class<? extends Module>> moduleClasses = new HashSet<>();
   private final Set<Module> modules = new HashSet<>();
   private final Set<File> configs = new HashSet<>();
   private final Map<String,String> constants = new HashMap<String,String>();

   private Bootstrap() {
   }

   public Injector bootstrap() throws BootstrapException {
      try {
         final Properties merged = mergeProperties();

         LifecycleInjectorBuilder builder = LifecycleInjector.builder()
            .withBootstrapModule(new BootstrapModule() {
               @Override
               public void configure(BootstrapBinder binder) {
                  Names.bindProperties(binder, merged);
                  binder.bindConfigurationProvider().toInstance(new PropertiesConfigurationProvider(merged));
               }
            })
            .withAdditionalBootstrapModules(boostrapModules);

         if(merged.containsKey(PROP_MODULES)) {
            String [] classnames = StringUtils.split(merged.getProperty(PROP_MODULES));
            Set<Class<? extends Module>> modules = classnamesToClasses(Arrays.asList(classnames));
            modules.addAll(moduleClasses);
            builder.withAdditionalModuleClasses(modules);
         }
         else if(!moduleClasses.isEmpty()) {
            builder.withAdditionalModuleClasses(moduleClasses);
         }
         if(!modules.isEmpty()) {
            builder.withAdditionalModules(modules);
         }

         return builder.build().createInjector();
      } catch(Exception e) {
         throw new BootstrapException("Error bootstrapping the injection context", e);
      }
   }

   private Properties mergeProperties() throws IOException {
      Properties properties = new Properties();

      if(configs != null) {
         for(File f : configs) {
            Properties tmp = loadProperties(f);
            properties.putAll(tmp);
         }
      }
      
      if (constants != null) {
      	properties.putAll(constants);
      }

      properties.putAll(System.getProperties());
      for (Map.Entry<String,String> entry : System.getenv().entrySet()) {
         String key = entry.getKey();
         String value = entry.getValue();
         if (key == null) continue;

         String trimKey = key.trim();
         if (trimKey.isEmpty()) continue;
         //termsAndConditions_version - original property name
         properties.put(trimKey, value);

         String dottedKey = trimKey.replaceAll("_+", ".");
         //termsAndConditions.version - dotted key with the original case
         properties.put(dottedKey, value);
         //termsandconditions.version - dotted key with lowercase
         properties.put(dottedKey.toLowerCase(), value);         
         
      }
      return properties;
   }

   private Properties loadProperties(File file) throws IOException {
      try(FileInputStream fis = new FileInputStream(file)) {
         Properties properties = new Properties();
         properties.load(fis);
         return properties;
      }
   }
}

