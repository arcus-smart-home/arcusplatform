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
package com.iris.platform.scene.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.ActionList;
import com.iris.common.rule.action.ActionList.Builder;
import com.iris.common.rule.action.SendAction;
import com.iris.device.model.AttributeDefinition;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.FanModel;
import com.iris.messages.model.dev.SpaceHeaterModel;
import com.iris.messages.type.ActionSelector;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.model.type.AttributeType;
import com.iris.model.type.IntType;
import com.iris.model.type.LongType;
import com.iris.model.type.TimestampType;
import com.iris.platform.rule.catalog.template.StringValue;
import com.iris.platform.scene.catalog.serializer.ActionTemplateType;
import com.iris.platform.scene.catalog.serializer.DurationUnitType;
import com.iris.platform.scene.catalog.serializer.GroupType;
import com.iris.platform.scene.catalog.serializer.OptionType;
import com.iris.platform.scene.catalog.serializer.SelectorType;
import com.iris.platform.scene.catalog.serializer.SelectorTypeType;
import com.iris.platform.scene.catalog.serializer.SetAttributesType;


public class CatalogActionTemplateResolver extends BaseCatalogResolver{
   private static final Logger logger = LoggerFactory.getLogger(CatalogActionTemplateResolver.class);
   
   private static final String SECURITY_RESOLVER_KEY="security";
   private static final String CAMERA_RESOLVER_KEY="camera";
   private static final String THEMOSTAT_RESOLVER_KEY="thermostat";
   private static final String BLIND_RESOLVER_KEY="blind";
   
   private final static Map<String,ActionResolver>staticResolvers=
         ImmutableMap.of(  SECURITY_RESOLVER_KEY, new SecurityAlarmResolver(),
                           THEMOSTAT_RESOLVER_KEY, new ThermostatResolver(),
                           CAMERA_RESOLVER_KEY, new CameraResolver(),
                           BLIND_RESOLVER_KEY, new ShadeResolver());
   
   private static final LoadingCache<String, Predicate<Model>> queryCache = CacheBuilder.newBuilder()
         .build(
                 new CacheLoader<String, Predicate<Model>>() {
                     @Override
                     public Predicate<Model> load(String query) throws Exception {
                         return ExpressionCompiler.compile(query);
                     }
                 }
         );
   
   private CapabilityRegistry registry;
   private ActionTemplateType actionTemplate;
   
   public CatalogActionTemplateResolver(CapabilityRegistry registry, ActionTemplateType actionTemplate){
      super(actionTemplate.getId(), actionTemplate.getName(), actionTemplate.getTypeHint(), actionTemplate.isPremium());
      this.registry = registry;
      this.actionTemplate=actionTemplate;
   }
   
   @Override
   public String getId() {
      return actionTemplate.getId();
   }
   
   @Override
   public List<ActionSelector> resolve(ActionContext context, Model model) {
      List<ActionSelector> actionSelectors = new ArrayList<ActionSelector>();
      for(SelectorType selector:actionTemplate.getSelector()){
         if(!matchesQuery(selector.getQuery(),model)) {
               continue;
          }
          List<ActionSelector> selecs = createSelectors(context, model, selector);
          actionSelectors.addAll(selecs);
      }
      return actionSelectors;
   }
   
   private boolean matchesQuery(String query,Model model){
      try{
         if(StringUtils.isEmpty(query)){
            return true;
         }
         return queryCache.get(query).apply(model);
      }catch (ExecutionException e){
         throw new RuntimeException(String.format("Error loading or evaluting query %s ",query),e);
      }
   }
   
   private List<ActionSelector> createSelectors(ActionContext context, Model model, SelectorType selectorType){
      List<ActionSelector>selectors = new ArrayList<ActionSelector>();
      SelectorTypeType type= selectorType.getType();
      switch(type){
         case GROUP:
            selectors.addAll(createGroupSelector(context,model,selectorType));
            break;
         case PERCENT:
            selectors.add(createPercentSelector(selectorType));
            break;
         case BOOLEAN:
            selectors.add(createBooleanSelector(selectorType));
            break;
         case LIST:
            selectors.add(createListSelector(selectorType,model));
            break;
         case THERMOSTAT:
            selectors.addAll(new ThermostatResolver().resolve(context, model));
            break;
         case DURATION:
            selectors.add(createDurationSelector(context,model,selectorType));
            break;
         case FAN:
            selectors.add(createFanSelector(selectorType,model));
            break;
         case RANGE:
             selectors.add(createRangeSelector(context,model,selectorType));
             break;
         case TEMPERATURE:
             selectors.add(createTemperatureSelector(context,model,selectorType));
             break;             
         default:
            break;
      }
      return selectors;
   }
   
