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
package com.iris.firmware;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.Hub;

@Singleton
public class HubFirmwareURLBuilder implements FirmwareURLBuilder<Hub> {

   @Inject(optional = true)
   @Named("firmware.download.scheme")
   private String firmwareDownloadScheme = "https";

   @Inject
   @Named("firmware.download.host")
   private String firmwareDownloadHost;
   
   private static final String EXTENSION = ".bin";

   @Override
   public String buildURL(Hub object, String firmwareTarget) {
      Preconditions.checkNotNull(object, "The hub must not be null.");
      Preconditions.checkNotNull(firmwareTarget, "The version must not be null.");
      return new StringBuilder()
         .append(firmwareDownloadScheme)
         .append("://")
         .append(firmwareDownloadHost)
         .append("/")
         .append(firmwareTarget)
         .append(EXTENSION)
         .toString();
   }
}

