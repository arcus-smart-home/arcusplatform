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
package com.iris.platform.rule.catalog.action.config;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.iris.common.rule.Context;
import com.iris.common.rule.action.ActionContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.util.IrisFunctions;

public class SendNotificationActionConfig extends SendActionConfig {
   public static final String TYPE = "send-notification";
   
   private List<ParameterConfig> parameterConfigs;

   public SendNotificationActionConfig(Set<String> availableContextVariables, String sendActionType) {
      super(availableContextVariables, sendActionType);
   }

   public SendNotificationActionConfig(String sendActionType) {
      super(sendActionType);
   }
   
   public List<ParameterConfig> getParameterConfigs() {
      if(parameterConfigs==null){
         parameterConfigs=new ArrayList<ParameterConfig>();
      }
      return parameterConfigs;
   }
   
   public void setParameterConfigs(List<ParameterConfig> parameterConfigs) {
      this.parameterConfigs = parameterConfigs;
   }
   
   @Override
   public String getType() {
      return TYPE;
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.action.config.SendActionConfig#generateDynamicAttributes()
    */
   @Override
   protected Map<String, Function<ActionContext, Object>> generateDynamicAttributes() {
      Map<String, Function<ActionContext, Object>> attributes = super.generateDynamicAttributes();
      attributes.put(NotificationCapability.NotifyRequest.ATTR_MSGPARAMS, generateMessageParams());
      return attributes;
   }

   private Function<ActionContext, Object> generateMessageParams() {
      if(getParameterConfigs() == null || getParameterConfigs().isEmpty()) {
         return null;
      }
      Map<String, Function<Context, String>> dynamicAttributes = new HashMap<String, Function<Context, String>>();
      for(ParameterConfig config:getParameterConfigs()){
         if(config.getType().equals(ParameterConfig.ParameterType.ATTRIBUTEVALUE)){
            Function<Context,String>getAttributFunction=
                  FunctionFactory.INSTANCE.createGetAttribute(String.class,
                        Address.fromString(config.getAddress()),
                        config.getAttributeName());
            dynamicAttributes.put(config.getName(), getAttributFunction);
         }
         if(config.getType().equals(ParameterConfig.ParameterType.CONSTANT)){
            Function<Context,String>constant=
                  FunctionFactory.INSTANCE.createConstant(Context.class,
                        config.getValue());
            dynamicAttributes.put(config.getName(), constant);
         }
         if(config.getType().equals(ParameterConfig.ParameterType.DATETIME)){
            DateFormat df = config.getDateType() == ParameterConfig.DateType.DATE
                  ? DateFormat.getDateInstance() 
                  : (config.getDateType() == ParameterConfig.DateType.TIME ? DateFormat.getTimeInstance() : DateFormat.getDateTimeInstance());
                  
            Function<Context,String>datetime=
                  FunctionFactory.INSTANCE.createCurrentTimeFormatted(df);
            dynamicAttributes.put(config.getName(), datetime);
         }

         
      }
      return new MapTransform(dynamicAttributes);
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result
            + ((parameterConfigs == null) ? 0 : parameterConfigs.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      SendNotificationActionConfig other = (SendNotificationActionConfig) obj;
      if (parameterConfigs == null) {
         if (other.parameterConfigs != null) return false;
      }
      else if (!parameterConfigs.equals(other.parameterConfigs)) return false;
      return true;
   }

   private static class MapTransform implements Function<ActionContext, Object>, Serializable {
      final Map<String, Function<Context, String>> inputMap; 
      
      MapTransform(Map<String, Function<Context, String>> inputMap) {
         this.inputMap = inputMap;
      }

      @Override
      public Object apply(ActionContext input) {
         return IrisFunctions.apply(inputMap, input);
      }
   }

}

