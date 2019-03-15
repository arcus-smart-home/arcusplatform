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

import static com.iris.alexa.AlexaUtil.getFromModelOrInternalError;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.commons.math3.util.Precision;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.dev.DoorLockModel;
import com.iris.messages.model.dev.FanModel;
import com.iris.messages.model.dev.LightModel;
import com.iris.messages.model.dev.SwitchModel;
import com.iris.messages.model.dev.ThermostatModel;
import com.iris.messages.type.AlexaColor;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.voice.VoicePredicates;
import com.iris.voice.context.VoiceContext;

public enum PropertyReporter {
   ;

   //-------------------------------------------------------------------------------------------------------------------
   // Internal Interface that the property reporter will use
   //-------------------------------------------------------------------------------------------------------------------

   interface PropertyReportable {
      boolean supported(Model m);
      String namespace();
      String propertyName();
      void populate(VoiceContext context, AlexaPropertyReport report, Model m);
   }

   private static final List<PropertyReportable> properties = ImmutableList.<PropertyReportable>builder()
      //----------------------------------------------------------------------------------------------------------------
      // brightness
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(DimmerCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.BrightnessController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            Integer b = getFromModelOrInternalError(m, DimmerCapability.ATTR_BRIGHTNESS, DimmerCapability.TYPE_BRIGHTNESS);
            report.setValue(b);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // color
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ColorCapability.NAMESPACE) && LightModel.isColormodeCOLOR(m);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ColorController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ColorController.PROP_COLOR;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            AlexaColor color = new AlexaColor();
            if(m.supports(DimmerCapability.NAMESPACE)) {
               Integer b = getFromModelOrInternalError(m, DimmerCapability.ATTR_BRIGHTNESS, DimmerCapability.TYPE_BRIGHTNESS);
               color.setBrightness(Precision.round(b / 100d, 2));
            } else {
               color.setBrightness(1.0);
            }
            Integer hue = getFromModelOrInternalError(m, ColorCapability.ATTR_HUE, ColorCapability.TYPE_HUE);
            Integer sat = getFromModelOrInternalError(m, ColorCapability.ATTR_SATURATION, ColorCapability.TYPE_SATURATION);
            color.setHue((double) Precision.round(hue, 2));
            color.setSaturation(Precision.round(sat / 100d, 2));
            report.setValue(color.toMap());
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // color temperature
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ColorTemperatureCapability.NAMESPACE) && LightModel.isColormodeCOLORTEMP(m);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ColorTemperatureController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            Integer ct = getFromModelOrInternalError(m, ColorTemperatureCapability.ATTR_COLORTEMP, ColorTemperatureCapability.TYPE_COLORTEMP);
            report.setValue(ct);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // lock
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(DoorLockCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.LockController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.LockController.PROP_LOCKSTATE;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            String state = getFromModelOrInternalError(m, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.TYPE_LOCKSTATE);
            if(VoicePredicates.isLockJammed(m)) {
               state = "JAMMED";
            } else if(DoorLockCapability.LOCKSTATE_LOCKING.equals(state) || DoorLockCapability.LOCKSTATE_UNLOCKING.equals(state)) {
               throw new AlexaException(AlexaErrors.ENDPOINT_BUSY);
            }
            report.setValue(state);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // percentage
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(FanCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.PercentageController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.PercentageController.PROP_PERCENTAGE;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            if(SwitchModel.isStateOFF(m)) {
               report.setValue(0);
            } else {
               Integer speed = getFromModelOrInternalError(m, FanCapability.ATTR_SPEED, FanCapability.TYPE_SPEED);
               double interval = 100d / FanModel.getMaxSpeed(m, 3);
               int perc = (int) Math.round(interval * speed);
               report.setValue(perc);
            }
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // power
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(SwitchCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.PowerController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.PowerController.PROP_POWERSTATE;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            report.setValue(getFromModelOrInternalError(m, SwitchCapability.ATTR_STATE, SwitchCapability.TYPE_STATE));
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // temperature
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(TemperatureCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.TemperatureSensor.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            Double t = getFromModelOrInternalError(m, TemperatureCapability.ATTR_TEMPERATURE, TemperatureCapability.TYPE_TEMPERATURE);
            report.setValue(AlexaUtil.createAlexaTemp(t).toMap());
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // thermostat target
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ThermostatCapability.NAMESPACE) &&
               !ThermostatModel.isHvacmodeOFF(m) &&
               !ThermostatModel.isHvacmodeECO(m) &&
               !ThermostatModel.isHvacmodeAUTO(m);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ThermostatController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            String mode = getFromModelOrInternalError(m, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.TYPE_HVACMODE);
            switch(mode) {
               case ThermostatCapability.HVACMODE_COOL:
                  Double c = getFromModelOrInternalError(m, ThermostatCapability.ATTR_COOLSETPOINT, ThermostatCapability.TYPE_COOLSETPOINT);
                  report.setValue(AlexaUtil.createAlexaTemp(c).toMap());
                  break;
               case ThermostatCapability.HVACMODE_HEAT:
                  Double h = getFromModelOrInternalError(m, ThermostatCapability.ATTR_HEATSETPOINT, ThermostatCapability.TYPE_HEATSETPOINT);
                  report.setValue(AlexaUtil.createAlexaTemp(h).toMap());
                  break;
               default:
                  break;
            }
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // thermostat lower
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ThermostatCapability.NAMESPACE) && ThermostatModel.isHvacmodeAUTO(m);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ThermostatController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            Double h = getFromModelOrInternalError(m, ThermostatCapability.ATTR_HEATSETPOINT, ThermostatCapability.TYPE_HEATSETPOINT);
            report.setValue(AlexaUtil.createAlexaTemp(h).toMap());
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // thermostat upper
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ThermostatCapability.NAMESPACE) && ThermostatModel.isHvacmodeAUTO(m);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ThermostatController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            Double c = getFromModelOrInternalError(m, ThermostatCapability.ATTR_COOLSETPOINT, ThermostatCapability.TYPE_COOLSETPOINT);
            report.setValue(AlexaUtil.createAlexaTemp(c).toMap());
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // thermostat mode
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return m.supports(ThermostatCapability.NAMESPACE);
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.ThermostatController.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            String mode = getFromModelOrInternalError(m, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.TYPE_HVACMODE);
            report.setValue(mode);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // connectivity
      .add(new PropertyReportable() {
         @Override
         public boolean supported(Model m) {
            return true;
         }

         @Override
         public String namespace() {
            return AlexaInterfaces.EndpointHealth.NAMESPACE;
         }

         @Override
         public String propertyName() {
            return AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY;
         }

         @Override
         public void populate(VoiceContext context, AlexaPropertyReport report, Model m) {
            boolean offline = VoicePredicates.isDeviceOffline(m, context.isHubOffline());
            report.setValue(ImmutableMap.of("value", offline ? "UNREACHABLE" : "OK"));
         }
      })
      .build();

   public static List<AlexaPropertyReport> report(VoiceContext context, Model m) {
      ImmutableList.Builder<AlexaPropertyReport> reports = ImmutableList.builder();
      properties.forEach(pr -> {
         if(pr.supported(m)) {
            AlexaPropertyReport report = new AlexaPropertyReport();
            report.setNamespace(pr.namespace());
            report.setName(pr.propertyName());
            report.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
            report.setUncertaintyInMilliseconds(0L);
            pr.populate(context, report, m);
            reports.add(report);
         }
      });
      return reports.build();
   }

}

