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
package com.iris.voice.alexa.reporting;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.Scope;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.service.VoiceService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.util.IrisUUID;
import com.iris.voice.VoicePredicates;
import com.iris.voice.VoiceUtil;
import com.iris.voice.alexa.AlexaConfig;
import com.iris.voice.alexa.AlexaMetrics;
import com.iris.voice.alexa.AlexaPredicates;
import com.iris.voice.alexa.http.AlexaHttpClient;
import com.iris.voice.alexa.http.SkillDisabledException;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceDAO;
import com.iris.voice.proactive.ProactiveCreds;
import com.iris.voice.proactive.ProactiveCredsDAO;
import com.iris.voice.proactive.ProactiveReportHandler;

@Singleton
public class AlexaProactiveReportHandler implements ProactiveReportHandler {

   private static final Logger logger = LoggerFactory.getLogger(AlexaProactiveReportHandler.class);

   private static final Set<String> interestingAttributes = ImmutableSet.of(
      SwitchCapability.ATTR_STATE,
      DimmerCapability.ATTR_BRIGHTNESS,
      FanCapability.ATTR_SPEED,
      ColorCapability.ATTR_HUE,
      ColorCapability.ATTR_SATURATION,
      ColorTemperatureCapability.ATTR_COLORTEMP,
      DoorLockCapability.ATTR_LOCKSTATE,
      TemperatureCapability.ATTR_TEMPERATURE,
      ThermostatCapability.ATTR_COOLSETPOINT,
      ThermostatCapability.ATTR_HEATSETPOINT,
      ThermostatCapability.ATTR_HVACMODE,
      DeviceConnectionCapability.ATTR_STATE,
      LightCapability.ATTR_COLORMODE,
      DeviceAdvancedCapability.ATTR_ERRORS
   );

   private final AlexaConfig config;
   private final AlexaHttpClient client;
   private final ProactiveCredsDAO proactiveCredsDao;
   private final VoiceDAO voiceDao;
   private final ConcurrentMap<Address, Set<DeferredCorrelator>> pendingResponses = new ConcurrentHashMap<>();
   private final ProductCatalogManager prodCat;

   @Inject
   public AlexaProactiveReportHandler(AlexaConfig config, AlexaHttpClient client, ProactiveCredsDAO proactiveCredsDao, VoiceDAO voiceDao, ProductCatalogManager prodCat) {
      this.config = config;
      this.client = client;
      this.proactiveCredsDao = proactiveCredsDao;
      this.voiceDao = voiceDao;
      this.prodCat = prodCat;
   }

   @Override
   public boolean isInterestedIn(VoiceContext context, Model m, MessageBody body) {
      if(!Capability.EVENT_VALUE_CHANGE.equals(body.getMessageType())) {
         return false;
      }

      if(m.supports(HubCapability.NAMESPACE) && body.getAttributes().containsKey(HubConnectionCapability.ATTR_STATE)) {
         return true;
      }

      Map<String, Object> attributes = body.getAttributes();

      if(attributes.keySet().stream().noneMatch(interestingAttributes::contains)) {
         return false;
      }

      // special cases for intermediate door lock states
      if(
         DoorLockCapability.LOCKSTATE_LOCKING.equals(attributes.get(DoorLockCapability.ATTR_LOCKSTATE)) ||
         DoorLockCapability.LOCKSTATE_UNLOCKING.equals(attributes.get(DoorLockCapability.ATTR_LOCKSTATE))
      ) {
         return false;
      }

      return AlexaPredicates.supported(m, VoiceUtil.getProduct(prodCat, m));
   }

   @Override
   public void report(VoiceContext context, Model m, MessageBody body) {
      Optional<ProactiveCreds> optionalCreds = context.getProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      if(!optionalCreds.isPresent()) {
         logger.trace("attempting to report model state for {} @ {}, but no creds exist.  place is likely a v2 customer", m.getAddress(), context.getPlaceId());
         return;
      }

      Set<DeferredCorrelator> correlators = pendingResponses.get(m.getAddress());
      if(correlators != null) {
         Set<DeferredCorrelator> correlatorsClone = new HashSet<>(correlators);
         Set<DeferredCorrelator> done = respond(optionalCreds.get(), context, m, body, correlatorsClone);
         correlatorsClone.removeAll(done);
         if(correlatorsClone.isEmpty()) {
            pendingResponses.remove(m.getAddress(), correlators);
         }
         return;
      }

      if(config.isProactiveEnabled()) {
         event(optionalCreds.get(), context, m, body);
      }
   }

   private Set<DeferredCorrelator> respond(ProactiveCreds creds, VoiceContext context, Model m, MessageBody body, Set<DeferredCorrelator> correlators) {
      Set<DeferredCorrelator> done = new HashSet<>();

      ProactiveCreds newCreds = refreshCreds(creds, context);
      Endpoint e = endpoint(newCreds, m);

      correlators.forEach(correlator -> {
         if(correlator.complete()) {
            done.add(correlator);
            if(correlator.expired()) {
               logger.warn("removing deferred response {} due to timeout", correlator.getMessageId());
            }
         } else if(correlator.complete(body)) {
            done.add(correlator);
            List<AlexaPropertyReport> report = PropertyReporter.report(context, m);
            Map<String, Object> contextPayload = ImmutableMap.of("properties", report.stream().map(AlexaPropertyReport::toMap).collect(Collectors.toList()));
            Header h = Header.v3(correlator.getMessageId(), AlexaInterfaces.RESPONSE_NAME, AlexaInterfaces.RESPONSE_NAMESPACE, correlator.getCorrelationToken());
            AlexaMessage msg = new AlexaMessage(h, ImmutableMap.of(), e, contextPayload);
            client.report(msg);
         }
      });

      return done;
   }