   private ActionSelector createListSelector(SelectorType selectorType,Model model){
      ActionSelector actionSelector = new ActionSelector();
      List<List<Object>> values = new ArrayList<>();
      actionSelector.setType(ActionSelector.TYPE_LIST);
      actionSelector.setName(selectorType.getName());
      for(OptionType optionType:selectorType.getOptions().getOption()){
         values.add(ImmutableList.of(optionType.getLabel(),optionType.getValue()));
      }
      actionSelector.setValue(values);
      return actionSelector;
   }
   private ActionSelector createFanSelector(SelectorType selectorType,Model model){
      ActionSelector actionSelector = new ActionSelector();
      List<List<Object>> values = new ArrayList<>();
      actionSelector.setType(ActionSelector.TYPE_LIST);
      actionSelector.setName(selectorType.getName());
      int fanMax = FanModel.getMaxSpeed(model);
      int fanLow = 1;
      int fanMed = (int)Math.round(fanMax/2.0);
      values.add(ImmutableList.of("LOW",fanLow));
      values.add(ImmutableList.of("MEDIUM",fanMed));
      values.add(ImmutableList.of("HIGH",fanMax));
      actionSelector.setValue(ImmutableList.copyOf(values));
      return actionSelector;
   }

   private ActionSelector createDurationSelector(ActionContext context, Model model,SelectorType selectorType){
      ActionSelector actionSelector = new ActionSelector();
      actionSelector.setName(selectorType.getName());
      actionSelector.setType(ActionSelector.TYPE_DURATION);
      actionSelector.setUnit(selectorType.getUnit()!=null?selectorType.getUnit().name():null);
      actionSelector.setMin(selectorType.getMin());
      actionSelector.setMax(selectorType.getMax());
      actionSelector.setStep(selectorType.getStep());
      return actionSelector;
   }
   
   private ActionSelector createRangeSelector(ActionContext context, Model model,SelectorType selectorType){
      ActionSelector actionSelector = new ActionSelector();
      actionSelector.setName(selectorType.getName());
      actionSelector.setType(ActionSelector.TYPE_RANGE);
      actionSelector.setUnit(selectorType.getUnit()!=null?selectorType.getUnit().name():null);
      actionSelector.setMin(selectorType.getMin());
      actionSelector.setMax(selectorType.getMax());
      actionSelector.setStep(selectorType.getStep());
      return actionSelector;
   }  
   
   private ActionSelector createTemperatureSelector(ActionContext context, Model model,SelectorType selectorType){
      ActionSelector actionSelector = new ActionSelector();
      actionSelector.setName(selectorType.getName());
      actionSelector.setType(ActionSelector.TYPE_TEMPERATURE);
      
      /*
       * Default to Celsius
       */
      actionSelector.setUnit(Optional.ofNullable(selectorType.getUnit()).orElse(DurationUnitType.C).name());
      
      /*
       * min and max setpoints are optional for the spaceheater model
       * if present however they are represented as a double value
       * Math.round accepts a double value to round it to the next whole number
       */
      long minSetpoint = Math.round(SpaceHeaterModel.getMinsetpoint(model, 10d));  // 10 Celsius = 50 Fahrenheit
      long maxSetPoint = Math.round(SpaceHeaterModel.getMaxsetpoint(model, 36d));  // 36 Celsius = 96.8 Fahrenheit
      
      // cast the resulting value to an int for the int slider in the UI which will 
      // handle the conversion from Celsius to Fahrenheit
      
      actionSelector.setMin((int)minSetpoint);
      actionSelector.setMax((int)maxSetPoint);    	  
      


      actionSelector.setStep(selectorType.getStep());
      return actionSelector;
   }  
   
   private ActionSelector createBooleanSelector(SelectorType selectorType){
      ActionSelector actionSelector = new ActionSelector();
      actionSelector.setName(selectorType.getName());
      actionSelector.setType(ActionSelector.TYPE_BOOLEAN);
      return actionSelector;
   }
   
   private ActionSelector createPercentSelector(SelectorType selectorType){
      ActionSelector actionSelector = new ActionSelector();
      actionSelector.setName(selectorType.getName());
      actionSelector.setType(ActionSelector.TYPE_PERCENT);
      return actionSelector;
   }
   
   private List<ActionSelector>createGroupSelector(ActionContext context, Model model,SelectorType selectorType){
      List<ActionSelector>selectors=new ArrayList<ActionSelector>();
      
      ActionSelector actionSelector = new ActionSelector();
      List<List<Object>> values = new ArrayList<>();
      actionSelector.setType(ActionSelector.TYPE_GROUP);
      actionSelector.setName(selectorType.getName());
      for(GroupType groupType:selectorType.getGroups().getGroup()){
         List<Object>groupValues=new ArrayList<>();
         groupValues.add(groupType.getValue());
         if(groupType.getSelector()!=null)
         {
            List<ActionSelector>subselectors =createSubSelectors(context,model,groupType);
            List<Object> subvalues=subselectors.stream().map(ActionSelector::toMap).collect(Collectors.toList());
            groupValues.add(subvalues);
         }
         values.add(groupValues);

      }
      actionSelector.setValue(values);
      selectors.add(actionSelector);
      return ImmutableList.<ActionSelector>copyOf(selectors);
   }
   
