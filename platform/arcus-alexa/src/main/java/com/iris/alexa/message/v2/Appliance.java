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
package com.iris.alexa.message.v2;

import java.util.Map;
import java.util.Set;

public class Appliance {

   public enum Type {
      CAMERA, LIGHT, SMARTLOCK, SMARTPLUG, SWITCH, THERMOSTAT, ACTIVITY_TRIGGER, SCENE_TRIGGER
   }

   public static final String SET_TEMPERATURE_ACTION = "setTargetTemperature";
   public static final String INC_TEMPERATURE_ACTION = "incrementTargetTemperature";
   public static final String DEC_TEMPERATURE_ACTION = "decrementTargetTemperature";
   public static final String SET_PERCENTAGE_ACTION = "setPercentage";
   public static final String INC_PERCENTAGE_ACTION = "incrementPercentage";
   public static final String DEC_PERCENTAGE_ACTION = "decrementPercentage";
   public static final String TURN_OFF_ACTION = "turnOff";
   public static final String TURN_ON_ACTION = "turnOn";
   public static final String GET_LOCKSTATE_ACTION = "getLockState";
   public static final String SET_LOCKSTATE_ACTION = "setLockState";
   public static final String GET_TEMPERATURE_READING_ACTION = "getTemperatureReading";
   public static final String GET_TARGET_TEMPERATURE_ACTION = "getTargetTemperature";

   public static final int DEF_FAN_MAXSPEED = 3;
   public static final String FAN_DEVICETYPE = "Fan Control";
   public static final String THERMOSTAT_DEVICETYPE = "Thermostat";
   public static final String LIGHT_DEVICETYPE = "Light";
   public static final String SWITCH_DEVICETYPE = "Switch";
   public static final String DIMMER_DEVICETYPE = "Dimmer";
   public static final String LOCK_DEVICETYPE = "Lock";

   private Set<String> actions;
   private Map<String,String> additionalApplianceDetails;
   private String applianceId;
   private String friendlyDescription;
   private String friendlyName;
   private boolean isReachable;
   private String manufacturerName;
   private String modelName;
   private String version;
   private Set<Type> applianceTypes;

   public Set<String> getActions() {
      return actions;
   }

   public void setActions(Set<String> actions) {
      this.actions = actions;
   }

   public Map<String, String> getAdditionalApplianceDetails() {
      return additionalApplianceDetails;
   }

   public void setAdditionalApplianceDetails(
         Map<String, String> additionalApplianceDetails) {
      this.additionalApplianceDetails = additionalApplianceDetails;
   }

   public String getApplianceId() {
      return applianceId;
   }

   public void setApplianceId(String applianceId) {
      this.applianceId = applianceId;
   }

   public String getFriendlyDescription() {
      return friendlyDescription;
   }

   public void setFriendlyDescription(String friendlyDescription) {
      this.friendlyDescription = friendlyDescription;
   }

   public String getFriendlyName() {
      return friendlyName;
   }

   public void setFriendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
   }

   public boolean isReachable() {
      return isReachable;
   }

   public void setReachable(boolean isReachable) {
      this.isReachable = isReachable;
   }

   public String getManufacturerName() {
      return manufacturerName;
   }

   public void setManufacturerName(String manufacturerName) {
      this.manufacturerName = manufacturerName;
   }

   public String getModelName() {
      return modelName;
   }

   public void setModelName(String modelName) {
      this.modelName = modelName;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public Set<Type> getApplianceTypes() {
      return applianceTypes;
   }

   public void setApplianceTypes(Set<Type> applianceTypes) {
      this.applianceTypes = applianceTypes;
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      Appliance appliance = (Appliance) o;

      if(isReachable != appliance.isReachable) return false;
      if(actions != null ? !actions.equals(appliance.actions) : appliance.actions != null) return false;
      if(additionalApplianceDetails != null ? !additionalApplianceDetails.equals(appliance.additionalApplianceDetails) : appliance.additionalApplianceDetails != null)
         return false;
      if(applianceId != null ? !applianceId.equals(appliance.applianceId) : appliance.applianceId != null) return false;
      if(friendlyDescription != null ? !friendlyDescription.equals(appliance.friendlyDescription) : appliance.friendlyDescription != null)
         return false;
      if(friendlyName != null ? !friendlyName.equals(appliance.friendlyName) : appliance.friendlyName != null)
         return false;
      if(manufacturerName != null ? !manufacturerName.equals(appliance.manufacturerName) : appliance.manufacturerName != null)
         return false;
      if(modelName != null ? !modelName.equals(appliance.modelName) : appliance.modelName != null) return false;
      if(version != null ? !version.equals(appliance.version) : appliance.version != null) return false;
      return applianceTypes != null ? applianceTypes.equals(appliance.applianceTypes) : appliance.applianceTypes == null;
   }

   @Override
   public int hashCode() {
      int result = actions != null ? actions.hashCode() : 0;
      result = 31 * result + (additionalApplianceDetails != null ? additionalApplianceDetails.hashCode() : 0);
      result = 31 * result + (applianceId != null ? applianceId.hashCode() : 0);
      result = 31 * result + (friendlyDescription != null ? friendlyDescription.hashCode() : 0);
      result = 31 * result + (friendlyName != null ? friendlyName.hashCode() : 0);
      result = 31 * result + (isReachable ? 1 : 0);
      result = 31 * result + (manufacturerName != null ? manufacturerName.hashCode() : 0);
      result = 31 * result + (modelName != null ? modelName.hashCode() : 0);
      result = 31 * result + (version != null ? version.hashCode() : 0);
      result = 31 * result + (applianceTypes != null ? applianceTypes.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "Appliance{" +
         "actions=" + actions +
         ", additionalApplianceDetails=" + additionalApplianceDetails +
         ", applianceId='" + applianceId + '\'' +
         ", friendlyDescription='" + friendlyDescription + '\'' +
         ", friendlyName='" + friendlyName + '\'' +
         ", isReachable=" + isReachable +
         ", manufacturerName='" + manufacturerName + '\'' +
         ", modelName='" + modelName + '\'' +
         ", version='" + version + '\'' +
         ", applianceTypes=" + applianceTypes +
         '}';
   }
}

