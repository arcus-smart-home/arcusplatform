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
package com.iris.agent.db;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.test.SystemTestCase;

@RunWith(JUnit4.class)
public class DbServiceShutdownTest extends SystemTestCase {

   @Test
   public void shutdownDoesNotWaitOutTheCheckpointersSleep() throws Exception {
      // A database's write ahead log checkpointer looks every 5 seconds. This one has
      // only just started, so waiting out its sleep would take nearly all of that.
      DbService.get("shutdownTest").execute("CREATE TABLE IF NOT EXISTS shutdown_test (id INTEGER)");
      Thread.sleep(200);

      long start = System.nanoTime();
      DbService.shutdown();
      long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

      assertTrue("shutdown took " + elapsedMs + " ms", elapsedMs < 1000);
   }
}