   private List<ActionSelector> createSubSelectors(ActionContext context, Model model, GroupType groupType){
      List<ActionSelector>selectors=new ArrayList<ActionSelector>();
      for(SelectorType selectorType:groupType.getSelector()){
         if(!matchesQuery(selectorType.getQuery(),model)){
            continue;
         }
         List<ActionSelector> actionSelector = createSelectors(context, model, selectorType);
         selectors.addAll(actionSelector);
      }
      return ImmutableList.<ActionSelector>copyOf(selectors);
   }

   @Override
   public Action generate(ActionContext context, Address target, Map<String, Object> variables) {
      Builder alb = new ActionList.Builder();
      Action action = delegateGenerateAction(context, target, variables);
      alb.addAction(action);
      context.logger().debug("generating action [{}] for {} {}",action,target,variables);
      return alb.build();
   }
   
   private Action delegateGenerateAction(ActionContext context, Address target, Map<String, Object> variables){
      String actionType= actionTemplate.getTypeHint()!=null?actionTemplate.getTypeHint():"";
      Action action = null;
      ActionResolver resolver=staticResolvers.get(actionType);
      if(resolver!=null){
         action=resolver.generate(context, target, variables);
      }
      else{
         action = generateGroupActions(context, target, variables);
      }
      return action;
   }
   
   private Action generateGroupActions(ActionContext context, Address target, Map<String, Object> variables){
      Builder actionList = new ActionList.Builder();
      for(SelectorType selector:actionTemplate.getSelector()){
         actionList.addAction(generateActionsForSelector(context,selector, target, variables));
      }
      return actionList.build();
   }
   
   private Action generateActionsForSelector(ActionContext context,SelectorType selector,Address target, Map<String, Object> variables){
      Builder actionList = new ActionList.Builder();
      Map<String,Object>attrs = new HashMap<>();
      if(selector.getGroups()!=null){
         String value = (String)variables.get(selector.getName());
         for(GroupType groupType:selector.getGroups().getGroup()){
            if(groupType.getValue().equals(value)){
               attrs.putAll(buildAttributesMap(selector,groupType.getSetAttributes(), target, variables));
               for(SelectorType nestedSelector:groupType.getSelector()){
                  if(matchesQuery(nestedSelector.getQuery(),context.getModelByAddress(target))){
                     attrs.putAll(buildAttributesMap(nestedSelector,nestedSelector.getSetAttributes(), target, variables));
                  }
               }
            }
         }
      }
      if(selector.getSetAttributes()!=null){
         attrs.putAll(buildAttributesMap(selector,selector.getSetAttributes(), target, variables));
      }
      actionList.addAction(generateSetAttributesAction(target, attrs));
      return actionList.build();
   }

   private Action generateSetAttributesAction(Address target, Map<String, Object> attributes){
         return new SendAction(Capability.CMD_SET_ATTRIBUTES, Functions.constant(target), attributes);
   }
   
   private Map<String,Object> buildAttributesMap(
         SelectorType selector, 
         List<SetAttributesType>setAttrs,
         Address target, 
         Map<String, Object> variables
   ){
      Map<String,Object> templateVariables = new HashMap<String, Object>(variables);
      Map<String,Object> attributesToSend = new HashMap<String,Object>();
      if(setAttrs !=null && !setAttrs.isEmpty()){
         for(SetAttributesType setAttributes:setAttrs){
            if(!StringUtils.isEmpty(selector.getVar())){
               templateVariables.put(selector.getVar(), variables.getOrDefault(selector.getName(), ""));
            }
            StringValue template = new StringValue(setAttributes.getValue());
            AttributeDefinition ad = registry.getAttributeDefinition(setAttributes.getName());
            Object value = template.apply(templateVariables);
            if(ad == null) {
               logger.warn("Unrecognized attributes [{}]", setAttributes.getName());
            }
            else {
               AttributeType type = ad.getAttributeType();
               // FIXME these types don't accept string encoded doubles and
               //       the template turns everything into a string
               if(
                     IntType.INSTANCE.equals(type) ||
                     LongType.INSTANCE.equals(type) ||
                     TimestampType.INSTANCE.equals(type)
               ) {
                  value = Double.parseDouble((String) value);
               }
               value = ad.getAttributeType().coerce(value);
            }
            attributesToSend.put(setAttributes.getName(), value);
         }
      }
      return attributesToSend;
   }
 
}

