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

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.action.stateful.ActionState;
import com.iris.common.rule.action.stateful.DelayAction;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;

public class TestDelayAction {

   SimpleContext context;
   Address source;

   @Before
   public void setUp() throws Exception {
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestDelayAction.class));
   }
   
   @Test
   public void testDelay() throws Exception {
      DelayAction delay = new DelayAction(1000);
      assertEquals(ActionState.FIRING, delay.execute(context));
      ScheduledEvent event = context.getEvents().poll();
      assertNotNull(event);
      
      // an immediate new ScheduledEvent shouldn't trigger the rule
      assertEquals(ActionState.FIRING, delay.keepFiring(context, new ScheduledEvent(), true));
      
      // the initial event should complete the action
      assertEquals(ActionState.IDLE, delay.keepFiring(context, event, true));
   }
}

