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
package com.iris.common.rule.trigger;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.common.rule.trigger.ThresholdTrigger.TriggerOn;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

public class TestThresholdTrigger extends Assert {
   SimpleContext context;
   Model model;
   ThresholdTrigger trigger;
   Predicate<Model> selector, filter;
   ModelPredicateMatcher matcher;
   AttributeValueChangedEvent valueChangeEvent;
   private String attributeName;
   private double thresholdValue;
   private double sensitivityValue;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestThresholdTrigger.class));
      model = context.createModel("test", UUID.randomUUID());
      //selector = EasyMock.createMock(Predicate.class);
      //filter = EasyMock.createMock(Predicate.class);
      //matcher = new ModelPredicateMatcher(selector, filter);     
      //valueChangeEvent = AttributeValueChangedEvent.create(model.getAddress(), "test:test", "newValue", "oldValue");
   }
   
   private AttributeValueChangedEvent createValueChange(String attributeName, double oldValue, double newValue) {
      /*model.setAttribute(attributeName, oldValue);
      return AttributeValueChangedEvent.create(model.getAddress(), attributeName, newValue, oldValue);*/
	   return createValueChange(model, attributeName, oldValue, newValue);
   }
   private AttributeValueChangedEvent createValueChange(Model curModel, String attributeName, double oldValue, double newValue) {
   curModel.setAttribute(attributeName, oldValue);
      return AttributeValueChangedEvent.create(curModel.getAddress(), attributeName, newValue, oldValue);
   }
   
   
   protected void replay() {
      EasyMock.replay(selector, filter);
   }
   
   protected void verify() {
      EasyMock.verify(selector, filter);
   }
   
   @Test
   public void testSatisfiable() {
	   Model model2 = context.createModel("test", UUID.randomUUID());
	   attributeName = "test:test";
	   thresholdValue = 85.0;
	   sensitivityValue = 5.0;
	   Predicate<Address> source =  Predicates.equalTo(model.getAddress());
	   trigger = new ThresholdTrigger(attributeName, thresholdValue, sensitivityValue, TriggerOn.GREATER_THAN, source);
	 
      trigger.activate(context);
      assertFalse(trigger.isSatisfiable(context));            
      
      model.setAttribute(attributeName, new Double(80));
      assertTrue(trigger.isSatisfiable(context));
      
      //valueChangeEvent = AttributeValueChangedEvent.create(model.getAddress(), attributeName, new Double(86.0), new Double(84.0));
      assertTrue(trigger.shouldFire(context, createValueChange(attributeName, new Double(80), new Double(86))));      
      assertFalse(trigger.shouldFire(context, createValueChange("test:bad", new Double(80), new Double(86))));  
      //source does not match
      assertFalse(trigger.shouldFire(context, createValueChange(model2, attributeName, new Double(80), new Double(86))));
   }
   
   @Test
   public void testMultiples() {
	   Model model2 = context.createModel("test", UUID.randomUUID());
	   attributeName = "test:test";
	   thresholdValue = 85.0;
	   sensitivityValue = 5.0;
	   Predicate<Address> source =  Predicates.equalTo(model.getAddress());
	   trigger = new ThresholdTrigger(attributeName, thresholdValue, sensitivityValue, TriggerOn.GREATER_THAN, source);
	 
      trigger.activate(context);
      
      //valueChangeEvent = AttributeValueChangedEvent.create(model.getAddress(), attributeName, new Double(86.0), new Double(84.0));
      assertTrue(trigger.shouldFire(context, createValueChange(attributeName, new Double(80), new Double(86))));      
      assertFalse(trigger.shouldFire(context, createValueChange(model2, attributeName, new Double(80), new Double(86))));
      
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, new Double(86), new Double(90))));  
      assertFalse(trigger.shouldFire(context, createValueChange(model2, attributeName, new Double(86), new Double(90))));
      
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, new Double(90), new Double(70))));
      assertFalse(trigger.shouldFire(context, createValueChange(model2, attributeName, new Double(90), new Double(70))));
      
      assertFalse(trigger.shouldFire(context, createValueChange(model2, attributeName, new Double(70), new Double(88))));
      assertTrue(trigger.shouldFire(context, createValueChange(attributeName, new Double(70), new Double(88))));
   }
   
   @Test
   public void testGreaterThan() {
	   attributeName = "test:test";
	   thresholdValue = 85.0;
	   sensitivityValue = 5.0;
	   Predicate<Address> source =  Predicates.equalTo(model.getAddress());
	   trigger = new ThresholdTrigger(attributeName, thresholdValue, sensitivityValue, TriggerOn.GREATER_THAN, source);	 
       trigger.activate(context);
       
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 65, 80)));  
       assertTrue(trigger.shouldFire(context, createValueChange(attributeName, 80, 86)));   
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 88, 86))); 
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 90, 88)));
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 88, 85)));
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 85, 84)));
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 84, 80)));
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 80, 79)));
       assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 79, 60)));
       
   }
   

   @Test
   public void testLessThan() {
	   attributeName = "test:test";
	   thresholdValue = 10.0;
	   sensitivityValue = 1.8;
	   Predicate<Address> source =  Predicates.equalTo(model.getAddress());
	   trigger = new ThresholdTrigger(attributeName, thresholdValue, sensitivityValue, TriggerOn.LESS_THAN, source);	 
      trigger.activate(context);
      
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 15, 11)));   
      assertTrue(trigger.shouldFire(context, createValueChange(attributeName, 11, 9.9))); 
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 9.9, 8.0)));
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 8.0, 10)));
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 10, 11.7)));
      assertFalse(trigger.shouldFire(context, createValueChange(attributeName, 11.7, 15)));
      
   }
}

