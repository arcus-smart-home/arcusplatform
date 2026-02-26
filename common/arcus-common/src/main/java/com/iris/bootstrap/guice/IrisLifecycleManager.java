package com.iris.bootstrap.guice;

import com.iris.bootstrap.annotations.WarmUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages lifecycle of injected objects: invokes @PostConstruct, @WarmUp,
 * and @PreDestroy methods in the correct order.
 *
 * @PostConstruct is invoked immediately when an object is registered (during
 * Guice injection), matching Governator's behavior. @WarmUp is deferred to
 * the explicit start() call. @PreDestroy is invoked on close().
 *
 * Replacement for com.netflix.governator.lifecycle.LifecycleManager.
 */
@Singleton
public class IrisLifecycleManager implements Closeable {
   private static final Logger LOGGER = LoggerFactory.getLogger(IrisLifecycleManager.class);

   private final List<LifecycleEntry> entries = Collections.synchronizedList(new ArrayList<>());
   private volatile boolean started = false;

   /**
    * Registers an object for lifecycle management. Immediately invokes
    * @PostConstruct methods (matching Governator behavior where PostConstruct
    * runs during injection, before ServiceLocator is available).
    *
    * If the lifecycle manager is already started, also invokes @WarmUp immediately.
    */
   public void register(Object instance) {
      List<Method> postConstructMethods = findAnnotatedMethods(instance.getClass(), PostConstruct.class);
      List<Method> warmUpMethods = findAnnotatedMethods(instance.getClass(), WarmUp.class);
      List<Method> preDestroyMethods = findAnnotatedMethods(instance.getClass(), PreDestroy.class);

      if (postConstructMethods.isEmpty() && warmUpMethods.isEmpty() && preDestroyMethods.isEmpty()) {
         return;
      }

      // Invoke @PostConstruct immediately during injection
      invokeAll(instance, postConstructMethods, "@PostConstruct");

      // Store for later @WarmUp and @PreDestroy
      LifecycleEntry entry = new LifecycleEntry(instance, warmUpMethods, preDestroyMethods);
      entries.add(entry);

      if (started) {
         invokeAll(instance, warmUpMethods, "@WarmUp");
      }
   }

   /**
    * Starts the lifecycle manager: invokes all pending @WarmUp methods.
    * @PostConstruct methods have already been invoked during registration.
    * This method is idempotent.
    */
   public void start() throws Exception {
      if (started) {
         return;
      }
      LOGGER.info("Starting lifecycle manager...");
      started = true;

      for (LifecycleEntry entry : entries) {
         invokeAll(entry.instance, entry.warmUpMethods, "@WarmUp");
      }

      LOGGER.info("Lifecycle manager started ({} managed objects)", entries.size());
   }

   /**
    * Stops the lifecycle manager: invokes all @PreDestroy methods in reverse order.
    */
   @Override
   public void close() {
      LOGGER.info("Stopping lifecycle manager...");
      List<LifecycleEntry> reversed = new ArrayList<>(entries);
      Collections.reverse(reversed);

      for (LifecycleEntry entry : reversed) {
         invokeAll(entry.instance, entry.preDestroyMethods, "@PreDestroy");
      }

      entries.clear();
      started = false;
      LOGGER.info("Lifecycle manager stopped");
   }

   private void invokeAll(Object instance, List<Method> methods, String phase) {
      for (Method method : methods) {
         try {
            method.setAccessible(true);
            method.invoke(instance);
         } catch (Exception e) {
            LOGGER.error("Error invoking {} method {}.{}()",
               phase, instance.getClass().getSimpleName(), method.getName(), e);
         }
      }
   }

   private static List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotation) {
      List<Method> result = new ArrayList<>();
      Class<?> current = clazz;
      while (current != null && current != Object.class) {
         for (Method method : current.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation) && method.getParameterCount() == 0) {
               result.add(method);
            }
         }
         current = current.getSuperclass();
      }
      return result;
   }

   private static class LifecycleEntry {
      final Object instance;
      final List<Method> warmUpMethods;
      final List<Method> preDestroyMethods;

      LifecycleEntry(Object instance, List<Method> warmUpMethods, List<Method> preDestroyMethods) {
         this.instance = instance;
         this.warmUpMethods = warmUpMethods;
         this.preDestroyMethods = preDestroyMethods;
      }
   }
}
