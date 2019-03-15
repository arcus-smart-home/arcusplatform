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
package com.iris.platform.location;

import static com.iris.resource.Resources.getResource;

import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.messages.model.Place;
import com.iris.resource.Resource;
import com.iris.resource.manager.DefaultSingleFileResourceManager;

@Singleton
public class ResourceLocationService implements LocationService
{
   @Inject(optional = true) @Named("zipcodes.resource.uri")
   private String zipCodesResourceUri = "classpath:/zipcodes.data";

   private ZipCodesResourceManager zipCodesResourceManager;

   @PostConstruct
   public void init()
   {
      zipCodesResourceManager = new ZipCodesResourceManager(getResource(zipCodesResourceUri));
   }

   @Override
   public Optional<PlaceLocation> getForPlace(Place place)
   {
      return getForZipCode(place.getZipCode());
   }

   @Override
   public Optional<PlaceLocation> getForZipCode(String zipCode)
   {
      PlaceLocation placeLocation = zipCodesResourceManager.getParsedData().get(zipCode);

      return Optional.ofNullable(placeLocation);
   }

   private class ZipCodesResourceManager extends DefaultSingleFileResourceManager<Map<String, PlaceLocation>>
   {
      public ZipCodesResourceManager(Resource managedResource)
      {
         super(managedResource, new ZipCodesCsvParser());
      }
   }
}

