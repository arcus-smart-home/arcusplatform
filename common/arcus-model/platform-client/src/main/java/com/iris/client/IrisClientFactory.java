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
/**
 * 
 */
package com.iris.client;

import java.util.concurrent.atomic.AtomicReference;

import com.iris.client.impl.netty.ClientConfig;
import com.iris.client.model.Model;
import com.iris.client.model.ModelCache;
import com.iris.client.model.Store;
import com.iris.client.service.Service;

/**
 *
 */
public abstract class IrisClientFactory {
   private static final AtomicReference<IrisClientFactory> reference = new AtomicReference<IrisClientFactory>();

   private static IrisClientFactory getFactory() {
      IrisClientFactory factory = reference.get();
      if(factory == null) {
         throw new IllegalStateException("The client has not been initialized, must call init() first");
      }
      return factory;
   }
   
   public static IrisClient getClient() {
      return getFactory().doGetClient();
   }
   
   public static ModelCache getModelCache() {
      return getFactory().doGetModelCache();
   }
   
   public static Store<Model> getStore(String type) {
      return getFactory().doGetStore(type);
   }
   
   public static <M extends Model> Store<M> getStore(Class<M> modelType) {
      return getFactory().doGetStore(modelType);
   }
   
   public static <S extends Service> S getService(Class<S> serviceType) {
      return getFactory().doGetService(serviceType);
   }
   
   public static ClientConfig getClientConfig() {
	   return getFactory().doGetClientConfig();
   }
   
   public static void init(IrisClientFactory factory) {
      IrisClientFactory old = reference.getAndSet(factory);
      if(old != null) {
         old.doDispose();
      }
      if(factory != null) {
         factory.doInitialize();
      }
   }
   
   public static void dispose() {
      IrisClientFactory factory = reference.getAndSet(null);
      if(factory != null) {
         factory.doDispose();
      }
   }

   protected abstract ClientConfig doGetClientConfig();
   
   protected void doInitialize() { }
   
   protected abstract IrisClient doGetClient();
   
   protected abstract ModelCache doGetModelCache();
   
   protected abstract Store<Model> doGetStore(String type) throws IllegalArgumentException;
   
   protected abstract <M extends Model> Store<M> doGetStore(Class<M> modelType) throws IllegalArgumentException;
   
   protected abstract <S extends Service> S doGetService(Class<S> serviceType) throws IllegalArgumentException;

   protected void doDispose() { }
}

