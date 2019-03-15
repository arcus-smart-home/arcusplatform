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
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ForEachModelAction;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;

/**
 * 
 */
public class TestForEachModelAction extends Assert {

   String targetVariable = "address";
   
   SimpleContext context;
   Address source;
   Action mockAction;
   
   SimpleModel model1, model2, model3;

   @Before
   public void setUp() throws Exception {
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestForEachModelAction.class));
      this.model1 = createDeviceModel();
      this.model2 = createDeviceModel();
      this.model3 = createDeviceModel();
      
      this.context.putModel(this.model1);
      this.context.putModel(this.model2);
      this.context.putModel(this.model3);
      
      this.mockAction = EasyMock.createMock(Action.class);
   }

   SimpleModel createDeviceModel() {
      UUID id = UUID.randomUUID();
      String type = DeviceCapability.NAMESPACE;
      SimpleModel model = new SimpleModel();
      model.setAttribute(Capability.ATTR_ID, id.toString());
      model.setAttribute(Capability.ATTR_TYPE, type);
      model.setAttribute(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation());
      return model;
   }

   @Test
   public void testNoMatchingModels() {
      EasyMock.replay(mockAction);
      
      ForEachModelAction selector = new ForEachModelAction(mockAction, Predicates.<Model>alwaysFalse(), "address");
      selector.execute(context);
      
      EasyMock.verify(mockAction);
   }

   @Test
   public void testOneMatchingModel() {
      mockAction.execute(context);
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
         @Override
         public Object answer() throws Throwable {
            // verify that the address has been changed
            assertEquals(model1.getAddress(), context.getVariable(targetVariable));
            return null;
         }
      });
      
      EasyMock.replay(mockAction);
      
      ForEachModelAction selector = new ForEachModelAction(mockAction, Predicates.<Model>equalTo(model1), "address");
      selector.execute(context);
      assertNull(context.getVariable(targetVariable));
      
      EasyMock.verify(mockAction);
   }

   @Test
   public void testMultipleMatchingModels() {
      mockAction.execute(context);
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
         @Override
         public Object answer() throws Throwable {
            // verify that the address has been changed
            assertEquals(model1.getAddress(), context.getVariable(targetVariable));
            return null;
         }
      });
      mockAction.execute(context);
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
         @Override
         public Object answer() throws Throwable {
            // verify that the address has been changed
            assertEquals(model2.getAddress(), context.getVariable(targetVariable));
            return null;
         }
      });
      mockAction.execute(context);
      EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
         @Override
         public Object answer() throws Throwable {
            // verify that the address has been changed
            assertEquals(model3.getAddress(), context.getVariable(targetVariable));
            return null;
         }
      });
      
      EasyMock.replay(mockAction);
      
      ForEachModelAction selector = new ForEachModelAction(mockAction, Predicates.<Model>alwaysTrue(), "address");
      selector.execute(context);
      assertNull(context.getVariable(targetVariable));
      
      EasyMock.verify(mockAction);
   }

   @Test
   public void testActionThrows() {
      EasyMock.expect(mockAction.getDescription())
         .andReturn("mock action")
         .anyTimes();
      mockAction.execute(context);
      EasyMock.expectLastCall().andThrow(new RuntimeException("error")).times(3);
      
      EasyMock.replay(mockAction);
      
      ForEachModelAction selector = new ForEachModelAction(mockAction, Predicates.<Model>alwaysTrue(), "address");
      selector.execute(context);
      assertNull(context.getVariable(targetVariable));
      
      EasyMock.verify(mockAction);
   }

}

