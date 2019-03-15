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
package com.iris.common.rule.action;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;

public class TestActionList extends Assert {

   SimpleContext context;
   Address source;

   @Before
   public void setUp() throws Exception {
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestActionList.class));
   }
   
   @Test
   public void testEmptyList() throws Exception {
      try {
         Actions.buildActionList().build();
         fail("Allowed empty list to be created");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }
   
   @Test
   public void testSingleEntry() throws Exception {
      Action firstAction = EasyMock.createMock(Action.class);
      EasyMock.expect(firstAction.getDescription()).andReturn("mock doing something").anyTimes();
      firstAction.execute(context);
      EasyMock.expectLastCall();
      EasyMock.replay(firstAction);
      
      ActionList action = 
            Actions
               .buildActionList()
               .addAction(firstAction)
               .build();
      
      assertEquals(ActionList.NAME, action.getName());
      assertEquals("first (mock doing something)", action.getDescription());
      action.execute(context);
      
      EasyMock.verify(firstAction);
   }

   @Test
   public void testSingleEntryThrowsException() throws Exception {
      Action firstAction = EasyMock.createMock(Action.class);
      EasyMock.expect(firstAction.getDescription()).andReturn("mock doing something").anyTimes();
      firstAction.execute(context);
      EasyMock.expectLastCall().andThrow(new RuntimeException("error"));
      EasyMock.replay(firstAction);
      
      ActionList action = 
            Actions
               .buildActionList()
               .addAction(firstAction)
               .build();
      
      assertEquals(ActionList.NAME, action.getName());
      action.execute(context);
      
      EasyMock.verify(firstAction);
   }

   @Test
   public void testMultiEntry() throws Exception {
      Action firstAction = EasyMock.createMock(Action.class);
      EasyMock.expect(firstAction.getDescription()).andReturn("mock something").anyTimes();
      firstAction.execute(context);
      EasyMock.expectLastCall();
      
      Action secondAction = EasyMock.createMock(Action.class);
      EasyMock.expect(secondAction.getDescription()).andReturn("mock something else").anyTimes();
      secondAction.execute(context);
      EasyMock.expectLastCall();
      
      EasyMock.replay(firstAction, secondAction);
      
      ActionList action = 
            Actions
               .buildActionList()
               .addAction(firstAction)
               .addAction(secondAction)
               .build();
      
      assertEquals(ActionList.NAME, action.getName());
      assertEquals("first (mock something) then (mock something else)", action.getDescription());
      action.execute(context);
      
      EasyMock.verify(firstAction, secondAction);
   }

   @Test
   public void testMultiEntryThrowsException() throws Exception {
      Action firstAction = EasyMock.createMock(Action.class);
      EasyMock.expect(firstAction.getDescription()).andReturn("mock something").anyTimes();
      firstAction.execute(context);
      EasyMock.expectLastCall().andThrow(new RuntimeException());
      
      Action secondAction = EasyMock.createMock(Action.class);
      EasyMock.expect(secondAction.getDescription()).andReturn("mock something else").anyTimes();
      secondAction.execute(context);
      EasyMock.expectLastCall().andThrow(new RuntimeException());
      
      EasyMock.replay(firstAction, secondAction);
      
      ActionList action = 
            Actions
               .buildActionList()
               .addAction(firstAction)
               .addAction(secondAction)
               .build();
      
      assertEquals(ActionList.NAME, action.getName());
      assertEquals("first (mock something) then (mock something else)", action.getDescription());
      action.execute(context);
      
      EasyMock.verify(firstAction, secondAction);
   }

   @Test
   public void testOverrideContext() throws Exception {
      Map<String, Object> firstVars = Collections.<String, Object>singletonMap("key", "first");
      Map<String, Object> secondVars = Collections.<String, Object>singletonMap("key", "second");
      Capture<ActionContext> firstContextRef = Capture.<ActionContext>newInstance();
      Capture<ActionContext> secondContextRef = Capture.<ActionContext>newInstance();
      
      Action firstAction = EasyMock.createMock(Action.class);
      EasyMock.expect(firstAction.getDescription()).andReturn("mock something").anyTimes();
      firstAction.execute(EasyMock.capture(firstContextRef));
      EasyMock.expectLastCall();
      
      Action secondAction = EasyMock.createMock(Action.class);
      EasyMock.expect(secondAction.getDescription()).andReturn("mock something else").anyTimes();
      secondAction.execute(EasyMock.capture(secondContextRef));
      EasyMock.expectLastCall();
      
      EasyMock.replay(firstAction, secondAction);
      
      ActionList action = 
            Actions
               .buildActionList()
               .addAction(firstAction, firstVars)
               .addAction(secondAction, secondVars)
               .build();
      
      assertEquals(ActionList.NAME, action.getName());
      assertEquals("first (mock something) then (mock something else)", action.getDescription());
      action.execute(context);
      
      assertEquals("first",  firstContextRef.getValue().getVariable("key"));
      assertEquals("second", secondContextRef.getValue().getVariable("key"));
      
      EasyMock.verify(firstAction, secondAction);
   }

   // TODO single entry with delay
   // TODO multi-entry with delay
}

