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
package com.iris.platform.rule;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionList;
import com.iris.common.rule.action.LogAction;
import com.iris.common.rule.action.SendAction;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * 
 */
@Modules({ PlatformRuleModule.class })
public class TestActionSerializer extends IrisTestCase {

   @Inject Serializer<Action> serializer;
   @Inject Deserializer<Action> deserializer;
   
   @Test
   public void testLogActionNoMessage() {
      LogAction expected = new LogAction();
      byte [] bytes = serializer.serialize(expected);
      System.out.println(new String(bytes));
      Action actual = deserializer.deserialize(bytes);
      
      assertTrue(actual instanceof LogAction);
      assertEquals(expected.getDescription(), ((LogAction) actual).getDescription());
   }

   @Test
   public void testLogActionWithMessage() {
      LogAction expected = new LogAction("Aloha world");
      byte [] bytes = serializer.serialize(expected);
      System.out.println(new String(bytes));
      Action actual = deserializer.deserialize(bytes);
      
      assertTrue(actual instanceof LogAction);
      assertEquals(expected.getDescription(), ((LogAction) actual).getDescription());
   }

   @Test
   public void testActionList() {
      LogAction action1 = new LogAction("Testing");
      SendAction action2 = new SendAction("test:Message", null, null);
      
      ActionList expected = new ActionList.Builder().addAction(action1).addAction(action2).build();
      byte [] bytes = serializer.serialize(expected);
      System.out.println(new String(bytes));
      Action actual = deserializer.deserialize(bytes);
      
      assertTrue(actual instanceof ActionList);
      assertEquals(expected.getDescription(), actual.getDescription());
   }

}

