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
package com.iris.client.impl.netty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.iris.client.ClientMessage;

/**
 * The client logs every incoming message at debug, and Oculus turns that logger on, so the log line
 * must not carry attribute values: a new API key or a Z-Wave S2 PIN arrives as one.
 */
public class NettyIrisClient2DescribeTest {
   private static final String KEY = "c2VjcmV0LWFwaS1rZXktdmFsdWU";

   @Test
   public void testNamesTheMessageWithoutItsValues() {
      ClientMessage msg = ClientMessage.builder()
            .withType("apikey:CreateResponse")
            .withSource("SERV:apikey:")
            .withCorrelationId("42")
            .withAttribute("key", KEY)
            .withAttribute("id", "a-key-id")
            .create();

      String described = NettyIrisClient2.describe(msg);

      assertFalse(described, described.contains(KEY));
      assertFalse(described, described.contains("a-key-id"));
      assertTrue(described, described.contains("apikey:CreateResponse"));
      assertTrue(described, described.contains("SERV:apikey:"));
      assertTrue(described, described.contains("42"));
      assertTrue(described, described.contains("key"));
   }

   @Test
   public void testMessageWithoutAttributes() {
      ClientMessage msg = ClientMessage.builder().withType("base:ValueChange").create();

      String described = NettyIrisClient2.describe(msg);

      assertTrue(described, described.contains("base:ValueChange"));
   }
}
