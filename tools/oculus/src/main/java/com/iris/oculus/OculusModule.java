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
package com.iris.oculus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;
import javax.swing.JMenu;

import com.google.inject.Provides;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.StaticDefinitionRegistry;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.event.DefaultExecutor;
import com.iris.client.impl.netty.NettyIrisClientFactory;
import com.iris.oculus.menu.DevicesMenu;
import com.iris.oculus.menu.HubsMenu;
import com.iris.oculus.menu.PlaceMenu;
import com.iris.oculus.menu.ServicesMenu;
import com.iris.oculus.menu.SessionMenu;
import com.iris.oculus.menu.WindowsMenu;
import com.iris.oculus.modules.account.AccountController;
import com.iris.oculus.modules.account.AccountSection;
import com.iris.oculus.modules.behaviors.BehaviorSection;
import com.iris.oculus.modules.capability.CapabilityController;
import com.iris.oculus.modules.dashboard.DashboardController;
import com.iris.oculus.modules.dashboard.DashboardSection;
import com.iris.oculus.modules.device.DeviceController;
import com.iris.oculus.modules.device.DeviceSection;
import com.iris.oculus.modules.device.mockaction.MockActionsNexus;
import com.iris.oculus.modules.hub.HubController;
import com.iris.oculus.modules.hub.HubSection;
import com.iris.oculus.modules.incident.IncidentController;
import com.iris.oculus.modules.incident.IncidentSection;
import com.iris.oculus.modules.pairing.PairingDeviceController;
import com.iris.oculus.modules.pairing.PairingDeviceSection;
import com.iris.oculus.modules.person.PersonController;
import com.iris.oculus.modules.person.PersonSection;
import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.modules.place.PlaceSection;
import com.iris.oculus.modules.product.ProductController;
import com.iris.oculus.modules.product.ProductSection;
import com.iris.oculus.modules.rule.RuleController;
import com.iris.oculus.modules.rule.RuleSection;
import com.iris.oculus.modules.scene.SceneSection;
import com.iris.oculus.modules.scheduler.SchedulerSection;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.modules.status.StatusController;
import com.iris.oculus.modules.subsystem.SubsystemController;
import com.iris.oculus.modules.subsystem.SubsystemSection;
import com.iris.oculus.modules.video.VideoSection;
import com.iris.oculus.util.ComponentWrapper;
import com.iris.oculus.util.SwingExecutorService;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

/**
 *
 */
public class OculusModule extends AbstractIrisModule {
   public OculusModule() {
   }

   @Provides
   public ViewModel<CapabilityDefinition> capabilities() {
      return new SimpleViewModel<>(StaticDefinitionRegistry.getInstance().getCapabilities());
   }

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      DefaultExecutor.setDefaultExecutor(SwingExecutorService.getInstance());

      // TODO load these from the server
      bind(DefinitionRegistry.class)
         .toInstance(StaticDefinitionRegistry.getInstance());

      IrisClientFactory.init(new NettyIrisClientFactory());
      bind(IrisClient.class)
         .toInstance(IrisClientFactory.getClient());

      bind(SessionController.class)
         .asEagerSingleton();
      bind(StatusController.class)
         .asEagerSingleton();
      bind(DashboardController.class)
         .asEagerSingleton();
      bind(DeviceController.class)
         .asEagerSingleton();
      bind(HubController.class)
         .asEagerSingleton();
      bind(ProductController.class)
         .asEagerSingleton();
      bind(RuleController.class)
         .asEagerSingleton();
      bind(PersonController.class)
      	.asEagerSingleton();
      bind(PlaceController.class)
      	.asEagerSingleton();
      bind(AccountController.class)
         .asEagerSingleton();
      bind(SubsystemController.class)
         .asEagerSingleton();
      bind(IncidentController.class)
         .asEagerSingleton();
      bind(PairingDeviceController.class)
         .asEagerSingleton();
      bind(CapabilityController.class)
      	.asEagerSingleton();

      bind(DashboardSection.class);
      bind(HubSection.class);
      bind(DeviceSection.class);
      bind(SubsystemSection.class);
      bind(ProductSection.class);
      bind(RuleSection.class);
      bind(SceneSection.class);
      bind(PersonSection.class);
      bind(VideoSection.class);
      bind(PlaceSection.class);
      bind(AccountSection.class);
      bind(SchedulerSection.class);
      bind(BehaviorSection.class);
      bind(IncidentSection.class);
      bind(PairingDeviceSection.class);

      bindListToInstancesOf(OculusSection.class);
   }

   @Provides
   public List<ComponentWrapper<JMenu>> menus(
         SessionMenu session,
         PlaceMenu places,
         ServicesMenu services,
         HubsMenu hubs,
         DevicesMenu devices,
         WindowsMenu windows
   ) {
      return Arrays.asList(session, hubs, devices, places, services, windows);
   }

   @Provides
   @Singleton
   public MockActionsNexus provideMockActionsNexus() {
   	Resource resource = Resources.getResource("classpath:/mock.json");
   	return new MockActionsNexus(resource);
   }
}

