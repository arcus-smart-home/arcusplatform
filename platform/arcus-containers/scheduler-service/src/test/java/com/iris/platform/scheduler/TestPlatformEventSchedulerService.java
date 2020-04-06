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
package com.iris.platform.scheduler;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.Provides;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.scheduler.model.PartitionOffset;
import com.iris.platform.scheduler.model.ScheduledCommand;
import com.iris.service.scheduler.PlatformEventSchedulerService;
import com.iris.service.scheduler.PlatformSchedulerRegistry;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;


/**
 * 
 */
@Mocks(value={ Scheduler.class, ScheduleDao.class, PlaceDAO.class, PlatformSchedulerRegistry.class })
public class TestPlatformEventSchedulerService extends IrisMockTestCase {


   Capture<ScheduledEvent> eventRef;
   
   @Inject Scheduler mockScheduler;
   @Inject ScheduleDao mockScheduleDao;
   @Inject PlaceDAO mockPlaceDao;
   @Inject PlatformSchedulerRegistry mockPlatformSchedulerRegistry;
   
   @Inject PlatformEventSchedulerService service;
   
   UUID placeId = UUID.randomUUID();
   Address schedulerAddress = Address.platformService(UUID.randomUUID(), SchedulerCapability.NAMESPACE);
   PlatformPartition partition = new DefaultPartition(0);

   @Provides @Named(PlatformEventSchedulerService.NAME_SCHEDULED_EVENT_LISTENER)
   public Listener<ScheduledEvent> listener() {
      eventRef = EasyMock.newCapture(CaptureType.ALL);
      Listener<ScheduledEvent> listener = EasyMock.createMock(Listener.class);
      listener.onEvent(EasyMock.capture(eventRef));
      EasyMock.expectLastCall().anyTimes();
      EasyMock.replay(listener);
      return listener;
   }
   
   @Before
   public void prepareScheduleDao() {
      EasyMock
         .expect(mockScheduleDao.getTimeBucketDurationMs())
         .andReturn(TimeUnit.MINUTES.toMillis(10))
         .anyTimes();
   }
   
   @Test
   public void testScheduleHistorical() {
      Date scheduledTime = new Date(System.currentTimeMillis() - 30000);
      expectScheduleAndReturn(scheduledTime);
      expectAndExecuteScheduleDelayed(0, TimeUnit.MILLISECONDS);
      
      replay();
      
      service.fireEventAt(placeId, schedulerAddress, scheduledTime);
      
      assertTrue(eventRef.hasCaptured());
      assertEquals(1, eventRef.getValues().size());
      
      ScheduledEvent event = eventRef.getValue();
      assertEquals(schedulerAddress, event.getAddress());
      assertEquals(scheduledTime.getTime(), event.getScheduledTimestamp());
      
      verify();
   }
   
   @Test
   public void testScheduleInActive() throws Exception {
      Date currentPartition = new Date();
      Date scheduledTime = new Date(currentPartition.getTime() + 30000);
      PartitionOffset offset = expectGetPendingAndReturnOffset(currentPartition);
      expectStreamByAnyOffset();
      // only schedule one partition
      expectScheduleAndReturn(scheduledTime)
         .setOffset(offset);
      expectAndExecuteScheduleAt(scheduledTime);
      
      replay();
      
      service.onPartitionsChanged(createPartitionChangedEvent(new DefaultPartition(0)));
      
      // TODO need some sort of sync to wait until its done scheduling
      Thread.sleep(1000);
      
      service.fireEventAt(placeId, schedulerAddress, scheduledTime);
      
      // the mock scheduler runs the event immediately
      assertTrue(eventRef.hasCaptured());
      assertEquals(1, eventRef.getValues().size());
      
      ScheduledEvent event = eventRef.getValue();
      assertEquals(schedulerAddress, event.getAddress());
      assertEquals(scheduledTime.getTime(), event.getScheduledTimestamp());
      
      verify();
   }
   
