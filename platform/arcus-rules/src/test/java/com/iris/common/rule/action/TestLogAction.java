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
package com.iris.common.rule.action;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.iris.common.rule.action.LogAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;

/**
 * 
 */
public class TestLogAction extends Assert {
   Logger mockLogger;
   SimpleContext context;

   @Before
   public void setUp() throws Exception {
      this.mockLogger = EasyMock.createMock(Logger.class);
      EasyMock.expect(this.mockLogger.isInfoEnabled()).andReturn(true).anyTimes();
      this.context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), mockLogger);
   }
   
   protected void replay() {
      EasyMock.replay(mockLogger);
   }
   
   protected void verify() {
      EasyMock.verify(mockLogger);
   }
   
   @Test
   public void testStaticMessage() {
      String message = "test message";
      LogAction action = new LogAction(message);

      mockLogger.info(message);
      EasyMock.expectLastCall();
      replay();
      
      assertEquals("log", action.getName());
      assertEquals("log test message", action.getDescription());
      action.execute(context);
      
      verify();
   }

   @Test
   public void testSubstitutedMessage() {
      String messageTemplate = "var1: ${var1}, var2: ${var2}, notspecified: ${notspecified}, escaped: $${var1}";
      context.setVariable("var1", "var1");
      context.setVariable("var2", true);
      
      String expected = "var1: var1, var2: true, notspecified: ${notspecified}, escaped: ${var1}";
      LogAction action = new LogAction(messageTemplate);

      mockLogger.info(expected);
      EasyMock.expectLastCall();
      replay();
      
      assertEquals("log", action.getName());
      assertEquals("log " + messageTemplate, action.getDescription());
      action.execute(context);
      
      verify();
   }

   @Test
   public void testStaticMessageFromContext() {
      String message = "test message";
      context.setVariable(LogAction.VAR_MESSAGE, message);
      
      LogAction action = new LogAction();

      mockLogger.info(message);
      EasyMock.expectLastCall();
      replay();
      
      assertEquals("log", action.getName());
      assertEquals("log ${message}", action.getDescription());
      action.execute(context);
      
      verify();
   }

   @Test
   public void testSubstitutedMessageFromContext() {
      String messageTemplate = "var1: ${var1}, var2: ${var2}, notspecified: ${notspecified}, escaped: $${var1}";
      context.setVariable(LogAction.VAR_MESSAGE, messageTemplate);
      context.setVariable("var1", "var1");
      context.setVariable("var2", true);
      
      String expected = "var1: var1, var2: true, notspecified: ${notspecified}, escaped: ${var1}";
      LogAction action = new LogAction();

      mockLogger.info(expected);
      EasyMock.expectLastCall();
      replay();
      
      assertEquals("log", action.getName());
      assertEquals("log ${message}", action.getDescription());
      action.execute(context);
      
      verify();
   }

   @Test
   public void testMessageMissingFromContext() {
      LogAction action = new LogAction();

      mockLogger.warn(EasyMock.isA(String.class), EasyMock.eq(context));
      EasyMock.expectLastCall();
      replay();
      
      assertEquals("log", action.getName());
      assertEquals("log ${message}", action.getDescription());
      action.execute(context);
      
      verify();
   }

}

