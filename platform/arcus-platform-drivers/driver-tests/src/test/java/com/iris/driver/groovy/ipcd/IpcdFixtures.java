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
package com.iris.driver.groovy.ipcd;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.DeviceInfo;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoCommand;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoResponse;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.SetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.SetParameterValuesResponse;
import com.iris.protocol.ipcd.message.model.Status;
import com.iris.protocol.ipcd.message.model.StatusType;
import com.iris.protocol.ipcd.message.model.ValueChange;
import com.iris.util.IrisCollections;

public class IpcdFixtures {
   public static AttributeMap createProtocolAttributes() {
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(strKey(IpcdProtocol.ATTR_VENDOR), "BlackBox");
      attributes.set(strKey(IpcdProtocol.ATTR_MODEL), "ms2");
      attributes.set(strKey(IpcdProtocol.ATTR_SN), "abc123");
      attributes.set(strKey(IpcdProtocol.ATTR_IPCDVER), "1.0");
      attributes.set(strKey(IpcdProtocol.ATTR_FWVER), "3.2.1a");
      attributes.set(strKey(IpcdProtocol.ATTR_CONNECTION), "persistent");
      attributes.set(setKey(IpcdProtocol.ATTR_COMMANDS),
               IrisCollections.setOf("GetDeviceInfo", 
                     "SetDeviceInfo", 
                     "GetParameterValues",
                     "SetParameterValues",
                     "GetParameterInfo",
                     "GetReportConfiguration",
                     "SetReportConfiguration",
                     "GetEventConfiguration",
                     "SetEventConfiguration",
                     "Download",
                     "FactoryReset")
            );
      attributes.set(setKey(IpcdProtocol.ATTR_ACTIONS), IrisCollections.setOf("Report", "Event"));
      return attributes;
   };
   
   public static Device getDevice() {
      Device device = new Device();
      device.setVendor("BlackBox");
      device.setModel("ms2");
      device.setSn("abc123");
      device.setIpcdver("1.0");
      return device;
   }
   
   public static DeviceInfo getDeviceInfo() {
      DeviceInfo deviceInfo = new DeviceInfo();
      deviceInfo.setFwver("3.2.1a");
      return deviceInfo;
   }
   
   public static IpcdEvent getEvent() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(getDevice());
      event.setEvents(Arrays.asList("onValueChange", "onUpdate"));
      event.setValueChanges(Collections.<ValueChange>emptyList());
      return event;
   }
   
   public static GetDeviceInfoResponse getDeviceInfoResponse(StatusType statusType) {
      GetDeviceInfoResponse response = new GetDeviceInfoResponse();
      GetDeviceInfoCommand command = new GetDeviceInfoCommand();
      response.setRequest(command);
      Status status = new Status();
      status.setResult(statusType);
      response.setStatus(status);
      response.setResponse(getDeviceInfo());
      response.setDevice(getDevice());
      return response;
   }
   
   public static SetParameterValuesResponse getSetParameterValuesResponse(StatusType statusType) {
      SetParameterValuesResponse response = new SetParameterValuesResponse();
      SetParameterValuesCommand command = new SetParameterValuesCommand();
      response.setRequest(command);
      Status status = new Status();
      status.setResult(statusType);
      response.setStatus(status);
      response.setResponse(Collections.<String,Object>emptyMap());
      response.setDevice(getDevice());
      return response;
   }
   
   public static ValueChange createValueChange(String parameter, Object value) {
      ValueChange vc = new ValueChange();
      vc.setParameter(parameter);
      vc.setValue(value);
      vc.setThresholdRule("onChange");
      return vc;
   }
   
   public static ValueChange createValueChange(String parameter, Object value, String rule, Object ruleValue) {
      ValueChange vc = new ValueChange();
      vc.setParameter(parameter);
      vc.setValue(value);
      vc.setThresholdRule(rule);
      vc.setThresholdValue(ruleValue);
      return vc;
   }
   
   private static AttributeKey<String> strKey(String attr) {
      return AttributeKey.create(attr, String.class);
   }
   
   private static AttributeKey<Set<String>> setKey(String attr) {
      return AttributeKey.createSetOf(attr, String.class);
   }
}


