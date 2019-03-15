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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO:  i know this is a convenient mechanism, but it also feels a bit like an anti-pattern with di
public final class ServiceLocator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLocator.class);

   private volatile static Impl impl;

   private ServiceLocator() {
   }

   public synchronized static void init(Impl impl) {
      if(ServiceLocator.impl != null) {
         LOGGER.warn("Service locator already initialized, destroying and re-initializing");
         destroy();
      }

   	ServiceLocator.impl = impl;
   	if (ServiceLocator.impl != null) {
   	   ServiceLocator.impl.init();
   	}
   }

   public synchronized static void destroy() {
   	// swap operation, make sure that the injector can't be accessed before destroying the modules
   	Impl impl = ServiceLocator.impl;
   	if(impl == null) {
   		// already shutdown
   		LOGGER.debug("Already shutdown, ignoring destroy request");
   		return;
   	}

   	LOGGER.info("Received shutdown signal, shutting down modules");
   	impl.destroy();

   	ServiceLocator.impl = null;
   	LOGGER.info("Shutdown");
   }

   // this is playing a bit fast and loose with threading rules, but since
   // injector is volatile we should be ok
   public static Impl get() {
   	Impl impl = ServiceLocator.impl;
   	if(impl == null) {
   		throw new IllegalStateException("No service locator implementation exists, please ensure the system has been bootstrapped.");
   	}

   	return impl;
   }

   /**
    * Retrieves the instance bound to {@code clazz}.
    * @param clazz
    * @return
    */
   public static <T> T getInstance(Class<T> clazz) {
   	try {
   		return get().getInstance(clazz);
   	} catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + clazz + "]: " + e.getMessage(), e);
   	}
   }

   /*
   public static <T> T getInstance(TypeLiteral<T> type) {
      try {
         return get().getInstance(type);
      } catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + type + "]: " + e.getMessage(), e);
      }
   }
   */

   /**
    * Retrieves the instance bound to {@code clazz} with
    * the named annotation of {@code name}.
    * @param clazz
    * @param name
    * @return
    */
   public static <T> T getNamedInstance(Class<T> clazz, String name) {
   	try {
   	   return get().getNamedInstance(clazz, name);
   	} catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + clazz + "]: " + e.getMessage(), e);
   	}
   }

   /**
    * Retrieves any instances bound to a key that implements / extends {@code type}.
    * @param type
    * @return
    */
	public static <T> List<? extends T> getInstancesOf(Class<T> type) {
	   return get().getInstancesOf(type);
   }

   public interface Impl {
      void init();
      void destroy();

      <T> T getInstance(Class<T> clazz);
      //<T> T getInstance(TypeLiteral<T> type);
      <T> T getNamedInstance(Class<T> clazz, String name);
	   <T> List<? extends T> getInstancesOf(Class<T> type);
   }
}

