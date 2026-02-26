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
package com.iris.bootstrap.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.iris.bootstrap.annotations.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves module dependencies declared via @Modules annotation and
 * @Inject constructor parameters. Recursively discovers and instantiates
 * all transitively included modules.
 *
 * Module-typed @Inject constructor parameters are resolved from already-instantiated
 * modules (dependency order). Non-Module parameters (e.g. ConfigurationProvider) are
 * resolved via an optional bootstrap Guice injector.
 */
public class ModuleResolver {
   private static final Logger LOGGER = LoggerFactory.getLogger(ModuleResolver.class);

   /**
    * Given a set of module classes, resolves all transitive @Modules dependencies
    * and returns instantiated Module objects in dependency order.
    */
   public static List<Module> resolve(Set<Class<? extends Module>> rootClasses) {
      return resolve(rootClasses, null);
   }

   /**
    * Given a set of module classes, resolves all transitive @Modules dependencies
    * and returns instantiated Module objects in dependency order.
    *
    * @param bootstrapInjector optional injector used to resolve non-Module @Inject
    *        constructor parameters (e.g. ConfigurationProvider, config classes)
    */
   public static List<Module> resolve(Set<Class<? extends Module>> rootClasses, Injector bootstrapInjector) {
      Set<Class<? extends Module>> allClasses = new LinkedHashSet<>();
      Set<Class<? extends Module>> visited = new HashSet<>();

      for (Class<? extends Module> clazz : rootClasses) {
         collectModules(clazz, allClasses, visited);
      }

      // Map of class -> instance for resolving Module-typed constructor parameters
      Map<Class<? extends Module>, Module> instanceMap = new HashMap<>();
      List<Module> modules = new ArrayList<>();

      for (Class<? extends Module> clazz : allClasses) {
         try {
            Module instance = instantiateModule(clazz, instanceMap, bootstrapInjector);
            instanceMap.put(clazz, instance);
            modules.add(instance);
         } catch (Exception e) {
            // Avoid calling e.getMessage() — Guice 4.0's ASM can't read Java 11+ class files,
            // which causes getMessage() to throw during error formatting
            LOGGER.error("Failed to instantiate module: {}", clazz.getName());
            throw new RuntimeException("Failed to instantiate module " + clazz.getName(), e);
         }
      }
      return modules;
   }

   /**
    * Instantiate a single module class. Strategy:
    * 1. Try no-arg constructor
    * 2. Find @Inject constructor, resolve parameters:
    *    - Module-typed params from instanceMap (already instantiated in dependency order)
    *    - Non-Module params from bootstrapInjector (if available)
    */
   private static Module instantiateModule(
         Class<? extends Module> clazz,
         Map<Class<? extends Module>, Module> instanceMap,
         Injector bootstrapInjector) throws Exception {

      Module instance;

      // Try no-arg constructor first
      Constructor<?> injectCtor = findInjectConstructor(clazz);
      if (injectCtor == null) {
         // Use no-arg constructor
         instance = clazz.newInstance();
      } else {
         // Resolve @Inject constructor parameters
         Class<?>[] paramTypes = injectCtor.getParameterTypes();
         Object[] args = new Object[paramTypes.length];

         for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (Module.class.isAssignableFrom(paramType)) {
               // Module-typed parameter: look up from already-instantiated modules
               args[i] = findModuleInstance(paramType, instanceMap);
            } else if (bootstrapInjector != null) {
               // Non-Module parameter: resolve from bootstrap injector
               try {
                  args[i] = bootstrapInjector.getInstance(paramType);
               } catch (Exception e) {
                  LOGGER.warn("Could not resolve parameter {} for module {}, using null",
                        paramType.getSimpleName(), clazz.getSimpleName());
                  args[i] = null;
               }
            }
         }

         injectCtor.setAccessible(true);
         instance = (Module) injectCtor.newInstance(args);
      }

      // Inject @Inject fields (e.g. @Inject @Named properties) via bootstrap injector
      if (bootstrapInjector != null) {
         try {
            bootstrapInjector.injectMembers(instance);
         } catch (Exception e) {
            LOGGER.debug("Could not inject members on module {}: {}",
                  clazz.getSimpleName(), e.getClass().getSimpleName());
         }
      }

      return instance;
   }

   private static Constructor<?> findInjectConstructor(Class<?> clazz) {
      for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
         if (ctor.isAnnotationPresent(Inject.class) || ctor.isAnnotationPresent(javax.inject.Inject.class)) {
            return ctor;
         }
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   private static Module findModuleInstance(Class<?> paramType, Map<Class<? extends Module>, Module> instanceMap) {
      // Direct match
      Module instance = instanceMap.get(paramType);
      if (instance != null) {
         return instance;
      }
      // Subtype match (parameter may be a superclass/interface of the actual module)
      for (Map.Entry<Class<? extends Module>, Module> entry : instanceMap.entrySet()) {
         if (paramType.isAssignableFrom(entry.getKey())) {
            return entry.getValue();
         }
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   private static void collectModules(Class<? extends Module> clazz,
                                       Set<Class<? extends Module>> collected,
                                       Set<Class<? extends Module>> visited) {
      if (!visited.add(clazz)) {
         return;
      }

      // Resolve @Modules annotation dependencies
      Modules annotation = clazz.getAnnotation(Modules.class);
      if (annotation != null) {
         for (Class<? extends Module> dep : annotation.include()) {
            collectModules(dep, collected, visited);
         }
         for (Class<? extends Module> dep : annotation.value()) {
            collectModules(dep, collected, visited);
         }
      }

      // Resolve @Inject constructor dependencies that are Module types.
      // In Governator, @Inject constructors on modules declared module dependencies —
      // any Module-typed parameter meant that module should also be installed.
      for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
         if (ctor.isAnnotationPresent(Inject.class) || ctor.isAnnotationPresent(javax.inject.Inject.class)) {
            for (Class<?> paramType : ctor.getParameterTypes()) {
               if (Module.class.isAssignableFrom(paramType) && paramType != Module.class) {
                  collectModules((Class<? extends Module>) paramType, collected, visited);
               }
            }
         }
      }

      collected.add(clazz);
   }
}
