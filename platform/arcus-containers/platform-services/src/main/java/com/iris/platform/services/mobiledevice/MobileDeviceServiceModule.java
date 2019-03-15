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
package com.iris.platform.services.mobiledevice;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformService;
import com.iris.messages.model.MobileDevice;
import com.iris.platform.services.mobiledevice.handlers.MobileDeviceGetAttributesHandler;
import com.iris.platform.services.mobiledevice.handlers.MobileDeviceSetAttributesHandler;

public class MobileDeviceServiceModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      Multibinder<ContextualRequestMessageHandler<MobileDevice>> handlerBinder = bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<MobileDevice>>() {});
      handlerBinder.addBinding().to(MobileDeviceGetAttributesHandler.class);
      handlerBinder.addBinding().to(MobileDeviceSetAttributesHandler.class);

      bindSetOf(PlatformService.class).addBinding().to(MobileDeviceService.class);
   }

}

