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
package com.iris.platform.subsystem.handler;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CareSubsystemCapability.ListActivityRequest;
import com.iris.messages.capability.CareSubsystemCapability.ListActivityResponse;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.type.ActivityInterval;
import com.iris.platform.history.ActivityEventProcessor;
import com.iris.platform.history.HistoryActivityDAO;
import com.iris.platform.history.SubsystemId;

@Singleton
public class ListActivityIntervalHandler {
   
   private final BeanListTransformer<ActivityInterval> transformer;
   private final HistoryActivityDAO activityDao;

   @Inject
   public ListActivityIntervalHandler(
         BeanAttributesTransformer<ActivityInterval> transformer,
         HistoryActivityDAO logDao
   ) {
      this.transformer = new BeanListTransformer<>(transformer);
      this.activityDao = logDao;
   }

   @Request(ListActivityRequest.NAME)
   public MessageBody handleRequest(SubsystemContext<CareSubsystemModel> context, PlatformMessage msg) {
      MessageBody request = msg.getValue();
      Date start = ListActivityRequest.getStart(request);
      Date end = ListActivityRequest.getEnd(request);
      Integer bucketSizeSec = ListActivityRequest.getBucket(request);
      
      Errors.assertRequiredParam(start, ListActivityRequest.ATTR_START);
      Errors.assertRequiredParam(end, ListActivityRequest.ATTR_END);
      Errors.assertRequiredParam(bucketSizeSec, ListActivityRequest.ATTR_BUCKET);
      Errors.assertValidRequest(end.after(start), "end must be after start");

      Set<String> devices = ListActivityRequest.getDevices(request);
      
      if (devices == null || devices.isEmpty()) {
      	devices = context.model().getCareDevices();
      }
      
      SubsystemId subsystemId = new SubsystemId(context.model().getAddress());
      ActivityEventProcessor processor = new ActivityEventProcessor(start, end, (int) bucketSizeSec * 1000, devices);
      List<ActivityInterval> intervals = processor.consume( activityDao.stream(subsystemId.getPrimaryId(), start, end) ); 
      
      return
            ListActivityResponse
               .builder()
               .withIntervals(transformer.convertListToAttributes(intervals))
               .build();
   }
}

