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
package com.iris.platform.services.place.handlers;

import java.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Place;
import com.iris.platform.services.AbstractGetAttributesPlatformMessageHandler;

@Singleton
public class PlaceGetAttributesHandler extends AbstractGetAttributesPlatformMessageHandler<Place> {

   @Inject
   public PlaceGetAttributesHandler(BeanAttributesTransformer<Place> placeTransformer) {
      super(placeTransformer);
   }

}

