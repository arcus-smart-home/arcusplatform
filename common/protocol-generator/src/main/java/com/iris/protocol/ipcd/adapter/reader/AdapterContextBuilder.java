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
package com.iris.protocol.ipcd.adapter.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.iris.protocol.ipcd.adapter.context.AdapterContext;
import com.iris.protocol.ipcd.adapter.context.AptDeviceDef;
import com.iris.protocol.ipcd.adapter.context.AptDeviceValue;
import com.iris.protocol.ipcd.adapter.context.AptParameterDef;
import com.iris.protocol.ipcd.aos.xml.model.Adapter;
import com.iris.protocol.ipcd.aos.xml.model.AptDevice;
import com.iris.protocol.ipcd.aos.xml.model.AptEvent;
import com.iris.protocol.ipcd.aos.xml.model.AptEvents;
import com.iris.protocol.ipcd.aos.xml.model.AptParameter;
import com.iris.protocol.ipcd.aos.xml.model.AptParameters;

public class AdapterContextBuilder {
   public static final String PARAMETER_INDICATOR = "#";
   public static final String SEPARATOR = ",";
      
   public AdapterContext build(Adapter adapter) {
      AdapterContext context = new AdapterContext();
      context.setDeviceDef(buildDevice(adapter.getDevice()));
      context.setSupportedEvents(buildSupportedEvents(adapter.getEvents()));
      context.setParameterDefs(buildParameters(adapter.getParameters()));
      return context;
   }
   
   private AptDeviceDef buildDevice(AptDevice device) {
      AptDeviceDef deviceDef = new AptDeviceDef();
      deviceDef.setVendor(getValue(device.getVendor()));
      deviceDef.setModel(getValue(device.getModel()));
      deviceDef.setSn(getValue(device.getSn()));
      deviceDef.setIpcdver(getValue(device.getIpcdver()));
      deviceDef.setFwver(getValue(device.getFwver()));
      deviceDef.setConnectURL(getValue(device.getConnectURL()));
      deviceDef.setConnection(getValue(device.getConnection()));
      deviceDef.setActions(getValue(device.getActions()));
      deviceDef.setCommands(getValue(device.getCommands()));
      return deviceDef;
   }
   
   private List<String> buildSupportedEvents(AptEvents events) {
      List<AptEvent> eventList = events.getEvent();
      List<String> supportedEvents = new ArrayList<>();
      if (eventList != null && !eventList.isEmpty()) {
         for (AptEvent event : eventList) {
            supportedEvents.add(event.getName());
         }
      }
      return supportedEvents;
   }
   
   private List<AptParameterDef> buildParameters(AptParameters params) {
      if (params == null || params.getParameter() == null) {
         return Collections.emptyList();
      }
      List<AptParameterDef> list = new ArrayList<>(params.getParameter().size());
      for (AptParameter p : params.getParameter()) {
         list.add(buildParameter(p));
      }
      return list;
   }
   
   private AptParameterDef buildParameter(AptParameter p) {
      AptParameterDef paramDef = new AptParameterDef();
      paramDef.setAosName(p.getAosName());
      paramDef.setAosType(p.getAosType());
      paramDef.setName(p.getName());
      paramDef.setType(p.getType());
      paramDef.setValues(getList(p.getValues()));
      paramDef.setAttrib(p.getAttrib());
      paramDef.setUnit(p.getUnit());
      paramDef.setFloor(getDouble(p.getFloor()));
      paramDef.setCeiling(getDouble(p.getCeiling()));
      paramDef.setDescription(p.getDescription());
      return paramDef;
   }
   
   private static Double getDouble(String s) {
      if (StringUtils.isEmpty(s)) {
         return null;
      }
      try {
         return Double.valueOf(s);
      } catch(Exception ex) {
         return null;
      }
   }
   
   private static List<String> getList(String s) {
      if (StringUtils.isEmpty(s)) {
         return Collections.emptyList();
      }
      if (s.contains(SEPARATOR)) {
         return Arrays.asList(s.split(SEPARATOR));
      }
      else {
         return Arrays.asList(s);
      }
   }
   
   private static AptDeviceValue getValue(String s) {
      AptDeviceValue value = new AptDeviceValue();
      if (s.startsWith(PARAMETER_INDICATOR)) {
         value.setValue(s.substring(1));
         value.setParameter(true);
      }
      else {
         value.setValue(s);
         value.setParameter(false);
      }
      return value;
   }
}

