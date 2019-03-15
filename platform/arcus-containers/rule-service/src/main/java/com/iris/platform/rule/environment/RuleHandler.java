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
package com.iris.platform.rule.environment;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.Rule;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;

/**
 * Responsible for handling events to a single rule.  This
 * helps to bind the rule and the definition together.
 *  
 */
// TODO this should probably be an interface
// TODO set attributes should be handled here as well
public class RuleHandler implements PlaceEventHandler {
   private static final Logger logger = LoggerFactory.getLogger(RuleHandler.class);
   
   private Rule rule;
   // TODO just use the rule from the model store...
   private RuleDefinition definition;
   private RuleDao ruleDao;
   
   // last known state of satisfiability
   private boolean satisfiable;
   private boolean premium;
   
   public RuleHandler(Rule rule, RuleDefinition definition, RuleDao ruleDao, boolean premium) {
      this.rule = rule;
      this.definition = definition;
      this.ruleDao = ruleDao;
      this.premium = premium;
   }
   
   public boolean isDeleted() {
      return false;
   }
   
   public RuleContext getContext() {
      return rule.getContext();
   }

   public Address getAddress() {
      return rule.getAddress();
   }

   public boolean isAvailable() {
      return 
            rule != null &&
            // TODO would like to include this but will need more testing then I have time for now
//            rule.isSatisfiable() &&
            !definition.isSuspended() &&
            !definition.isDisabled() &&
            isAvailableForPlace();
   }
   
   public void start() {
      disableIfNotMeetingRequirements();
      if(isAvailable()) {
         rule.activate();
      }
      else {
         rule.deactivate();
      }
      syncOrReload();
   }
   
   public void stop() {
      //rule.deactivate();
      syncOrReload();
   }
   
   public void activate() {
      rule.activate();
      satisfiable = rule.isSatisfiable();
   }
   
   public void deactivate() {
      rule.deactivate();
   }

   public void enable() {
      if(!rule.isSatisfiable()) {
         throw new ErrorEventException(Errors.invalidRequest("Rule can't be enabled because it isn't satisfiable"));
      }
      
      if(!isAvailableForPlace() ) {
         throw new ErrorEventException(Errors.invalidRequest(String.format("Rule %s may only be enabled for premium accounts", definition.getRuleTemplate())));
      }
      
      // reload to make sure we don't write stale state
      setEnabled(true);
      activate();
   }
   
   public void disable() {
      setEnabled(false);
      deactivate();
   }
   
   public void onEvent(RuleEvent event) {
      try {
         if(isServiceLevelChange(event)) {
            disableIfNotMeetingRequirements();
         }
         
         boolean wasSatisfiable = this.satisfiable;
         if(rule.isSatisfiable()) {
            // TODO expose satisfiable on the rule definition
            if(!wasSatisfiable) {
               rule
                  .getContext()
                  .logger()
                  .debug("Rule {} became satisfiable because of event {}", rule.getAddress(), event);
               satisfiable = true;
               start();
            }
            rule.execute(event);
         } 
         else if(wasSatisfiable) {
            rule
               .getContext()
               .logger()
               .debug("Rule {} became unsatisfiable because of event {} -- disabling rule", rule.getAddress(), event);
            this.satisfiable = false;
            
            disable();
         }
         syncOrReload();
      } 
      catch(Exception e) {
         rule.getContext().logger().warn("Error dispatching [{}]", event, e);
      }
   }
   
   private void syncOrReload(){
      //TODO: need to catch the exception and reload the rule
      try{
         syncContextVariables();
      }
      catch(IllegalStateException e){
         rule.getContext().logger().warn("reloading rule because of error [{}]", e.getMessage());
         this.definition = ruleDao.findById(this.definition.getId());
      }
   }
   private void syncContextVariables(){
      if(rule.getContext().isDirty()){
         rule.getContext().logger().debug("Detected changes to rule variables. Syncing with database. variable/old value [{}]",rule.getContext().getDirtyVariables());
         Map<String,Object>variables=new HashMap<String,Object>();
         variables.putAll(rule.getContext().getVariables());
         ruleDao.updateVariables(definition.getId(),variables,definition.getModified());
         rule.getContext().clearDirty();
      }
   }
   public MessageBody handleRequest(PlatformMessage message) {
      switch(message.getMessageType()) {
      case RuleCapability.EnableRequest.NAME:
         enable();
         return RuleCapability.EnableResponse.instance();
      case RuleCapability.DisableRequest.NAME:
         disable();
         return RuleCapability.DisableResponse.instance();
      default:
         return Errors.unsupportedMessageType(message.getMessageType());
      }
   }
   
   private boolean isAvailableForPlace() {
      if (PlaceCapability.SERVICELEVEL_BASIC.equals(rule.getContext().getServiceLevel()) && premium) {
         return false;
      }

      // always serviceable for premium or above
      return true;
   }
   
   private boolean isServiceLevelChange(RuleEvent event) {
      return 
            event instanceof AttributeValueChangedEvent && 
            PlaceCapability.ATTR_SERVICELEVEL.equals(((AttributeValueChangedEvent) event).getAttributeName());
   }

   private void disableIfNotMeetingRequirements() {
      if(!definition.isDisabled() && !isAvailableForPlace()) {
         disable();
      }
   }
   
   private void setEnabled(boolean enabled) {
      RuleDefinition definition = ruleDao.findById(this.definition.getId());
      if(definition == null) {
         rule.getContext().logger().warn("Unable to load definition for rule [{}]", rule.getAddress());
         throw new ErrorEventException(Errors.genericError());
      }
      if(definition.isDisabled() != enabled) {
         rule.getContext().logger().debug("Rule already {}", enabled ? "enabled" : "disabled");
         return;
      }
      
      rule.getContext().logger().debug("{} rule", enabled ? "enabling" : "disabling");
      definition.setDisabled(!enabled);
      ruleDao.save(definition);
      this.definition = definition;

      MessageBody valueChange = MessageBody.buildMessage(
            Capability.EVENT_VALUE_CHANGE,
            ImmutableMap.of(RuleCapability.ATTR_STATE, enabled ? RuleCapability.STATE_ENABLED : RuleCapability.STATE_DISABLED)
      );
      rule.getContext().broadcast(valueChange);
   }

}

