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
package com.iris.ipcd.server;

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.session.SessionFactory;
import com.iris.bridge.server.session.SessionListener;
import com.iris.core.dao.cassandra.CassandraDeviceDAOModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.protocol.ipcd.IpcdDeviceDaoModule;
import com.iris.ipcd.AbstractIpcdServerModule;
import com.iris.ipcd.IpcdBridgeConfigModule;
import com.iris.ipcd.server.http.handlers.IpcdIndexPage;
import com.iris.ipcd.server.message.IpcdMessageHandler;
import com.iris.ipcd.server.session.IpcdSessionFactory;
import com.iris.ipcd.server.session.IpcdSessionListener;
import com.iris.ipcd.server.session.SessionFacade;
import com.iris.ipcd.session.PartitionedSession;
import com.iris.population.PlacePopulationCacheModule;
import com.netflix.governator.annotations.Modules;

@Modules(include = {
   IpcdBridgeConfigModule.class,
   IpcdDeviceDaoModule.class,
   CassandraDeviceDAOModule.class,
   CassandraPlaceDAOModule.class,
   CassandraResourceBundleDAOModule.class,
   PlacePopulationCacheModule.class
})
public class IpcdServerModule extends AbstractIpcdServerModule {

   @Inject
   public IpcdServerModule() {}

	@Override
   protected void bindMessageHandler() {
      bind(new TypeLiteral<DeviceMessageHandler<String>>(){}).to(IpcdMessageHandler.class);
   }

   @Override
   protected void bindSessionFactory() {
		bind(SessionFactory.class).to(IpcdSessionFactory.class);
   }

   @Override
   protected void bindSessionListener(Multibinder<SessionListener> slBindings) {
      slBindings.addBinding().to(IpcdSessionListener.class);
   }

   @Override
   protected void bindHttpHandlers(Multibinder<RequestHandler> rhBindings) {
      rhBindings.addBinding().to(IpcdIndexPage.class);
   }

   @Provides @Singleton
   public BridgeMetrics provideBridgeMetrics() {
      return new BridgeMetrics("ipcd");
   }

   @Override
   protected void bindSessionSupplier() {
      bind(new TypeLiteral<Supplier<Stream<PartitionedSession>>>(){}).to(SessionFacade.class);
   }
}

