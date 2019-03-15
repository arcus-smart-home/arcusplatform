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
/**
 *
 */
package com.iris.common.rule.scenario;

import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.Actions;
import com.iris.common.rule.action.ForEachModelAction;
import com.iris.common.rule.action.SendAction;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.event.AttributeValueChangedEvent;
import com.iris.common.rule.filter.MatcherFilter;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;

/**
 * When a door is opened turn all the switches that are off to on
 */
public class TestWhenADoorIsOpened extends ScenarioTestCase {
   SimpleRule rule;
   Address doorModel = Address.platformDriverAddress(UUID.randomUUID());

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();

      // 'is opened'
      Condition trigger = new ValueChangeTrigger(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED, ContactCapability.CONTACT_OPENED);

      // 'a door'
      ModelPredicateMatcher matcher = new ModelPredicateMatcher(Predicates.hasTag("functionalType:door"), Predicates.hasTag("functionalType:door"));

      // 'a door is opened'
      Condition condition = new MatcherFilter(trigger, matcher);

      // 'turn on'
      Action turnOn = Actions.setValue(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);

      // 'all the switches that are off'
      Action turnOnAllSwitchesThatAreOff =
            new ForEachModelAction(turnOn, Predicates.attributeEquals(SwitchCapability.ATTR_STATE,  SwitchCapability.STATE_OFF), SendAction.VAR_TO);

      rule = new SimpleRule(context, condition, turnOnAllSwitchesThatAreOff, Address.platformService(UUID.randomUUID(), "rule", 1));
   }

   @Test
   public void testEmptyContext() {
      assertFalse(rule.isSatisfiable());
      rule.activate();
      assertNull(context.getMessages().poll());

      rule.execute(AttributeValueChangedEvent.create(doorModel, ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED, ContactCapability.CONTACT_CLOSED));
      assertNull(context.getMessages().poll());
   }

   @Test
   public void testDoorAndSwitchesOneOff() {
      rule.activate();

      Model door = addDevice(ContactCapability.NAMESPACE);
      door.setAttribute(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED);
      ((Collection<String>) door.getAttribute(Capability.ATTR_TAGS)).add("functionalType:door");

      Model switch1 = addDevice(SwitchCapability.NAMESPACE);
      switch1.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      Model switch2 = addDevice(SwitchCapability.NAMESPACE);
      switch2.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      Model switch3 = addDevice(SwitchCapability.NAMESPACE);
      switch3.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);

      assertTrue(rule.isSatisfiable());
      rule.execute(AttributeValueChangedEvent.create(door.getAddress(), ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED, ContactCapability.CONTACT_CLOSED));

      PlatformMessage pm = context.getMessages().poll();
      assertNotNull(pm);
      assertEquals(switch1.getAddress(), pm.getDestination());
      assertEquals(Capability.CMD_SET_ATTRIBUTES, pm.getMessageType());
      assertEquals(SwitchCapability.STATE_ON, pm.getValue().getAttributes().get(SwitchCapability.ATTR_STATE));

      assertNull(context.getMessages().poll());
   }

   @Test
   public void testDoorAndSwitchesAllOff() {
      rule.activate();

      Model door = addDevice(ContactCapability.NAMESPACE);
      door.setAttribute(ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED);
      ((Collection<String>) door.getAttribute(Capability.ATTR_TAGS)).add("functionalType:door");

      Model switch1 = addDevice(SwitchCapability.NAMESPACE);
      switch1.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      Model switch2 = addDevice(SwitchCapability.NAMESPACE);
      switch2.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      Model switch3 = addDevice(SwitchCapability.NAMESPACE);
      switch3.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);

      assertTrue(rule.isSatisfiable());
      rule.execute(AttributeValueChangedEvent.create(door.getAddress(), ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED, ContactCapability.CONTACT_CLOSED));

      for(int i=0; i<3; i++) {
         PlatformMessage pm = context.getMessages().poll();
         assertNotNull("Message " + i + " was null", pm);

      }
      assertNull(context.getMessages().poll());
   }

}