   @Test
   public void testScheduleBeyondHorizon() {
      Date currentPartition = new Date();
      Date scheduledTime = new Date(currentPartition.getTime() + TimeUnit.DAYS.toMillis(1));
      expectScheduleAndReturn(scheduledTime);
      
      replay();
      
      service.fireEventAt(placeId, schedulerAddress, scheduledTime);

      assertFalse(eventRef.hasCaptured());
      
      verify();
   }
   
   @Test
   public void testScheduleHistoricalPartitions() throws Exception {
      Date currentPartition = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(20));
      expectGetPendingAndReturnOffset(currentPartition);
      
      replay();
      
      service.onPartitionsChanged(createPartitionChangedEvent(new DefaultPartition(0)));
      
      // TODO verify the partitions were actually executed
      
      verify();
   }
   
   private PartitionChangedEvent createPartitionChangedEvent(PlatformPartition partition) {
      PartitionChangedEvent event = new PartitionChangedEvent();
      event.setAddedPartitions(ImmutableSet.of(partition.getId()));
      event.setMembers(1);
      event.setPartitions(ImmutableSet.of(partition));
      return event;
   }

   protected PartitionOffset expectGetPendingAndReturnOffset(Date currentOffset) {
      return expectGetPendingAndReturnOffset(currentOffset, 1, TimeUnit.DAYS);
   }
   protected PartitionOffset expectGetPendingAndReturnOffset(Date currentOffset, long partitionDuration, TimeUnit unit) {
      PartitionOffset offset = new PartitionOffset(partition, currentOffset, unit.toMillis(partitionDuration));
      EasyMock
         .expect(mockScheduleDao.getPendingPartitions(ImmutableSet.of(partition)))
         .andReturn(ImmutableMap.of(partition, offset));
      return offset;
   }

   protected ScheduledCommand newScheduledCommand(UUID placeId, Address schedulerAddress, Date scheduledTime) {
      ScheduledCommand command = new ScheduledCommand();
      command.setPlaceId(placeId);
      command.setSchedulerAddress(schedulerAddress);
      command.setScheduledTime(scheduledTime);
      command.setOffset(new PartitionOffset(new DefaultPartition(0), scheduledTime, 600000));
      return command;
   }
   
   protected ScheduledCommand expectScheduleAndReturn(Date scheduledTime) {
      ScheduledCommand command = newScheduledCommand(placeId, schedulerAddress, scheduledTime);
      return expectScheduleAndReturn(scheduledTime, command);
   }
   
   protected ScheduledCommand expectScheduleAndReturn(Date scheduledTime, ScheduledCommand command) {
      EasyMock
         .expect(mockScheduleDao.schedule(placeId, schedulerAddress, scheduledTime))
         .andReturn(command);
      return command;
   }
   
   protected void expectAndExecuteScheduleDelayed(long delay, TimeUnit unit) {
      Capture<Runnable> runnable = EasyMock.newCapture();
      EasyMock
         .expect(mockScheduler.scheduleDelayed(EasyMock.capture(runnable), EasyMock.eq(delay), EasyMock.eq(unit)))
         .andAnswer(new RunAndAnswer(runnable))
         .once();
   }

   protected void expectAndExecuteScheduleAt(Date scheduledTime) {
      Capture<Runnable> runnable = EasyMock.newCapture();
      EasyMock
         .expect(mockScheduler.scheduleAt(EasyMock.capture(runnable), EasyMock.eq(scheduledTime)))
         .andAnswer(new RunAndAnswer(runnable))
         .once();
   }

   protected void expectStreamByAnyOffset() {
      EasyMock
         .expect(mockScheduleDao.streamByPartitionOffset(EasyMock.notNull()))
         .andReturn(ImmutableList.<ScheduledCommand>of().stream())
         .anyTimes();
      
   }

   private class RunAndAnswer implements IAnswer<ScheduledTask> {
      Capture<Runnable> capture;
      
      RunAndAnswer(Capture<Runnable> capture) {
         this.capture = capture;
      }
      
      @Override
      public ScheduledTask answer() throws Throwable {
         capture.getValue().run();
         return new ScheduledTask() {
            @Override
            public boolean isPending() {
               return false;
            }
            
            @Override
            public boolean cancel() {
               return false;
            }
         };
      }
      
   }

}

