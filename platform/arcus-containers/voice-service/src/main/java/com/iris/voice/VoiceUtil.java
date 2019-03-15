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
package com.iris.voice;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

public enum VoiceUtil {
   ;

   @Nullable
   public static ProductCatalogEntry getProduct(ProductCatalogManager prodCat, Model m) {
      if(m == null) {
         return null;
      }

      if(m.supports(SceneCapability.NAMESPACE)) {
         return null;
      }
      if(StringUtils.isBlank(DeviceModel.getProductId(m))) {
         return null;
      }

      return prodCat.getCatalog(Population.NAME_GENERAL).getProductById(DeviceModel.getProductId(m));
   }
}

