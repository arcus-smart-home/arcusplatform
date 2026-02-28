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
package com.iris.agent.boot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.iris.agent.config.ConfigService;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.bootstrap.config.AbstractObjectConfigurationProvider;
import com.iris.bootstrap.config.ConfigurationKey;
import com.iris.bootstrap.config.ConfigurationProvider;
import com.iris.bootstrap.config.Property;
import com.iris.bootstrap.guice.IrisLifecycleManager;
import com.iris.bootstrap.guice.LifecycleModule;
import com.iris.bootstrap.guice.ModuleResolver;

public class Bootstrap {
   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private final Set<Module> bootstrapModules = new HashSet<>();
      private final Set<Class<? extends Module>> moduleClasses = new HashSet<>();
      private final Set<Module> modules = new HashSet<>();

      public Builder withBootstrapModules(Collection<? extends Module> bootstrapModules) {
         Preconditions.checkArgument(bootstrapModules != null, "bootstrap modules cannot be null");
         this.bootstrapModules.addAll(bootstrapModules);
         return this;
      }

      public Builder withModuleClasses(Collection<Class<? extends Module>> classes) {
         Preconditions.checkArgument(classes != null, "module classes cannot be null");
         moduleClasses.addAll(classes);
         return this;
      }

      public Builder withModules(Collection<Module> modules) {
         Preconditions.checkArgument(modules != null, "modules cannot be null");
         this.modules.addAll(modules);
         return this;
      }

      public Bootstrap build() {
         Bootstrap bootstrap = new Bootstrap();
         bootstrap.bootstrapModules.addAll(bootstrapModules);
         bootstrap.moduleClasses.addAll(moduleClasses);
         bootstrap.modules.addAll(modules);
         return bootstrap;
      }
   }

   private final Set<Module> bootstrapModules = new HashSet<>();
   private final Set<Class<? extends Module>> moduleClasses = new HashSet<>();
   private final Set<Module> modules = new HashSet<>();

   private Bootstrap() {
   }

   public Injector bootstrap() {
      try {
         List<Module> allModules = new ArrayList<>();

         // Lifecycle module must come first
         allModules.add(new LifecycleModule());

         // Configuration bootstrap module
         allModules.add(binder -> {
            binder.bind(ConfigurationProvider.class).to(AgentConfigurationProvider.class);
         });

         // Add any explicit bootstrap modules
         allModules.addAll(bootstrapModules);

         // Resolve @Modules annotations and instantiate module classes
         allModules.addAll(ModuleResolver.resolve(moduleClasses));

         // Add any explicit module instances
         if(!modules.isEmpty()) {
            allModules.addAll(modules);
         }

         Injector result = Guice.createInjector(Stage.PRODUCTION, allModules);

         // Start lifecycle manager: @PostConstruct already invoked during injection,
         // this triggers the @WarmUp phase
         try {
            result.getInstance(IrisLifecycleManager.class).start();
         } catch (Exception e) {
            // ignore if not bound
         }

         LifeCycleService.setState(LifeCycle.STARTING_UP, LifeCycle.STARTED);
         return result;
      } catch(Exception ex) {
         throw new BootException("error bootstrapping", ex);
      }
   }

   private static final class AgentConfigurationProvider extends AbstractObjectConfigurationProvider {
      private static final Map<String,String> VARS = Collections.emptyMap();

      @Override
      public Property<Boolean> getBooleanProperty(@Nullable ConfigurationKey key, @Nullable Boolean defaultValue) {
         if (key == null) throw new NullPointerException();

         String cnf = key.getKey(VARS);
         return new ConfigProperty<Boolean>(cnf, defaultValue, Boolean.class);
      }

      @Override
      public Property<Integer> getIntegerProperty(@Nullable ConfigurationKey key, @Nullable Integer defaultValue) {
         if (key == null) throw new NullPointerException();

         String cnf = key.getKey(VARS);
         return new ConfigProperty<Integer>(cnf, defaultValue, Integer.class);
      }

      @Override
      public Property<Long> getLongProperty(@Nullable ConfigurationKey key, @Nullable Long defaultValue) {
         if (key == null) throw new NullPointerException();

         String cnf = key.getKey(VARS);
         return new ConfigProperty<Long>(cnf, defaultValue, Long.class);
      }

      @Override
      public Property<Double> getDoubleProperty(@Nullable ConfigurationKey key, @Nullable Double defaultValue) {
         if (key == null) throw new NullPointerException();

         String cnf = key.getKey(VARS);
         return new ConfigProperty<Double>(cnf, defaultValue, Double.class);
      }

      @Override
      public Property<String> getStringProperty(@Nullable ConfigurationKey key, @Nullable String defaultValue) {
         if (key == null) throw new NullPointerException();

         String cnf = key.getKey(VARS);
         return new ConfigProperty<String>(cnf, defaultValue, String.class);
      }
   }

   private static final class ConfigProperty<T> extends Property<T> {
      private final String key;
      private final Class<T> type;

      @Nullable
      private final T defaultValue;

      public ConfigProperty(String key, @Nullable T defaultValue, Class<T> type) {
         this.defaultValue = defaultValue;
         this.key = key;
         this.type = type;
      }

      @Override
      public T get() {
         return ConfigService.get(key, type, defaultValue);
      }
   }
}
