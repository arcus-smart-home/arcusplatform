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
package com.iris.common.rule.event;

import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.stateful.TestForEachModelAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;

public class TestScheduledReference extends Assert {
   
   ScheduledReference reference = null;
   RuleContext context = null; 
   Address source;
   Date timeoutAt;
   
   @Before
   public void setUp(){
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestForEachModelAction.class));
      timeoutAt = new Date(context.getLocalTime().getTimeInMillis()+2000);

   }
   
   @Test
   public void testScheduledReference() {
      reference = new ScheduledReference("testref");
      reference.setTimeout(context,timeoutAt);
      Date timeout = (Date)context.getVariable("testref");
      assertEquals(timeoutAt.getTime(), timeout.getTime());
      
      assertTrue(reference.hasScheduled(context));
      
      ScheduledEvent event = new ScheduledEvent(timeoutAt.getTime());
      assertTrue(reference.isReferencedEvent(context, event));
      reference.cancel(context);
      assertFalse(reference.isReferencedEvent(context, event));
      assertFalse(reference.hasScheduled(context));
   }
   
   @Test
   public void testRestore() {
      reference = new ScheduledReference("testref");
      assertFalse(reference.hasScheduled(context));
      context.setVariable("testref", timeoutAt);
      reference.restore(context);
      assertTrue(reference.hasScheduled(context));
   }
}

