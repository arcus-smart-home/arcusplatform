package com.iris.bootstrap.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.iris.bootstrap.annotations.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves module dependencies declared via @Modules annotation and
 * @Inject constructor parameters. Recursively discovers and instantiates
 * all transitively included modules.
 */
public class ModuleResolver {
   private static final Logger LOGGER = LoggerFactory.getLogger(ModuleResolver.class);

   /**
    * Given a set of module classes, resolves all transitive @Modules dependencies
    * and returns instantiated Module objects in dependency order.
    * Uses no-arg constructors only.
    */
   public static List<Module> resolve(Set<Class<? extends Module>> rootClasses) {
      return resolve(rootClasses, null);
   }

   /**
    * Given a set of module classes, resolves all transitive @Modules dependencies
    * and returns instantiated Module objects in dependency order.
    * Uses the provided bootstrap injector to resolve @Inject constructor dependencies.
    */
   public static List<Module> resolve(Set<Class<? extends Module>> rootClasses, Injector bootstrapInjector) {
      Set<Class<? extends Module>> allClasses = new LinkedHashSet<>();
      Set<Class<? extends Module>> visited = new HashSet<>();

      for (Class<? extends Module> clazz : rootClasses) {
         collectModules(clazz, allClasses, visited);
      }

      List<Module> modules = new ArrayList<>();
      for (Class<? extends Module> clazz : allClasses) {
         try {
            if (bootstrapInjector != null) {
               modules.add(bootstrapInjector.getInstance(clazz));
            } else {
               modules.add(clazz.newInstance());
            }
         } catch (Exception e) {
            LOGGER.error("Failed to instantiate module {}", clazz.getName(), e);
            throw new RuntimeException("Failed to instantiate module " + clazz.getName(), e);
         }
      }
      return modules;
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
