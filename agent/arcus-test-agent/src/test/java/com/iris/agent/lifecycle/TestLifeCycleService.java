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
package com.iris.agent.lifecycle;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.test.SystemTestCase;

@Ignore
@RunWith(JUnit4.class)
public class TestLifeCycleService extends SystemTestCase {
   @Test
   public void testLifeCycle() throws Exception {
      TestListener listener = new TestListener();
      LifeCycleService.addListener(listener);

      // Simulate initial state
      LifeCycleService.setState(LifeCycle.INITIAL);
      Assert.assertEquals(LifeCycle.INITIAL, LifeCycleService.getState());
      Assert.assertFalse(LifeCycleService.isConnected());

      // Simulated starting up
      LifeCycleService.setState(LifeCycle.STARTING_UP);
      Assert.assertEquals(LifeCycle.STARTING_UP, LifeCycleService.getState());
      Assert.assertFalse(LifeCycleService.isConnected());

      // Simulated started
      LifeCycleService.setState(LifeCycle.STARTED);
      Assert.assertEquals(LifeCycle.STARTED, LifeCycleService.getState());
      Assert.assertFalse(LifeCycleService.isConnected());

      // Simulated connecting
      LifeCycleService.setState(LifeCycle.CONNECTING);
      Assert.assertEquals(LifeCycle.CONNECTING, LifeCycleService.getState());
      Assert.assertFalse(LifeCycleService.isConnected());

      // Simulated connected
      LifeCycleService.setState(LifeCycle.CONNECTED);
      Assert.assertEquals(LifeCycle.CONNECTED, LifeCycleService.getState());
      Assert.assertTrue(LifeCycleService.isConnected());

      // Simulated authorized
      LifeCycleService.setState(LifeCycle.AUTHORIZED);
      Assert.assertEquals(LifeCycle.AUTHORIZED, LifeCycleService.getState());
      Assert.assertTrue(LifeCycleService.isConnected());

      // Stay in authorized, should not send a notificaton
      LifeCycleService.setState(LifeCycle.AUTHORIZED);
      Assert.assertEquals(LifeCycle.AUTHORIZED, LifeCycleService.getState());
      Assert.assertTrue(LifeCycleService.isConnected());

      // Simulated shutdown
      LifeCycleService.setState(LifeCycle.SHUTTING_DOWN);
      Assert.assertEquals(LifeCycle.SHUTTING_DOWN, LifeCycleService.getState());
      Assert.assertFalse(LifeCycleService.isConnected());

      // Expected notifications:
      //    * State changes to initial
      //    * State changes to starting up
      //    * State changes to stated
      //    * State changes to connecting
      //    * State changes to connected
      //    * State changes to authorized
      //    * State changes to shutting down
      Thread.sleep(2000);
      Assert.assertEquals(7, listener.changes.get());
   }

   private static final class TestListener extends AbstractLifeCycleListener {
      private AtomicLong changes = new AtomicLong(0);

      @Override
      public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
         changes.getAndIncrement();
         System.out.println("STATE CHANGE: " + oldState + " -> " + newState);
      }
   }
}

