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
package com.iris.firmware.hub;

import java.net.URISyntaxException;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.firmware.FirmwareManager;
import com.iris.firmware.FirmwareURLBuilder;
import com.iris.firmware.FirmwareUpdateResolver;
import com.iris.firmware.HubFirmwareURLBuilder;
import com.iris.firmware.XMLFirmwareResolver;
import com.iris.messages.model.Hub;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

public class HubFirmwareModule extends AbstractIrisModule {
   @Inject(optional = true)
   @Named(value = "firmware.update.path")
   private String firmwareUpdatePath = "conf/firmware.xml";

   @Override
   protected void configure() {
      bind(FirmwareUpdateResolver.class).to(XMLFirmwareResolver.class);
      bind(new Key<FirmwareURLBuilder<Hub>>(){}).to(HubFirmwareURLBuilder.class);
   }

   @Provides @Singleton
   public FirmwareManager provideFirmwareManager() throws IllegalArgumentException, URISyntaxException {
      Resource firmwareUpdates = Resources.getResource(firmwareUpdatePath);
      return new FirmwareManager(firmwareUpdates);
   }

}

