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
package com.iris.voice.alexa.handlers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.FanModel;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaCapability;
import com.iris.messages.type.AlexaEndpoint;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceUtil;
import com.iris.voice.alexa.AlexaConfig;
import com.iris.voice.alexa.AlexaMetrics;
import com.iris.voice.alexa.AlexaPredicates;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceDAO;

@Singleton
public class DiscoverHandler {

   private static final String IRIS = "Iris by Lowe's";
   private static final String UNKNOWN = "Unknown";
   private static final String DEV_MANUFACTURER = "Iris";

   private static final Map<String, Set<String>> DISPLAY_CATEGORIES = ImmutableMap.<String, Set<String>>builder()
      .put("Thermostat", ImmutableSet.of("THERMOSTAT"))
      .put("Light", ImmutableSet.of("LIGHT"))
      .put("Dimmer", ImmutableSet.of("LIGHT"))
      .put("Switch", ImmutableSet.of("SWITCH"))
      .put("Fan Control", ImmutableSet.of("SWITCH"))
      .put("Lock", ImmutableSet.of("SMARTLOCK"))
      .put("Halo", ImmutableSet.of("LIGHT"))
      .put("Scene", ImmutableSet.of("SCENE_TRIGGER"))
      .build();

   private final VoiceDAO voiceDao;
   private final ProductCatalogManager prodCat;
   private final Map<String, AlexaCapability> caps;

   @Inject
   public DiscoverHandler(VoiceDAO voiceDao, ProductCatalogManager prodCat, AlexaConfig config) {
      this.voiceDao = voiceDao;
      this.prodCat = prodCat;
      caps = ImmutableMap.<String, AlexaCapability>builder()
         .put(DimmerCapability.NAMESPACE, AlexaInterfaces.BrightnessController.createCapability(config.isProactiveEnabled()))
         .put(SwitchCapability.NAMESPACE, AlexaInterfaces.PowerController.createCapability(config.isProactiveEnabled()))
         .put(FanCapability.NAMESPACE, AlexaInterfaces.PercentageController.createCapability(config.isProactiveEnabled()))
         .put(ThermostatCapability.NAMESPACE, AlexaInterfaces.ThermostatController.createCapability(config.isProactiveEnabled()))
         .put(TemperatureCapability.NAMESPACE, AlexaInterfaces.TemperatureSensor.createCapability(config.isProactiveEnabled()))
         .put(ColorCapability.NAMESPACE, AlexaInterfaces.ColorController.createCapability(config.isProactiveEnabled()))
         .put(ColorTemperatureCapability.NAMESPACE, AlexaInterfaces.ColorTemperatureController.createCapability(config.isProactiveEnabled()))
         .put(DoorLockCapability.NAMESPACE, AlexaInterfaces.LockController.createCapability(config.isProactiveEnabled()))
         .put(DeviceConnectionCapability.NAMESPACE, AlexaInterfaces.EndpointHealth.createCapability(config.isProactiveEnabled()))
         .put(SceneCapability.NAMESPACE, AlexaInterfaces.SceneController.CAPABILITY)
         .build();
   }

   @Request(value = AlexaService.DiscoverRequest.NAME, service = true)
   public MessageBody handleDiscover(VoiceContext context) {
      HandlerUtil.markAssistantIfNecessary(context, voiceDao);
      AlexaMetrics.incCommand(AlexaInterfaces.Discovery.REQUEST_DISCOVER);

      Stream<AlexaEndpoint> devices = context.streamSupported(
         model -> AlexaPredicates.supportedDevice(model, VoiceUtil.getProduct(prodCat, model)),
         model -> Optional.ofNullable(modelToEndpoint(model, VoiceUtil.getProduct(prodCat, model)))
      );

      Stream<AlexaEndpoint> scenes = context.streamSupported(
         AlexaPredicates::supportedScene,
         model -> Optional.ofNullable(modelToEndpoint(model, VoiceUtil.getProduct(prodCat, model)))
      )
      .sorted(Comparator.comparing(AlexaEndpoint::getEndpointId));

      return AlexaService.DiscoverResponse.builder()
         .withEndpoints(
            Stream.concat(devices, scenes).map(AlexaEndpoint::toMap).collect(Collectors.toList())
         )
         .build();
   }

