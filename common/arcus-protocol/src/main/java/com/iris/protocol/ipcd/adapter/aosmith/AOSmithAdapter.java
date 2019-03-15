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
package com.iris.protocol.ipcd.adapter.aosmith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.adapter.AdapterResponse;
import com.iris.protocol.ipcd.adapter.IpcdPollingAdapter;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoCommand;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoResponse;
import com.iris.protocol.ipcd.message.model.GetEventConfiguration;
import com.iris.protocol.ipcd.message.model.GetEventConfigurationCommand;
import com.iris.protocol.ipcd.message.model.GetEventConfigurationResponse;
import com.iris.protocol.ipcd.message.model.GetParameterInfoCommand;
import com.iris.protocol.ipcd.message.model.GetParameterInfoResponse;
import com.iris.protocol.ipcd.message.model.GetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.GetParameterValuesResponse;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.IpcdReport;
import com.iris.protocol.ipcd.message.model.IpcdResponse;
import com.iris.protocol.ipcd.message.model.ParameterInfo;
import com.iris.protocol.ipcd.message.model.SetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.SetParameterValuesResponse;
import com.iris.protocol.ipcd.message.model.Status;
import com.iris.protocol.ipcd.message.model.StatusType;
import com.iris.protocol.ipcd.message.model.ValueChange;
import com.iris.protocol.ipcd.message.model.ValueChangeThreshold;
import com.iris.type.TypeUtil;
import com.iris.util.UnitConversion;

public class AOSmithAdapter extends AosConstants implements IpcdPollingAdapter<Map<String,String>> {
   private static final List<String> ON_CONNECT_EVENT = Arrays.asList(IpcdProtocol.EVENT_ON_CONNECT);
   private static final List<String> EVENTS = Arrays.asList("onValueChange");
   private static final List<String> VALUE_SUBEVENTS = Arrays.asList("onChange");
   private static final Function<Object, Double>  TO_DOUBLE = TypeUtil.INSTANCE.createTransformer(Double.class);
   
   private final Map<String, String> parameterMap = new ConcurrentHashMap<>();
   
   private AtomicReference<List<SetParameterValuesCommand>> sendCommandsRef = 
         new AtomicReference<List<SetParameterValuesCommand>>(new ArrayList<SetParameterValuesCommand>());
   private List<AOSPending> pendingCommands = new ArrayList<>();

