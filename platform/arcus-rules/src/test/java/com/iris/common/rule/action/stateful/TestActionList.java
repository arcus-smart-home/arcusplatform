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
package com.iris.common.rule.action.stateful;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.RuleContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.simple.NamespaceContext;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;

public class TestActionList {

   RuleContext context;
   Address source;

   @Before
   public void setUp() throws Exception {
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestDelayAction.class));
   }
   
   @Test
   public void testSingleActionReturnsImmediately() {
      StatefulAction action = EasyMock.createMock(StatefulAction.class);
      EasyMock
         .expect(action.execute(new NamespaceContext("0", context)))
         .andReturn(ActionState.IDLE);
      EasyMock.replay(action);
      
      SequentialActionList list = new SequentialActionList.Builder().addAction(action).build();
      assertEquals(ActionState.IDLE, list.execute(context));
      
      EasyMock.verify(action);
   }

   @Test
   public void testSingleActionTakesAwhile() {
      RuleEvent event1 = new ScheduledEvent();
      RuleEvent event2 = new ScheduledEvent();
      
      StatefulAction action = EasyMock.createMock(StatefulAction.class);
      EasyMock
         .expect(action.execute(new NamespaceContext("0", context)))
         .andReturn(ActionState.FIRING);
      EasyMock
         .expect(action.keepFiring(new NamespaceContext("0", context), event1, true))
         .andReturn(ActionState.FIRING);
      EasyMock
         .expect(action.keepFiring(new NamespaceContext("0", context), event2, true))

         .andReturn(ActionState.IDLE);
      
      EasyMock.replay(action);
      
      SequentialActionList list = new SequentialActionList.Builder().addAction(action).build();
      
      assertEquals(ActionState.FIRING, list.execute(context));
      assertEquals(ActionState.FIRING, list.keepFiring(context, event1, true));
      assertEquals(ActionState.IDLE, list.keepFiring(context, event1, true));
      
      EasyMock.verify(action);
   }

   @Test
   public void testChainedActionAllFireImmediately() {
      StatefulAction action1 = EasyMock.createMock(StatefulAction.class);
      StatefulAction action2 = EasyMock.createMock(StatefulAction.class);
      StatefulAction action3 = EasyMock.createMock(StatefulAction.class);
      EasyMock
         .expect(action1.execute(new NamespaceContext("0", context)))
         .andReturn(ActionState.IDLE);
      EasyMock
         .expect(action2.execute(new NamespaceContext("1", context)))
         .andReturn(ActionState.IDLE);
      EasyMock
         .expect(action3.execute(new NamespaceContext("2", context)))
         .andReturn(ActionState.IDLE);
      
      EasyMock.replay(action1, action2, action3);
      
      SequentialActionList list = 
            new SequentialActionList.Builder()
               .addAction(action1)
               .addAction(action2)
               .addAction(action3)
               .build();
      
      assertEquals(ActionState.IDLE, list.execute(context));
      
      EasyMock.verify(action1, action2, action3);
   }

   @Test
   public void testChainedActionAllWait() {
      RuleEvent event1 = new ScheduledEvent();
      RuleEvent event2 = new ScheduledEvent();
      RuleEvent event3 = new ScheduledEvent();

      StatefulAction action1 = EasyMock.createMock(StatefulAction.class);
      StatefulAction action2 = EasyMock.createMock(StatefulAction.class);
      StatefulAction action3 = EasyMock.createMock(StatefulAction.class);
      
      EasyMock
         .expect(action1.execute(new NamespaceContext("0", context)))
         .andReturn(ActionState.FIRING);
      EasyMock
         .expect(action1.keepFiring(new NamespaceContext("0", context), event1,true))
         .andReturn(ActionState.IDLE);
      EasyMock
         .expect(action2.execute(new NamespaceContext("1", context)))
         .andReturn(ActionState.FIRING);
      EasyMock
         .expect(action2.keepFiring(new NamespaceContext("1", context), event2,true))
         .andReturn(ActionState.IDLE);
      EasyMock
         .expect(action3.execute(new NamespaceContext("2", context)))
         .andReturn(ActionState.FIRING);
      EasyMock
         .expect(action3.keepFiring(new NamespaceContext("2", context), event3,true))
         .andReturn(ActionState.IDLE);
      
      EasyMock.replay(action1, action2, action3);
      
      SequentialActionList list = 
            new SequentialActionList.Builder()
               .addAction(action1)
               .addAction(action2)
               .addAction(action3)
               .build();
      
      assertEquals(ActionState.FIRING, list.execute(context));
      assertEquals(ActionState.FIRING, list.keepFiring(context, event1, true));
      assertEquals(ActionState.FIRING, list.keepFiring(context, event2, true));
      assertEquals(ActionState.IDLE, list.keepFiring(context, event3, true));
      
      EasyMock.verify(action1, action2, action3);
   }

}

