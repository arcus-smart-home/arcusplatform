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
package com.iris.platform.services.person;

import java.util.ResourceBundle;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformService;
import com.iris.i18n.DBResourceBundleControl;
import com.iris.i18n.I18NBundle;
import com.iris.messages.model.Person;
import com.iris.platform.services.person.handlers.AcceptInvitationHandler;
import com.iris.platform.services.person.handlers.AcceptPolicyHandler;
import com.iris.platform.services.person.handlers.AddMobileDeviceHandler;
import com.iris.platform.services.person.handlers.DeleteLoginHandler;
import com.iris.platform.services.person.handlers.GetSecurityAnswersHandler;
import com.iris.platform.services.person.handlers.ListAvailablePlacesHandler;
import com.iris.platform.services.person.handlers.ListHistoryEntriesHandler;
import com.iris.platform.services.person.handlers.ListMobileDevicesHandler;
import com.iris.platform.services.person.handlers.PendingInvitationsHandler;
import com.iris.platform.services.person.handlers.PersonDeleteHandler;
import com.iris.platform.services.person.handlers.PersonGetAttributesHandler;
import com.iris.platform.services.person.handlers.PersonRemoveFromPlaceHandler;
import com.iris.platform.services.person.handlers.PersonSetAttributesHandler;
import com.iris.platform.services.person.handlers.PromoteToAccountHandler;
import com.iris.platform.services.person.handlers.RejectInvitationHandler;
import com.iris.platform.services.person.handlers.RejectPolicyHandler;
import com.iris.platform.services.person.handlers.RemoveMobileDeviceHandler;
import com.iris.platform.services.person.handlers.SetSecurityAnswersHandler;

public class PersonServiceModule extends AbstractIrisModule {

   @Provides @Singleton @Named(SetSecurityAnswersHandler.NAME_RESOURCE_BUNDLE)
   public ResourceBundle securityResources(DBResourceBundleControl control) {
      return ResourceBundle.getBundle(I18NBundle.SECURITY_QUESTION.getBundleName(), control);
   }

   @Override
   protected void configure() {
      Multibinder<ContextualRequestMessageHandler<Person>> handlerBinder = bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<Person>>() {});
      handlerBinder.addBinding().to(PersonGetAttributesHandler.class);
      handlerBinder.addBinding().to(PersonSetAttributesHandler.class);
      handlerBinder.addBinding().to(SetSecurityAnswersHandler.class);
      handlerBinder.addBinding().to(GetSecurityAnswersHandler.class);
      handlerBinder.addBinding().to(AddMobileDeviceHandler.class);
      handlerBinder.addBinding().to(RemoveMobileDeviceHandler.class);
      handlerBinder.addBinding().to(ListMobileDevicesHandler.class);
      handlerBinder.addBinding().to(PersonDeleteHandler.class);
      handlerBinder.addBinding().to(ListHistoryEntriesHandler.class);
      handlerBinder.addBinding().to(AcceptInvitationHandler.class);
      handlerBinder.addBinding().to(RejectInvitationHandler.class);
      handlerBinder.addBinding().to(PendingInvitationsHandler.class);
      handlerBinder.addBinding().to(PersonRemoveFromPlaceHandler.class);
      handlerBinder.addBinding().to(PromoteToAccountHandler.class);
      handlerBinder.addBinding().to(DeleteLoginHandler.class);
      handlerBinder.addBinding().to(ListAvailablePlacesHandler.class);
      handlerBinder.addBinding().to(AcceptPolicyHandler.class);
      handlerBinder.addBinding().to(RejectPolicyHandler.class);
      
      bindSetOf(PlatformService.class).addBinding().to(PersonService.class);
   }

}

