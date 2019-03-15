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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.care.CareErrors;
import com.iris.common.subsystem.care.behavior.BehaviorCatalog.BehaviorCatalogTemplate;
import com.iris.common.subsystem.care.behavior.BehaviorCatalog.BehaviorOption;
import com.iris.common.subsystem.care.behavior.evaluators.BehaviorEvaluator;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.CareBehaviorTemplate;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.util.TypeMarker;

public class BehaviorManager {
   private final BehaviorCatalog catalog = new BehaviorCatalog();

   public BehaviorManager() {
      catalog.init();
   }

   public void bind(SubsystemContext<CareSubsystemModel> context) {
      context.model().setActiveBehaviors(ImmutableSet.<String> of());
   }

   public CareBehaviorTypeWrapper getBehaviorFromContext(String id, SubsystemContext<CareSubsystemModel> context) {
      Map<String, Object> behaviorInfo = context.getVariable(BehaviorMonitor.BEHAVIOR_KEY.create(id)).as(TypeMarker.mapOf(String.class, Object.class));
      if (behaviorInfo == null) {
         context.logger().error("beahvior with id {} not found", id);
         removeBehavior(id, context);
         return null;
      }
      return new CareBehaviorTypeWrapper(behaviorInfo);
   }

   public List<CareBehaviorTypeWrapper> listCareBehaviors(SubsystemContext<CareSubsystemModel> context) {
      List<CareBehaviorTypeWrapper> behaviors = new ArrayList<CareBehaviorTypeWrapper>(context.model().getBehaviors().size());
      for (String behaviorId : CareSubsystemModel.getBehaviors(context.model(), new HashSet<String>())){
         CareBehaviorTypeWrapper behavior = getBehaviorFromContext(behaviorId, context);
         BehaviorCatalogTemplate catalogTemplate = catalog.getBehaviorCatalogTemplate(behavior.getTemplateId());
         behavior.setAvailbleDevices(new ArrayList<>(getAvailableDevices(catalogTemplate, context)));
         behaviors.add(behavior);
      }
      return behaviors;
   }

   public List<CareBehaviorTemplate> listCareBehaviorTemplates(SubsystemContext<CareSubsystemModel> context) {
      List<CareBehaviorTemplate> clientTemplates = new ArrayList<CareBehaviorTemplate>();
      for (BehaviorCatalogTemplate catalogTemplate : catalog.getBehavior()){
         CareBehaviorTemplate template = new CareBehaviorTemplate();
         template.setName(catalogTemplate.getName());
         template.setDescription(catalogTemplate.getDescription());
         template.setId(catalogTemplate.getId());
         template.setType(catalogTemplate.getType());
         template.setAvailableDevices(getAvailableDevices(catalogTemplate, context));
         template.setTimeWindowSupport(catalogTemplate.getTimeWindowSupport());
         loadOptions(catalogTemplate.getOption(), template);
         clientTemplates.add(template);
      }
      return clientTemplates;
   }

   public void loadOptions(List<BehaviorOption> options, CareBehaviorTemplate template) {
      if (options == null) {
         return;
      }
      Map<String, String> labels = new HashMap<>();
      Map<String, String> descrptions = new HashMap<>();
      Map<String, String> units = new HashMap<>();
      Map<String, String> values = new HashMap<>();

      for (BehaviorOption option : options){
         labels.put(option.getName(), option.getLabel());
         descrptions.put(option.getName(), option.getDescription());
         units.put(option.getName(), option.getUnit());
         values.put(option.getName(), option.getValues());
      }
      template.setFieldLabels(labels);
      template.setFieldDescriptions(descrptions);
      template.setFieldUnits(units);
      template.setFieldValues(values);
   }

   public void updateBehavior(CareBehaviorTypeWrapper behavior, SubsystemContext<CareSubsystemModel> context) {
      setBehavior(behavior, context);
      broadcastBehaviorEvent(behavior, context, CareSubsystemCapability.BehaviorActionEvent.BEHAVIORACTION_MODIFIED);
   }

   private Set<String> getAvailableDevices(BehaviorCatalogTemplate catalogTemplate, SubsystemContext<CareSubsystemModel> context) {
      if (catalogTemplate == null) {
         return ImmutableSet.of();
      }
      Set<String> availableDevices = new HashSet<String>();
      for (Model model : context.models().getModels(ExpressionCompiler.compile(catalogTemplate.getDeviceSelectorQuery()))){
         availableDevices.add(model.getAddress().getRepresentation());
      }
      return availableDevices;

   }