   private AlexaEndpoint modelToEndpoint(Model m, @Nullable ProductCatalogEntry productModel) {
      AlexaEndpoint endpoint = new AlexaEndpoint();
      endpoint.setEndpointId(AlexaUtil.addressToEndpointId(m.getAddress().getRepresentation()));
      endpoint.setFriendlyName(friendlyName(m));
      endpoint.setDescription(description(m, productModel));
      endpoint.setManufacturerName(manufacturerName(m, productModel));
      endpoint.setDisplayCategories(displayCategories(m));
      endpoint.setCapabilities(capabilities(m).map(AlexaCapability::toMap).collect(Collectors.toList()));
      endpoint.setOnline(m.supports(SceneCapability.NAMESPACE) || DeviceConnectionModel.isStateONLINE(m));
      endpoint.setModel(modelName(m));
      endpoint.setCookie(cookie(m));
      return endpoint;
   }

   private static String friendlyName(Model m) {
      return m.supports(SceneCapability.NAMESPACE) ? SceneModel.getName(m) : DeviceModel.getName(m);
   }

   private static String description(Model m, ProductCatalogEntry productModel) {
      if(m.supports(SceneCapability.NAMESPACE)) {
         return "Scene connected via " + IRIS;
      }
      String friendlyDesc = DeviceModel.getDevtypehint(m);
      if(productModel != null) {
         friendlyDesc = productModel.getVendor() + ' ' + productModel.getName();
      }
      return friendlyDesc + " connected via " + IRIS;
   }

   private static String manufacturerName(Model m, ProductCatalogEntry productModel) {
      return m.supports(SceneCapability.NAMESPACE) ? IRIS : DEV_MANUFACTURER;
   }

   private static Map<String, String> cookie(Model m) {
      Map<String,String> details = new HashMap<>();
      if(m.supports(DeviceCapability.NAMESPACE)) {
         details.put(DeviceCapability.ATTR_DEVTYPEHINT, DeviceModel.getDevtypehint(m, "Undefined"));
         if(m.supports(FanCapability.NAMESPACE)) {
            details.put(FanCapability.ATTR_MAXSPEED, String.valueOf(FanModel.getMaxSpeed(m, 3)));
         }
      }
      return details;
   }

   private static String modelName(Model m) {
      if(m.supports(SceneCapability.NAMESPACE)) {
         return SceneCapability.NAME;
      }
      return DeviceModel.getModel(m, UNKNOWN);
   }

   private static Set<String> displayCategories(Model m) {
      Set<String> displayCategories =
         m.supports(SceneCapability.NAMESPACE) ?
         DISPLAY_CATEGORIES.get("Scene") :
         DISPLAY_CATEGORIES.get(DeviceModel.getDevtypehint(m));

      return displayCategories == null ? displayCategoryFromCaps(m) : displayCategories;
   }

   private static Set<String> displayCategoryFromCaps(Model m) {
      String devtype = "Switch";
      for(String cap : m.getCapabilities()) {
         switch(cap) {
            case DimmerCapability.NAMESPACE:
            case ColorCapability.NAMESPACE:
            case ColorTemperatureCapability.NAMESPACE:
            case LightCapability.NAMESPACE:
               devtype = "Light";
               break;
            case DoorLockCapability.NAMESPACE:
               devtype = "Lock";
               break;
            case ThermostatCapability.NAMESPACE:
               devtype = "Thermostat";
               break;
            default:
               break;
         }
      }
      return DISPLAY_CATEGORIES.get(devtype);
   }

   private Stream<AlexaCapability> capabilities(Model m) {
      Stream<AlexaCapability> mapped = m.getCapabilities().stream()
         .filter(caps::containsKey)
         .map(caps::get);

      if(m.supports(SceneCapability.NAMESPACE)) {
         // looks odd, but device connection cap is mapped to the endpoint health (online/offline) stuff for alexa
         // and we don't have a concept of that for scenes
         return Stream.concat(mapped, ImmutableList.of(caps.get(DeviceConnectionCapability.NAMESPACE)).stream());
      }

      return mapped;
   }

}

