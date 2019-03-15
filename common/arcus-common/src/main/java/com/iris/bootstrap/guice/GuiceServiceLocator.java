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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.Injectors;
import com.netflix.governator.lifecycle.LifecycleManager;

public final class GuiceServiceLocator implements ServiceLocator.Impl {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLocator.class);
   private final Injector injector;

   private GuiceServiceLocator(Injector injector) {
      this.injector = injector;
   }

   public static ServiceLocator.Impl create(Injector injector) {
      return new GuiceServiceLocator(injector);
   }

   @Override
   public void init() {
   	// TODO:  this really feels like it should be part of bootstrap, but if that is done
   	// then @WarmUp, @PostConstruct cannot use the ServiceLocator
   	try {
   	   LifecycleManager manager = getInstance(LifecycleManager.class);
         if(manager != null) {
            LOGGER.info("Starting up all modules...");
            try {
               manager.start();
               LOGGER.info("Started up");
            } catch(Exception e) {
               LOGGER.error("Failure to startup the injection context", e);
               throw new RuntimeException("Failure to startup the injection context", e);
            }
         }
      } catch (Exception ex) {
         // ignore
      }
   }

   @Override
   public void destroy() {
      try {
   	   LifecycleManager manager = getInstance(LifecycleManager.class);
   	   if(manager != null) {
   	      manager.close();
   	   }
   	} catch (Exception ex) {
   	   // ignore
   	}
   }

   @Override
   public <T> T getInstance(Class<T> clazz) {
   	try {
   		return injector.getInstance(clazz);
   	} catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + clazz + "]: " + e.getMessage(), e);
   	}
   }

   public <T> T getInstance(TypeLiteral<T> type) {
      try {
         return injector.getInstance(Key.get(type));
      } catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + type + "]: " + e.getMessage(), e);
      }
   }

   @Override
   public <T> T getNamedInstance(Class<T> clazz, String name) {
   	try {
   	   return injector.getInstance(Key.get(clazz, Names.named(name)));
   	} catch(Exception e) {
   		throw new IllegalArgumentException("Could not load type [" + clazz + "]: " + e.getMessage(), e);
   	}
   }

   @Override
	public <T> List<? extends T> getInstancesOf(Class<T> type) {
	   return Injectors.listInstancesOf(injector, type);
   }
}

