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
package com.iris.platform.services.place;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.model.Place;
import com.iris.platform.location.TimezonesModule;
import com.iris.platform.services.handlers.AddTagsHandler;
import com.iris.platform.services.handlers.RemoveTagsHandler;
import com.iris.platform.services.place.handlers.AddPersonHandler;
import com.iris.platform.services.place.handlers.CancelInvitationHandler;
import com.iris.platform.services.place.handlers.CreateInvitationHandler;
import com.iris.platform.services.place.handlers.GetHubHandler;
import com.iris.platform.services.place.handlers.ListDashboardEntriesHandler;
import com.iris.platform.services.place.handlers.ListDevicesHandler;
import com.iris.platform.services.place.handlers.ListHistoryEntriesHandler;
import com.iris.platform.services.place.handlers.ListPersonsHandler;
import com.iris.platform.services.place.handlers.ListPersonsWithAccessHandler;
import com.iris.platform.services.place.handlers.PendingInvitationsHandler;
import com.iris.platform.services.place.handlers.PlaceDeleteHandler;
import com.iris.platform.services.place.handlers.PlaceGetAttributesHandler;
import com.iris.platform.services.place.handlers.PlaceSetAttributesHandler;
import com.iris.platform.services.place.handlers.PlaceUpdateAddressHandler;
import com.iris.platform.services.place.handlers.RegisterHubHandler;
import com.iris.platform.services.place.handlers.RegisterHubV2Handler;
import com.iris.platform.services.place.handlers.SendInvitationHandler;
import com.iris.platform.services.place.handlers.StartAddingDevicesHandler;
import com.iris.platform.services.place.handlers.StopAddingDevicesHandler;
import com.iris.platform.services.place.handlers.ValidateAddressHandler;
import com.netflix.governator.annotations.Modules;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDaoModule;
import com.iris.platform.pairing.PairingDeviceDaoModule;

@Modules(include={
		TimezonesModule.class,
		ManufactureKittingDaoModule.class,
		PairingDeviceDaoModule.class
})
public class PlaceServiceModule extends AbstractIrisModule {	
		
   @Inject
   public PlaceServiceModule() {
   }

   @Override
   protected void configure() {
      Multibinder<ContextualRequestMessageHandler<Place>> handlerBinder = bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<Place>>() {});
      handlerBinder.addBinding().to(PlaceGetAttributesHandler.class);
      handlerBinder.addBinding().to(PlaceSetAttributesHandler.class);
      handlerBinder.addBinding().to(ListDevicesHandler.class);
      handlerBinder.addBinding().to(GetHubHandler.class);
      handlerBinder.addBinding().to(RegisterHubHandler.class);
      handlerBinder.addBinding().to(StartAddingDevicesHandler.class);
      handlerBinder.addBinding().to(StopAddingDevicesHandler.class);
      handlerBinder.addBinding().to(ListPersonsHandler.class);
      handlerBinder.addBinding().to(AddPersonHandler.class);
      handlerBinder.addBinding().to(PlaceDeleteHandler.class);
      handlerBinder.addBinding().to(ListDashboardEntriesHandler.class);
      handlerBinder.addBinding().to(ListHistoryEntriesHandler.class);
      handlerBinder.addBinding().to(new TypeLiteral<AddTagsHandler<Place>>() {});
      handlerBinder.addBinding().to(new TypeLiteral<RemoveTagsHandler<Place>>() {});
      handlerBinder.addBinding().to(CreateInvitationHandler.class);
      handlerBinder.addBinding().to(SendInvitationHandler.class);
      handlerBinder.addBinding().to(PendingInvitationsHandler.class);
      handlerBinder.addBinding().to(CancelInvitationHandler.class);
      handlerBinder.addBinding().to(ListPersonsWithAccessHandler.class);
      handlerBinder.addBinding().to(PlaceUpdateAddressHandler.class);
      handlerBinder.addBinding().to(ValidateAddressHandler.class);
      handlerBinder.addBinding().to(RegisterHubV2Handler.class);

      Multibinder<ContextualEventMessageHandler<Place>> eventHandler = bindSetOf(new TypeLiteral<ContextualEventMessageHandler<Place>>() {});

      bindSetOf(PlatformService.class).addBinding().to(PlaceService.class);
   }

   @Provides
   @Singleton
   public AddTagsHandler<Place> placeAddTags(PlaceDAO placeDao, PlatformMessageBus platformBus) {
      return new AddTagsHandler<Place>(placeDao, platformBus);
   }

   @Provides
   @Singleton
   public RemoveTagsHandler<Place> placeRemoveTags(PlaceDAO placeDao, PlatformMessageBus platformBus) {
      return new RemoveTagsHandler<Place>(placeDao, platformBus);
   }

}

