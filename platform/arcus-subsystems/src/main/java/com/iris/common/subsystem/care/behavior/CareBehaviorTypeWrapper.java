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
package com.iris.common.subsystem.care.behavior;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.iris.messages.type.CareBehavior;

public class CareBehaviorTypeWrapper {
   private Map<String,Object>attributes;
   
   @Override
   public String toString() {
      return "CareBehaviorTypeWrapper [attributes=" + attributes + "]";
   }

   public CareBehaviorTypeWrapper(Map<String, Object> attributes) {
      this.attributes = new HashMap<>(attributes);
   }

   public Map<String, Object> getAttributes() {
      return attributes;
   }
   
   public Map<String, Object> toMap(){
      return attributes;
   }
   
   public String getId(){
         return toStringOrDefault(attributes.get(CareBehavior.ATTR_ID),null);
   }
   
   public void setId(String id){
      attributes.put(CareBehavior.ATTR_ID,id);
   }


   
   public void setLastFired(Date date){
      attributes.put(CareBehavior.ATTR_LASTFIRED, date);
   }
   
   public void setLastActivated(Date date){
      attributes.put(CareBehavior.ATTR_LASTACTIVATED, date);
   }
   public Date getLastActivated(){
      CareBehavior behavior = new CareBehavior(attributes);
      return behavior.getLastActivated();
   }
   
   public Set<String> getDevices(){
      CareBehavior behavior = new CareBehavior(attributes);
      return behavior.getDevices();
   }   
   
   public Set<String> getAvailableDevices(){
      CareBehavior behavior = new CareBehavior(attributes);
      return behavior.getAvailableDevices();
   }   

   public boolean getEnabled(){
      CareBehavior behavior = new CareBehavior(attributes);
      return behavior.getEnabled()==null?false:behavior.getEnabled();
   }
   
   public String getType(){
      return toStringOrDefault(attributes.get(CareBehavior.ATTR_TYPE),null);
   }

   public String getName(){
      return toStringOrDefault(attributes.get(CareBehavior.ATTR_NAME),null);
   }

   public String getTemplateId(){
      return toStringOrDefault(attributes.get(CareBehavior.ATTR_TEMPLATEID),null);
   }
   
   public void setAvailbleDevices(List<String>availableDevices){
      attributes.put(CareBehavior.ATTR_AVAILABLEDEVICES, availableDevices);
   }
   
   public void setDevices(List<String> devices){
      attributes.put(CareBehavior.ATTR_DEVICES, devices);
   }
   
   public List<Map<String,Object>>getTimeWindows(){
      return (List<Map<String,Object>>)attributes.get(CareBehavior.ATTR_TIMEWINDOWS);
   }

   //public void setEnabled(boolean enabled){
   //   attributes.put(CareBehavior.ATTR_ENABLED, enabled);
   //}
   
   public void setActive(boolean active){
      attributes.put(CareBehavior.ATTR_ACTIVE, active);
   }

   public boolean getActive(){
      Boolean active = new CareBehavior(attributes).getActive();
      return active!=null && active;
   }
   
   private String toStringOrDefault(Object value,String defaultVal){
      if(value==null){
         return defaultVal;
      }
      return String.valueOf(value);
      
   }
}

