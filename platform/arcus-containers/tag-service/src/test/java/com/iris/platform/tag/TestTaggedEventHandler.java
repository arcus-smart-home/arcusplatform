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
package com.iris.platform.tag;

import static com.iris.core.platform.TaggedEventBuilder.KEY_RULE_TEMPLATE_ID;
import static com.iris.messages.address.Address.clientAddress;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_SERVICELEVEL;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_SOURCE;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_VERSION;
import static com.iris.metrics.IrisMetrics.metrics;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.SessionService.TaggedEvent;
import com.iris.metrics.tag.TaggingMetric;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({TagCounterManager.class})
public class TestTaggedEventHandler extends IrisMockTestCase
{
   @Inject
   private TagCounterManager mockTagCounterManager;

   @Inject
   private TaggedEventHandler componentUnderTest;

   @Test
   public void testUnknownTagName()
   {
      expect(mockTagCounterManager.getTagCounter("test.tag")).andReturn(null);

      replay();

      PlatformMessage msg = PlatformMessage.builder()
         .from(clientAddress("test", "10"))
         .withPayload(TaggedEvent.builder()
            .withName("test.tag")
            .build())
         .create();

      componentUnderTest.onTaggedEvent(msg);

      long count = metrics("tag").counter("tagName.unknown").getCount();
      assertThat(count, equalTo(1L));

      // Cleanup
      metrics("tag").counter("tagName.unknown").dec(count);
   }

   @Test
   public void testNormal()
   {
      test(
         TaggedEvent.builder()
            .withName("test.tag")
            .withSource("android")
            .withVersion("2.9.0")
            .withServiceLevel("premium_promon")
            .withContext(null)
            .build(),
         ImmutableMap.of(
            ATTR_SOURCE, "ANDROID",
            ATTR_VERSION, "2.9.0",
            ATTR_SERVICELEVEL, "PREMIUM_PROMON"));
   }

   @Test
   public void testNoServiceLevel()
   {
      test(
         TaggedEvent.builder()
            .withName("test.tag")
            .withSource("android")
            .withVersion("2.9.0")
            .withServiceLevel(null)
            .withContext(null)
            .build(),
         ImmutableMap.of(
            ATTR_SOURCE, "ANDROID",
            ATTR_VERSION, "2.9.0"));
   }

   @Test
   public void testWithContext()
   {
      test(
         TaggedEvent.builder()
            .withName("test.tag")
            .withSource("android")
            .withVersion("2.9.0")
            .withServiceLevel("premium_promon")
            .withContext(ImmutableMap.of(KEY_RULE_TEMPLATE_ID, "abc123"))
            .build(),
         ImmutableMap.of(
            ATTR_SOURCE, "ANDROID",
            ATTR_VERSION, "2.9.0",
            ATTR_SERVICELEVEL, "PREMIUM_PROMON",
            KEY_RULE_TEMPLATE_ID, "abc123"));
   }

   @Test
   public void testWithContextHavingEmptyValue()
   {
      test(
         TaggedEvent.builder()
            .withName("test.tag")
            .withSource("android")
            .withVersion("2.9.0")
            .withServiceLevel("premium_promon")
            .withContext(ImmutableMap.of(KEY_RULE_TEMPLATE_ID, ""))
            .build(),
         ImmutableMap.of(
            ATTR_SOURCE, "ANDROID",
            ATTR_VERSION, "2.9.0",
            ATTR_SERVICELEVEL, "PREMIUM_PROMON"));
   }

   @Test
   public void testWithContextHavingUnknownKey()
   {
      test(
         TaggedEvent.builder()
            .withName("test.tag")
            .withSource("android")
            .withVersion("2.9.0")
            .withServiceLevel("premium_promon")
            .withContext(ImmutableMap.of("someUnknownKey", "abc123"))
            .build(),
         ImmutableMap.of(
            ATTR_SOURCE, "ANDROID",
            ATTR_VERSION, "2.9.0",
            ATTR_SERVICELEVEL, "PREMIUM_PROMON"));
   }

   private void test(MessageBody msgBody, Map<String, Object> expectedMetricTags)
   {
      @SuppressWarnings("unchecked")
      TaggingMetric<Counter> mockTagCounter = createMock(TaggingMetric.class);
      expect(mockTagCounterManager.getTagCounter("test.tag")).andReturn(mockTagCounter);

      Capture<Map<String, Object>> metricTagsCapture = newCapture();
      Counter mockCounter = createMock(Counter.class);
      expect(mockTagCounter.tag(capture(metricTagsCapture))).andReturn(mockCounter);

      mockCounter.inc();
      expectLastCall();

      replay();
      EasyMock.replay(mockTagCounter, mockCounter);

      PlatformMessage msg = PlatformMessage.builder()
         .from(clientAddress("test", "10"))
         .withPayload(msgBody)
         .create();

      componentUnderTest.onTaggedEvent(msg);

      Map<String, Object> metricTags = metricTagsCapture.getValue();
      assertThat(metricTags, equalTo(expectedMetricTags));

      EasyMock.verify(mockTagCounter, mockCounter);
      EasyMock.reset(mockTagCounter, mockCounter);
   }

   @Override
   public void tearDown() throws Exception
   {
      verify();

      reset();

      super.tearDown();
   }
}

