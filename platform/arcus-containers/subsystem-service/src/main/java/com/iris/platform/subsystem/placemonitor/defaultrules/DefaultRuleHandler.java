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
package com.iris.platform.subsystem.placemonitor.defaultrules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.platform.rule.catalog.template.TemplatedValue;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;
import com.iris.platform.subsystem.placemonitor.config.XMLUtil;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class DefaultRuleHandler extends BasePlaceMonitorHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRuleHandler.class);
   
   @Inject(optional = true)
   @Named(value = "product.default.rules.path")
   private String deviceRulesFile = "conf/product-default-rules.xml";

   private AtomicReference<ProductDefaultRules> defaults = new AtomicReference<ProductDefaultRules>(new ProductDefaultRules());

   @Inject
   public DefaultRuleHandler() {
   }
   
   public DefaultRuleHandler(String devicesRulesFile) {
      this.deviceRulesFile = devicesRulesFile;
   }
   
   @PostConstruct
   public void init() {
      Resource resource = Resources.getResource(deviceRulesFile);
      if(resource.isWatchable()){
         resource.addWatch(() -> {
            reloadDefaultRules();
         });
      }
      reloadDefaultRules();
   }

   @Override
   public void onAdded(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.model().setDefaultRulesDevices(ImmutableSet.<String>of());
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      setIfNull(context.model(), PlaceMonitorSubsystemCapability.ATTR_DEFAULTRULESDEVICES, ImmutableSet.<String>of());
      
      Set<String> devices = context.model().getDefaultRulesDevices();
      
      Iterable<Model> existingModels = context.models().getModelsByType(DeviceCapability.NAMESPACE);
      
      /*
       * Historical Cleanup, There may have already been some rules in DefaultRulesDevices  
       * which should have been removed but were left in over time. 
       */
      cleanupDefaultRuleDevices(existingModels, devices, context);
      
      // add in all devices that are not already in DefaultRulesDevices
      for(Model model: existingModels){
         if(!devices.contains(model.getAddress().getRepresentation())){
            addDefaultRulesForDevice(model,context);
         }
      }
      
   }

   private void reloadDefaultRules(){
      LOGGER.info("Loading Default Rules");
      defaults.set(XMLUtil.deserializeJAXB(Resources.getResource(deviceRulesFile), ProductDefaultRules.class));
   }

   @Override
   public void onDeviceAdded(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("onDeviceAdded Default Rules Device [{}]", DeviceModel.getProductId(model));
      addDefaultRulesForDevice(model,context);
   }
   
   
   @Override
   public void onDeviceRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      removeAddressFromSet(model.getAddress().getRepresentation(), PlaceMonitorSubsystemCapability.ATTR_DEFAULTRULESDEVICES, context.model(), context);
   }

   protected boolean removeAddressFromSet(String address, String attribute, Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      Set<String> newDefaultDevices = new HashSet<>(context.model().getDefaultRulesDevices());
      newDefaultDevices.remove(address);
      model.setAttribute(attribute, newDefaultDevices);
      return true;
   } 
   
   private void cleanupDefaultRuleDevices(Iterable<Model> existingModels, Set<String> defaultRuleDevices, SubsystemContext<PlaceMonitorSubsystemModel> context) {
   // get a set of all the addresses for devices in the system
      Set<String> existingDevices = new HashSet<String>();
      for (Model device : existingModels){
         existingDevices.add(device.getAddress().getRepresentation());
      }

      /* Filter out any devices in defaultRuleDevices 
       * that don't also exist in existingDevices.
       * This intersection will only contain defaultRuleDevices
       * that are still existing in the system
       */
      context.model().setAttribute( PlaceMonitorSubsystemCapability.ATTR_DEFAULTRULESDEVICES, defaultRuleDevices.stream().filter(existingDevices::contains).collect(Collectors.toSet()));     
   }
   
   private void addDefaultRulesForDevice(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context){
      String productId = DeviceModel.getProductId(model);
      List<RuleSet> sets = resolveRuleSetsForProduct(productId);
      for (RuleSet ruleSet : sets){
         addRuleSet(ruleSet, context, model);
      }
      Set<String>devices = new HashSet<String>(context.model().getDefaultRulesDevices());
      devices.add(model.getAddress().getRepresentation());
      context.model().setDefaultRulesDevices(devices);
   }

   private void addRuleSet(RuleSet ruleSet, SubsystemContext<PlaceMonitorSubsystemModel> context, Model model) {
      for (Rule rule : ruleSet.getRule()){
         addRule(rule, context, model);
      }
   }

   private void addRule(Rule rule, SubsystemContext<PlaceMonitorSubsystemModel> context, Model model) {
      Map<String, Object> contextVariables = new HashMap<String, Object>();
      for (Context ruleContext : rule.getContext()){
         TemplatedValue<Object> o = TemplatedValue.parse(ruleContext.getValue());
         contextVariables.put(ruleContext.getName(), o.apply(model.toMap()));
      }
      TemplatedValue<Object> o = TemplatedValue.parse(rule.getName());
      MessageBody body = RuleTemplateCapability.CreateRuleRequest.builder()
            .withPlaceId(context.getPlaceId().toString())
            .withName(o.apply(model.toMap()).toString())
            .withContext(contextVariables).build();
      context.request(Address.platformService(rule.getTeplate(), RuleTemplateCapability.NAMESPACE), body);
   }

   private List<RuleSet> resolveRuleSetsForProduct(String productId) {
      List<RuleSet> ruleSets = new ArrayList<RuleSet>();
      for (RuleSet set : defaults.get().getRuleSet()){
         if(set.getProductId().equals(productId)){
            ruleSets.add(set);
         }
      }
      return ruleSets;
   }
}

