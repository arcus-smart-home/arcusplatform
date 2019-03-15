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
package com.iris.protocol.ipcd.adapter.context;

import java.util.List;

public class AdapterContext {
   private AptDeviceDef deviceDef;
   private List<String> supportedEvents;
   private List<AptParameterDef> parameterDefs;
   
   public AptDeviceDef getDeviceDef() {
      return deviceDef;
   }
   public void setDeviceDef(AptDeviceDef deviceDef) {
      this.deviceDef = deviceDef;
   }
   public List<String> getSupportedEvents() {
      return supportedEvents;
   }
   public void setSupportedEvents(List<String> supportedEvents) {
      this.supportedEvents = supportedEvents;
   }
   public List<AptParameterDef> getParameterDefs() {
      return parameterDefs;
   }
   public void setParameterDefs(List<AptParameterDef> parameterDefs) {
      this.parameterDefs = parameterDefs;
   }
}

