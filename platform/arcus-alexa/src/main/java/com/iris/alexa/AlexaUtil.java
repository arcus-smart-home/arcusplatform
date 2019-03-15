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
package com.iris.alexa;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;

import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.capability.definition.AttributeType;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaTemperature;

public enum AlexaUtil {
   ;

   private static final Pattern DOT_MATCHER = Pattern.compile("\\.");
   private static final Pattern UNDERSCORE_MATCHER = Pattern.compile("_");

   public static final Address ADDRESS_BRIDGE = Address.bridgeAddress("ALXA");
   public static final Address ADDRESS_SERVICE = Address.platformService(AlexaService.NAMESPACE);

   public static AlexaTemperature createAlexaTemp(double value) {
      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(Precision.round(value, 2));
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);
      return temp;
   }

   @SuppressWarnings("unchecked")
   public static <T> T getFromModelOrInternalError(Model m, String attr, AttributeType type) {
      Object value = m.getAttribute(attr);
      if(value == null) {
         throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
      }
      return (T) type.coerce(value);
   }

   public static String addressToEndpointId(String modelAddress) {
      return
         isSceneAddress(modelAddress) ?
         DOT_MATCHER.matcher(modelAddress).replaceAll("_") :
         modelAddress;
   }

   public static String endpointIdToAddress(String endpointId) {
      return
         isSceneAddress(endpointId) ?
         UNDERSCORE_MATCHER.matcher(endpointId).replaceAll("\\.") :
         endpointId;
   }

   public static boolean isSceneAddress(String address) {
      return StringUtils.contains(address, ":scene:");
   }
}