   public boolean removeBehavior(String id, SubsystemContext<CareSubsystemModel> context) {
      CareBehaviorTypeWrapper behavior = getBehaviorFromContext(id, context);
      BehaviorMonitor.removeBehavior(id, context);
      context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(id), null);
      BehaviorMonitor.clearBehaviorTimeouts(id, context);
      BehaviorUtil.removeStringFromSet(id, CareSubsystemCapability.ATTR_BEHAVIORS, context.model());
      BehaviorUtil.removeStringFromSet(id, CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, context.model());
      broadcastBehaviorEvent(behavior, context, CareSubsystemCapability.BehaviorActionEvent.BEHAVIORACTION_DELETED);
      return true;
   }

   public String addBehavior(CareBehaviorTypeWrapper behavior, SubsystemContext<CareSubsystemModel> context) {
      if (behaviorNameExists(behavior.getName(), context)) {
         throw new ErrorEventException(CareErrors.duplicateName(behavior.getName()));
      }
      validateTimeWindows(context, behavior);
      validateBehaviorType(behavior, context);
      behavior.setId(UUID.randomUUID().toString());
      context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behavior.getId()), behavior.toMap());
      BehaviorUtil.addStringToSet(behavior.getId(), CareSubsystemCapability.ATTR_BEHAVIORS, context.model());
      BehaviorMonitor.initBehavior(behavior.getId(), context);
      broadcastBehaviorEvent(behavior, context, CareSubsystemCapability.BehaviorActionEvent.BEHAVIORACTION_ADDED);
      return behavior.getId();
   }
   private void broadcastBehaviorEvent(CareBehaviorTypeWrapper behavior,SubsystemContext<CareSubsystemModel> context,String action){
      context.broadcast(CareSubsystemCapability.BehaviorActionEvent.builder()
            .withBehaviorId(behavior.getId())
            .withBehaviorName(behavior.getName())
            .withBehaviorAction(action).build());
   }
   private void validateTimeWindows(SubsystemContext<CareSubsystemModel> context, CareBehaviorTypeWrapper behavior) {
      if (behavior.getTimeWindows() == null || behavior.getTimeWindows().isEmpty()) {
         return;
      }
      List<WeeklyTimeWindow> windows = WeeklyTimeWindow.fromTimeWindowDataListSorted(behavior.getTimeWindows(), context.getLocalTime());
      Date lastestEndDate = null;
      for (WeeklyTimeWindow wtw : windows){
         Date startDate = wtw.nextOrCurrentStartDate(context.getLocalTime().getTime(), context.getLocalTime().getTimeZone());
         Date endDate = wtw.calculateEndDate(startDate);
         if (lastestEndDate != null && startDate.before(lastestEndDate)) {
            throw new ErrorEventException(CareErrors.duplicateTimeWindows(wtw.toString()));
         }
         lastestEndDate = endDate;
      }
   }

   public boolean behaviorNameExists(String name, SubsystemContext<CareSubsystemModel> context) {
      return loadBehaviorByName(name, context) != null;
   }

   public boolean behaviorNameExists(CareBehaviorTypeWrapper behaviorWrapper, SubsystemContext<CareSubsystemModel> context) {
      CareBehaviorTypeWrapper found = loadBehaviorByName(behaviorWrapper.getName(), context);
      return found != null && !found.getId().equals(behaviorWrapper.getId());
   }

   public CareBehaviorTypeWrapper loadBehaviorByName(String name, SubsystemContext<CareSubsystemModel> context) {
      for (String behaviorId : context.model().getBehaviors()){
         CareBehaviorTypeWrapper behavior = getBehaviorFromContext(behaviorId, context);
         if (name.equalsIgnoreCase(behavior.getName())) {
            return behavior;
         }
      }
      return null;
   }

   private void setBehavior(CareBehaviorTypeWrapper behavior, SubsystemContext<CareSubsystemModel> context) {
      context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behavior.getId()), behavior.toMap());
      BehaviorMonitor.clearBehaviorTimeouts(behavior.getId(), context);
      BehaviorMonitor.initBehavior(behavior.getId(), context);
   }

   private void validateBehaviorType(CareBehaviorTypeWrapper behavior, SubsystemContext<CareSubsystemModel> context) {
      BehaviorEvaluator evaluator = BehaviorMonitor.loadBehaviorEvaluator(behavior, context);
      evaluator.validateConfig(context);
   }

}

