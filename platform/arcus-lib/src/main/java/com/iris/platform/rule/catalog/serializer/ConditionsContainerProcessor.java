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
package com.iris.platform.rule.catalog.serializer;

import java.util.concurrent.TimeUnit;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.condition.config.DurationConfig;
import com.iris.platform.rule.catalog.condition.config.QueryChangeConfig;
import com.iris.platform.rule.catalog.condition.config.ThresholdConfig;
import com.iris.platform.rule.catalog.condition.config.TimeOfDayConfig;
import com.iris.platform.rule.catalog.condition.config.ValueChangeConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

public abstract class ConditionsContainerProcessor extends BaseCatalogProcessor {
   public static final String TAG_VALUE_CHANGE_TRIGGER = "attribute-value-change";
   public static final String TAG_QUERY_TRIGGER        = "query-change";
   public static final String TAG_TIME_OF_DAY          = "time-of-day";
   public static final String TAG_DURATION             = "duration-trigger";
   public static final String TAG_THRESHOLD_TRIGGER    = "attribute-value-threshold";
   public static final String TAG_OR                   = "or";

   private ConditionConfig filter;

   protected ConditionsContainerProcessor(Validator validator) {
      super(validator);
   }

   abstract public boolean isFilter();

   public ConditionConfig getCondition() {
      return filter;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.
    * lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if (FilterProcessor.TAG.equals(qName)) {
         return new FilterProcessor(getValidator());
      }
      if (DayOfWeekProcessor.TAG.equals(qName)) {
         return new DayOfWeekProcessor(getValidator());
      }
      if (ContextQueryProcessor.TAG.equals(qName)) {
         return new ContextQueryProcessor(getValidator());
      }
      if (IfConditionProcessor.TAG.equals(qName)) {
         return new IfConditionProcessor(getValidator());
      }
      if (ReceivedMessageProcessor.TAG.equals(qName)) {
         return new ReceivedMessageProcessor(getValidator());
      }
      if (OrConditionProcessor.TAG.equals(qName)) {
         return new OrConditionProcessor(getValidator());
      }
      return this;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.
    * lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if (TAG_VALUE_CHANGE_TRIGGER.equals(qName)) {
         addValueChangeTrigger(attributes);
      }

      else if (TAG_TIME_OF_DAY.equals(qName)) {
         addTimeOfDayTrigger(attributes);
      }

      else if (TAG_DURATION.equals(qName)) {
         // TODO add matchers
         addDurationCondition(attributes);
      } 
      
      else if (TAG_QUERY_TRIGGER.equals(qName)) {
         addQueryChangeTrigger(attributes);
      } 
      
      else if (TAG_THRESHOLD_TRIGGER.equals(qName)) {
    	  addThresholdCondition(attributes);
      }
      
   }

   

@Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if (FilterProcessor.TAG.equals(qName)) {
         setCondition(((FilterProcessor) handler).getCondition());
      } 
      else if (DayOfWeekProcessor.TAG.equals(qName)) {
         setCondition(((DayOfWeekProcessor) handler).getCondition());
      } 
      else if (ContextQueryProcessor.TAG.equals(qName)) {
         setCondition(((ContextQueryProcessor) handler).getCondition());
      } 
      else if (IfConditionProcessor.TAG.equals(qName)) {
         setCondition(((IfConditionProcessor) handler).getCondition());
      } 
      else if (ReceivedMessageProcessor.TAG.equals(qName)) {
         setCondition(((ReceivedMessageProcessor) handler).getCondition());
      }
      else if (OrConditionProcessor.TAG.equals(qName)) {
         setCondition(((OrConditionProcessor) handler).getCondition());
      }
   }
   
   protected void setCondition(ConditionConfig condition) {
      if (this.filter != null && condition != null) {
         validator.error("A " + (isFilter() ? "filter" : "conditions") + " tag can contain only one condition");
         return;
      }
      this.filter = condition;
   }

   private void addValueChangeTrigger(Attributes attributes) {
      TemplatedExpression attributeName = getTemplatedExpression("attribute", attributes);
      TemplatedExpression oldValue = getTemplatedExpression("old", null, attributes);
      TemplatedExpression newValue = getTemplatedExpression("new", null, attributes);
      TemplatedExpression query = getTemplatedExpression("query", null, attributes);

      ValueChangeConfig vc = new ValueChangeConfig();
      vc.setAttributeExpression(attributeName);
      vc.setNewValueExpression(newValue);
      vc.setOldValueExpression(oldValue);
      vc.setQueryExpression(query);
      setCondition(vc);
   }

   private void addQueryChangeTrigger(Attributes attributes) {

      TemplatedExpression deviceQuery = getTemplatedExpression("device-query", null, attributes);
      TemplatedExpression condition = getTemplatedExpression("condition", null, attributes);

      QueryChangeConfig qc = new QueryChangeConfig();
      qc.setQueryExpression(deviceQuery);
      qc.setConditionExpression(condition);
      setCondition(qc);
   }
      
   private void addTimeOfDayTrigger(Attributes attributes) {
      TemplatedExpression time = getTemplatedExpression("time", attributes);

      TimeOfDayConfig condition = new TimeOfDayConfig();
      condition.setTimeOfDayExpression(time);
      setCondition(condition);
   }

   private void addDurationCondition(Attributes attributes) {
      TemplatedExpression selector = getTemplatedExpression("device-query", attributes);
      TemplatedExpression matcher = getTemplatedExpression("condition", attributes);
      TemplatedExpression duration = getTemplatedExpression("duration", attributes);
      TimeUnit unit = getEnumValue("unit", TimeUnit.class, TimeUnit.SECONDS, attributes);

      DurationConfig condition = new DurationConfig();
      condition.setSelectorExpression(selector);
      condition.setMatcherExpression(matcher);
      condition.setDurationExpression(duration);
      condition.setUnit(unit);
      setCondition(condition);
   }
   
   
   private void addThresholdCondition(Attributes attributes) {
	  TemplatedExpression attribute = getTemplatedExpression("attribute", attributes);
      TemplatedExpression threshold = getTemplatedExpression("threshold", attributes);
      TemplatedExpression triggerWhen = getTemplatedExpression("trigger-when", attributes);
      TemplatedExpression sensitivity = getTemplatedExpression("sensitivity", null, attributes);
      TemplatedExpression sensitivityPercent = getTemplatedExpression("sensitivity-percent", null, attributes);
      TemplatedExpression source = getTemplatedExpression("source", attributes);
      

      ThresholdConfig condition = new ThresholdConfig();
      condition.setAttributeExpression(attribute);
      condition.setThresholdExpression(threshold);
      condition.setTriggerWhenExpression(triggerWhen);
      condition.setSensitivityExpression(sensitivity);
      condition.setSensitivityPercentExpression(sensitivityPercent);
      condition.setSourceExpression(source);
      setCondition(condition);
		
   }
}