   @Override
   public AdapterResponse<String> processNativeMessage(Map<String, String> nativeMsg) {
      List<ValueChange> changes = new ArrayList<>();
      List<IpcdMessage> messages = new ArrayList<>();
      // Update all parameters
      if (nativeMsg != null && !nativeMsg.isEmpty()) {
         String units = nativeMsg.get(AOS_PARAM_UNITS);
         if (units == null) {
            units = parameterMap.get(AOS_PARAM_UNITS);
         }
         Map<String, Object> reportMap = new HashMap<>(nativeMsg.size());
         for (Map.Entry<String, String> entry : nativeMsg.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!StringUtils.isEmpty(key) && value != null && aosToIpcd.containsKey(key)) {
               String convertedKey = AosConstants.aosToIpcd.get(key);
               Object convertedValue = convertValue(convertedKey, value, units);
            
               // Create Reports
               reportMap.put(convertedKey, convertedValue);
            
               // Create Value Change Events
               String oldValue = parameterMap.put(key, value);
               if (oldValue != null && !oldValue.equals(value)) {
                  ValueChange vc = new ValueChange();
                  vc.setParameter(convertedKey);
                  vc.setValue(convertedValue);
                  vc.setThresholdRule(IpcdProtocol.THRESHOLD_RULE_ON_CHANGE);
                  changes.add(vc);
               }
            }
         }
         
         Device device = AosDeviceBuilder.buildDevice(parameterMap);
         
         // Send Reports
         IpcdReport report = new IpcdReport();
         report.setDevice(device);
         report.setReport(reportMap);
         messages.add(report);
         
         // Send Events
         if (!changes.isEmpty()) {
            IpcdEvent event = new IpcdEvent();
            event.setDevice(device);
            event.setEvents(EVENTS);
            event.setValueChanges(changes);
            messages.add(event);
         }
         
         // Go through pending responses 
         List<IpcdMessage> responseMessages = createResponseMessages(device);
         messages.addAll(responseMessages);
         // Create the native and ipcd responses from this update from the device.
         String aosReturnJson = createAOSReturnJson(device, sendCommandsRef.getAndSet(new ArrayList<SetParameterValuesCommand>()));
         AOSResponse aosResponse = new AOSResponse(aosReturnJson, messages);
         return aosResponse;
      }
      // Bad Message From the Device So Ignore It.
      return null;
   }

   @Override
   public IpcdMessage processIpcdMessage(IpcdMessage ipcdMsg) {    
      if (ipcdMsg instanceof SetParameterValuesCommand) {
         // This command can't be processed until the device polls so stash it away.
         sendCommandsRef.get().add((SetParameterValuesCommand)ipcdMsg);
         return null;
      }
      
      if (ipcdMsg instanceof GetParameterValuesCommand) {
         List<String> params = ((GetParameterValuesCommand) ipcdMsg).getParameters();
         Map<String,Object> valueMap = new HashMap<>();
         if (params != null && !params.isEmpty()) {
            String units = parameterMap.get(AOS_PARAM_UNITS);
            for (String param : params) {
               Object value = convertValue(param, parameterMap.get(AosConstants.ipcdToAos.get(param)), units);
               valueMap.put(param, value);
            }
         }
         GetParameterValuesResponse response = new GetParameterValuesResponse();
         initResponse(response, StatusType.success);
         response.setRequest((GetParameterValuesCommand) ipcdMsg);
         response.setResponse(valueMap);
         return response;
      }
      
      if (ipcdMsg instanceof GetParameterInfoCommand) {
         GetParameterInfoResponse response = new GetParameterInfoResponse();
         initResponse(response, StatusType.success);
         response.setRequest((GetParameterInfoCommand) ipcdMsg);
         response.setResponse(parameterInfoMap);
         return response;
      }
      
      if (ipcdMsg instanceof GetDeviceInfoCommand) {
         GetDeviceInfoResponse response = new GetDeviceInfoResponse();
         initResponse(response, StatusType.success);
         response.setRequest((GetDeviceInfoCommand) ipcdMsg);
         response.setResponse(AosDeviceInfoBuilder.buildDeviceInfo(parameterMap));
         return response;
      }
      
      if (ipcdMsg instanceof GetEventConfiguration) {
         GetEventConfigurationResponse response = new GetEventConfigurationResponse();
         initResponse(response, StatusType.success);
         response.setRequest((GetEventConfigurationCommand) ipcdMsg);
         GetEventConfiguration gec = new GetEventConfiguration();
         gec.setSupportedEvents(supportedEvents);
         gec.setEnabledEvents(supportedEvents);
         Map<String,List<String>> supportedValueChanges = new HashMap<>();
         Map<String,ValueChangeThreshold> enabledValueChanges = new HashMap<>();
         for (String param : ipcdToAos.keySet()) {
            supportedValueChanges.put(param, VALUE_SUBEVENTS);
            ValueChangeThreshold vct = new ValueChangeThreshold();
            vct.setOnChange(true);
            enabledValueChanges.put(param, vct);
         }
         gec.setSupportedValueChanges(supportedValueChanges);
         gec.setEnabledValueChanges(enabledValueChanges);
         response.setResponse(gec);
         return response;
      }
      
      return null;
   }
   
   public IpcdMessage createOnConnect(Device device) {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(device);
      event.setEvents(ON_CONNECT_EVENT);
      return event;
   }
   
   private void initResponse(IpcdResponse response, StatusType result) {
      initResponse(response, null, result, null);
   }
   
   /*
   private void initResponse(IpcdResponse response, Device device, StatusType result) {
      initResponse(response, device, result, null);
   }
   */
   
   private void initResponse(IpcdResponse response, Device device, StatusType result, List<String> messages) {
      if (device == null) {
         device = AosDeviceBuilder.buildDevice(parameterMap);
      }
      response.setDevice(device);
      Status status = new Status();
      status.setResult(result);
      status.setMessages(messages);
      response.setStatus(status);
   }
   
   private List<IpcdMessage> createResponseMessages(Device device) {
      List<IpcdMessage> ipcdMsgs = new ArrayList<>();
      if (!pendingCommands.isEmpty()) {
         try {
            String units = parameterMap.get(AOS_PARAM_UNITS);
            for (AOSPending pendingCommand : pendingCommands) {
               Map<String,String> expected = pendingCommand.getExpected();
               for (String param : expected.keySet()) {
                  if (!expected.get(param).equals(parameterMap.get(param))) {
                     String errMsg = aosToIpcd.get(param) + " parameter was set to " + expected.get(param) + " but is reported as " + parameterMap.get(param);
                     pendingCommand.addFail(errMsg);
                  }
               }
               Map<String,Object> results = new HashMap<>();
               for (Map.Entry<String,Object> entry : pendingCommand.getCommand().getValues().entrySet()) {
                  String aosParam = ipcdToAos.get(entry.getKey());
                  Object value = convertValue(entry.getKey(), parameterMap.get(aosParam), units);
                  results.put(entry.getKey(), value);
               }
               SetParameterValuesResponse response = new SetParameterValuesResponse();
               response.setDevice(device);
               response.setRequest(pendingCommand.getCommand());
               response.setStatus(pendingCommand.getStatus());
               response.setResponse(results);
               ipcdMsgs.add(response);
            }
         }
         finally {
            // Get the list ready for the next set of pending commands.
            pendingCommands.clear();   
         }
      }
      return ipcdMsgs;
   }
   
   public String createAOSReturnJson(Device device, List<SetParameterValuesCommand> cmds) {
      return createAOSReturnJson(unifyTheParameters(cmds));   // Does not use device.
   }

   // Squashes all of the prior commands into one set.
   public List<SetParameterValuesCommand> unifyTheParameters(List<SetParameterValuesCommand> commands) {
      Map<String,Object> newParameters = new HashMap<String,Object>();
      List<SetParameterValuesCommand> newCommands = new ArrayList<SetParameterValuesCommand>();
            
      for (SetParameterValuesCommand cmd : commands) {
         Map<String,Object> cmdParams = cmd.getValues();
         newParameters.putAll(cmdParams);
      }
      
      SetParameterValuesCommand cmd2 = new SetParameterValuesCommand();
      cmd2.setValues(newParameters);
     
      newCommands.add(cmd2);
      
      return newCommands;
   }
   
   
   public String createAOSReturnJson(List<SetParameterValuesCommand> cmds) {
      StringBuffer sb = new StringBuffer("{\"Success\":\"0\"");
      if (cmds != null && !cmds.isEmpty()) {
         Map<String,Object> aosParamToIpcdValue = new HashMap<>();
         Map<String, List<AOSPending>> processedCommands = new HashMap<>();
         // Create map of new parameter values.
         for (SetParameterValuesCommand cmd : cmds) {
            AOSPending pending = new AOSPending(cmd);
            Map<String,Object> settings = cmd.getValues();
            for (String param : settings.keySet()) {
               String aosName = ipcdToAos.get(param);
               if (aosName == null) {
                  pending.addFail("Parameter " + param + " is not a valid parameter name.");
                  break;
               }
               ParameterInfo paramInfo = parameterInfoMap.get(param);
               if (!IpcdProtocol.isWriteable(paramInfo)) {
                  pending.addFail("Parameter " + param + " is not writeable.");
                  break;
               }
               if (processedCommands.containsKey(aosName)) {
                  for (AOSPending processedCommand : processedCommands.get(param) ) {
                     processedCommand.removeExpected(aosName);
                  }
               }
               aosParamToIpcdValue.put(aosName, settings.get(param));
               List<AOSPending> processed = processedCommands.get(aosName);
               if (processed == null) {
                  processed = new LinkedList<AOSPending>();
                  processedCommands.put(aosName, processed);
               }
               processed.add(pending);
            }
            // Add to pending commands.
            pendingCommands.add(pending);
         }
         // Convert ipcd settings to AOS settings
         String units = aosParamToIpcdValue.containsKey(AOS_PARAM_UNITS)
               ? aosParamToIpcdValue.get(AOS_PARAM_UNITS).toString()
               : parameterMap.get(AOS_PARAM_UNITS);
         for (Map.Entry<String,Object> entry : aosParamToIpcdValue.entrySet()) {
            String aosName = entry.getKey();
            String aosValue = convertToAOSValue(aosName, entry.getValue(), units);
            // Add value to json.
            sb.append(",\"").append(aosName).append("\":\"").append(aosValue.trim()).append('"');
            // Add to expected values of pendings
            List<AOSPending> pendings = processedCommands.get(aosName);
            for (AOSPending pending : pendings) {
               pending.addExpected(aosName, aosValue);
            }
         }
      }
      sb.append('}');
      
      return sb.toString();
   }
   
   private Double adjustUnits(String key, Double d, String units) {
      if (key.equals(IPCD_PARAM_SETPOINT)
            || key.equals(IPCD_PARAM_MAXSETPOINT)
            || key.equals(AOS_PARAM_SETPOINT) 
            || key.equals(AOS_PARAM_MAXSETPOINT)) {
         // If we don't know the units then assume the driver does and send them unchanged.
         if (StringUtils.isEmpty(units) || units.equals(UNITS_C)) {
            return d;
         }
         else {
            return UnitConversion.tempFtoC(d);
         }
      }
      else {
         return d;
      }
   }
   
   private String convertToAOSValue(String key, Object value, String units) {
      // Set point is always in units of C in IPCD-land, but may be in C or F in AOS-land.
      if (AOS_PARAM_SETPOINT.equals(key)) {
         Double d = TO_DOUBLE.apply(value);
         
         if (UNITS_F.equals(units)) {
            d = UnitConversion.tempCtoF(d);
         }
         
         // The value must also always be an int.
         int convertedValue = (int)(d + 0.5);
         return String.valueOf(convertedValue);
      }
      
      return value.toString();
   }
   
   private Object convertValue(String key, String value, String units) {
      ParameterInfo paramInfo  = parameterInfoMap.get(key);
      if (paramInfo == null) {
         return value;
      }
      if (paramInfo.getType().equals(IpcdProtocol.PARAM_TYPE_NUMBER)) {
         Double d = null;
         if (!StringUtils.isEmpty(value)) {
            try {
               d = Double.valueOf(value);
            } catch (Exception ex) {
               d = null;
            }
         }
   
         return d != null ? adjustUnits(key, d, units) : null;
      }
      else {
         return value;
      }
   }
}

