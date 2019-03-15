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
package com.iris.driver.groovy.context;

import org.junit.Before;
import org.junit.Test;

public class TestLastProtocolMessageTimestampClosure extends AbstractGroovyClosureTestCase {
   
   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      initTest("TestLastProtocolMessageTimestampClosure.gscript");
      script.setProperty("lastProtocolMessageTimestamp", new LastProtocolMessageTimestampClosure(script));
      context.setLastProtocolMessageTimestamp(500);
   }
   
   @Test
   public void testRetrieveLastProtocolMessageTimestamp() throws Exception {
      long lastTimestamp = (Long) script.invokeMethod("testGetTimestamp", new Object[0]);
      assertEquals(500, lastTimestamp);
   }

}