   private void event(ProactiveCreds creds, VoiceContext context, Model m, MessageBody body) {

      if(m.supports(HubCapability.NAMESPACE)) {
         if(HubConnectionModel.isStateOFFLINE(m)) {
            eventHubOffline(creds, context);
         } else {
            eventHubOnline(creds ,context);
         }
         return;
      }

      try {
         Map<String, Object> attrs = body.getAttributes();
         List<AlexaPropertyReport> report = PropertyReporter.report(context, m);

         List<Map<String, Object>> contextProps = new LinkedList<>();
         List<Map<String, Object>> payloadProps = new LinkedList<>();

         report.forEach(r -> {
            if(wasChange(r, attrs)) {
               r.setUncertaintyInMilliseconds(0L);
               payloadProps.add(r.toMap());
            } else {
               contextProps.add(r.toMap());
            }
         });

         Header h = Header.v3(IrisUUID.randomUUID().toString(), AlexaInterfaces.EVENT_REPORT, AlexaInterfaces.RESPONSE_NAMESPACE, null);
         ImmutableMap.Builder<String, Object> bodyPayloadBuilder = ImmutableMap.builder();
         bodyPayloadBuilder.put(
            "change", ImmutableMap.of(
               "cause", ImmutableMap.of("type", "APP_INTERACTION"),
               "properties", payloadProps
            ));

         Map<String, Object> contextPayload = ImmutableMap.of("properties", contextProps);

         ProactiveCreds newCreds = refreshCreds(creds, context);
         client.report(new AlexaMessage(h, bodyPayloadBuilder.build(), endpoint(newCreds, m), contextPayload));
      } catch(SkillDisabledException sde) {
         handleSkillDisabled(context);
      } catch(Exception e) {
         if(e instanceof AlexaException) {
            // these can pop out when trying to report properties that haven't been set initially after a device add
            logger.trace("error resolving properties during reporting");
         } else {
            logger.warn("failure to report changes", e);
         }
      }
   }

   private void eventHubOffline(ProactiveCreds creds, VoiceContext context) {
      eventOnHubState(creds, context, DeviceConnectionCapability.STATE_OFFLINE);
   }

   private void eventHubOnline(ProactiveCreds creds, VoiceContext context) {
      eventOnHubState(creds, context, DeviceConnectionCapability.STATE_ONLINE);
   }

   private void eventOnHubState(ProactiveCreds creds, VoiceContext context, String state) {
      context.streamSupported(
         model -> AlexaPredicates.supported(model, VoiceUtil.getProduct(prodCat, model)),
         Optional::ofNullable
      )
      .filter(VoicePredicates::isHubRequired)
      .forEach(
         m -> event(
            creds,
            context,
            m,
            MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(DeviceConnectionCapability.ATTR_STATE, state))
         )
      );
   }

   public void deferResponse(Address device, DeferredCorrelator deferredResponse) {
      this.pendingResponses.computeIfAbsent(device, key -> new HashSet<>()).add(deferredResponse);
   }

   private ProactiveCreds refreshCreds(ProactiveCreds curCreds, VoiceContext context) {
      ProactiveCreds retCreds = curCreds;
      if(curCreds.expired(TimeUnit.MINUTES.toMillis(config.getPreemptRefreshTimeMins()))) {
         retCreds = client.refreshCreds(context.getPlaceId(), curCreds.getRefresh());
         proactiveCredsDao.upsert(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA, retCreds);
         context.updateProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA, retCreds);
      }
      return retCreds;
   }

   private Endpoint endpoint(ProactiveCreds creds, Model m) {
      Scope s = new Scope("BearerToken", creds.getAccess());
      return new Endpoint(s, AlexaUtil.addressToEndpointId(m.getAddress().getRepresentation()), null);
   }

   private boolean wasChange(AlexaPropertyReport report, Map<String, Object> changes) {
      switch(report.getName()) {
         case AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS:
            return changes.containsKey(DimmerCapability.ATTR_BRIGHTNESS);
         case AlexaInterfaces.ColorController.PROP_COLOR:
            return
               changes.containsKey(ColorCapability.ATTR_SATURATION) ||
               changes.containsKey(ColorCapability.ATTR_HUE);
         case AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN:
            return changes.containsKey(ColorTemperatureCapability.ATTR_COLORTEMP);
         case AlexaInterfaces.LockController.PROP_LOCKSTATE:
            return changes.containsKey(DoorLockCapability.ATTR_LOCKSTATE);
         case AlexaInterfaces.PercentageController.PROP_PERCENTAGE:
            return changes.containsKey(FanCapability.ATTR_SPEED);
         case AlexaInterfaces.PowerController.PROP_POWERSTATE:
            return changes.containsKey(SwitchCapability.ATTR_STATE);
         case AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE:
            return changes.containsKey(TemperatureCapability.ATTR_TEMPERATURE);
         case AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT:
            return changes.containsKey(ThermostatCapability.ATTR_HEATSETPOINT);
         case AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT:
            return changes.containsKey(ThermostatCapability.ATTR_COOLSETPOINT);
         case AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT:
            return
               changes.containsKey(ThermostatCapability.ATTR_COOLSETPOINT) ||
               changes.containsKey(ThermostatCapability.ATTR_HEATSETPOINT);
         case AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE:
            return changes.containsKey(ThermostatCapability.ATTR_HVACMODE);
         case AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY:
            return changes.containsKey(DeviceConnectionCapability.ATTR_STATE);
         default:
            return false;
      }
   }

   private void handleSkillDisabled(VoiceContext context) {
      voiceDao.removeAssistant(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      proactiveCredsDao.remove(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      context.removeAssistant(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      AlexaMetrics.incSkillDisabled();
   }

}

