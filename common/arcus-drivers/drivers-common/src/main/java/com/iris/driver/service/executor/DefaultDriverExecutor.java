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
package com.iris.driver.service.executor;

import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.common.scheduler.Scheduler;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.DriverId;
import com.iris.protocol.ProtocolMessage;

/**
 * Provides a single threaded model for drivers to consume messages, giving
 * them affinity to a single thread when multiple messages are received, but
 * not requiring a thread when no messages are available to process.
 *
 * The {@link #fire(Object)} is expected to be called from
 * multiple threads in a thread pool.  One (and only one) of the threads will win
 * and begin processing messages until the queue is empty, at which point {@link #fire(Object)}
 * will return for that thread, allowing it to be added back into the thread pool.
 * Other threads calling {@link #fire(Object)} while there is already a consumer
 * thread will queue the message and return immediately.
 */
public class DefaultDriverExecutor implements DriverExecutor {
   private static final Logger logger = LoggerFactory.getLogger(DefaultDriverExecutor.class);
   
   private final DeviceDriver        driver;
   private final DeviceDriverContext context;
   private final Scheduler           scheduler;
   private final ConcurrentMap<String, NamedEvent> namedEvents;
   
   /**
    * This lock is used to make sure that running / messages are
    * atomically changed.  I.e. if running is set to null it must
    * mean that messages is empty.  If running is set non-null then
    * there must be a message to process.  In order to prevent race conditions
    * in setting the running state and enqueuing/dequeuing a message they
    * must be set-together.
    */
   private final Object lock = new Object();
   private Thread running = null;
   private PriorityQueue<DeferredEvent> events;
   private boolean stopped = false;
   
   public DefaultDriverExecutor(
         DeviceDriver driver,
         DeviceDriverContext context,
         Scheduler scheduler,
         int queueBacklog
   ) {
      this.driver = driver;
      this.context = context;
      this.scheduler = scheduler;
      this.events = new PriorityQueue<DeferredEvent>(queueBacklog, new EventComparator());
      this.namedEvents = new ConcurrentHashMap<>(4, 0.75f, 1);
   }

   @Override
   public ListenableFuture<Void> fire(Object event) {
      DeferredEvent result = new DeferredEvent(event);
      run(result);
      return result;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.executor.DriverExecutor#start()
    */
   @Override
   public void start() {
      if(context.isTombstoned()) {
         context().getLogger().debug("Sending ForceRemove request for tombstoned device [{}]", context().getDriverAddress());
         PlatformMessage request =
               PlatformMessage
                  .request(context.getDriverAddress())
                  .from(Address.platformService(DeviceCapability.NAMESPACE))
                  .withPayload(DeviceCapability.ForceRemoveRequest.instance())
                  .create();
         context().sendToPlatform(request);
      }
      else {
         context().getLogger().debug("Starting driver [{}]", driver.getDriverId());
         run(new DeferredEvent(DriverEvent.driverStarted()));
      }
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.executor.DriverExecutor#upgraded(com.iris.messages.model.DriverId)
    */
   @Override
   public void upgraded(DriverId previous) {
      context().getLogger().debug("Driver [{}] upgraded", driver.getDriverId());
      run(new DeferredEvent(DriverEvent.driverUpgraded(previous)));
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.executor.DriverExecutor#stop()
    */
   @Override
   public void stop() {
      context().getLogger().debug("Stopping driver [{}]", driver.getDriverId());
      cancelNamedEvents();
      synchronized(lock) {
         this.stopped = true;
      }
      run(new DeferredEvent(DriverEvent.driverStopped()));
   }
   

@Override
   public ListenableFuture<Void> defer(Object event) {
      DeferredEvent e = new DeferredEvent(event);
      if(Thread.currentThread() == getExecutorThread()) {
         queue(e);
      }
      else {
         // have to do this through the scheduler instead of just queuing to
         // ensure there is a thread for it when it runs
         scheduler.scheduleDelayed(e, 0, TimeUnit.MILLISECONDS);
      }
      return e;
   }

   @Override
   public ListenableFuture<Void> defer(Object event, Date timestamp) {
      DeferredEvent e = new DeferredEvent(event);
      scheduler.scheduleAt(e, timestamp);
      return e;
   }

   @Override
   public ListenableFuture<Void> defer(String key, Object event, Date timestamp) {
      NamedEvent e = new NamedEvent(key, event);
      NamedEvent old = namedEvents.put(key, e);
      if(old != null) {
         old.cancel(false);
      }
      scheduler.scheduleAt(e, timestamp);
      return e;
   }

   @Override
   public boolean cancel(String key) {
      NamedEvent e = namedEvents.remove(key);
      if(e == null) {
         return false;
      }
      return e.cancel(false);
   }

   @Override
   public DeviceDriver driver() {
      return driver;
   }

   @Override
   public DeviceDriverContext context() {
      return context;
   }

   @Override
   @Nullable
   public Thread getExecutorThread() {
      synchronized(lock) {
         return running;
      }
   }

   public boolean isRunning() {
      synchronized(lock) {
         return running != null;
      }
   }

   public String getExecutorThreadName() {
      Thread t = getExecutorThread();
      return t == null ? null : t.getName();
   }

   public int getQueuedMessageCount() {
      return events.size();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DefaultDriverExecutor [driver=" + driver + ", context=" + context
            + ", running=" + running + ", backlog=" + getQueuedMessageCount() + "]";
   }

   protected final void run(DeferredEvent event) {
      DeferredEvent e = lockOrQueue(event);
      if(e == null) {
         return;
      }
      try {
         while(e != null) {
            e.deliver();
            e = takeOrUnlock();
         }
      }
      finally {
         // safety, this should never be needed
         unlock();
      }
   }
   
   /**
    * Atomic operation to obtain the run lock or
    * queue the message.  If this returns a non-null
    * value then the current thread has the run lock.
    * @param message
    * @return
    */
   protected final DeferredEvent lockOrQueue(DeferredEvent event) {
      Preconditions.checkNotNull(event, "message may not be null");
      synchronized(lock) {
         queue(event);
         if(tryLock()) {
            return takeOrUnlock();
         }
         return null;
      }
   }

   /**
    * Atomic operation to retrieve the next message
    * to dispatch or release the run lock.
    * @return
    */
   protected final DeferredEvent takeOrUnlock() {
      synchronized (lock) {
         DeferredEvent e = poll();
         if(e == null) {
            unlock();
         }
         return e;
      }
   }

   private boolean tryLock() {
      synchronized(lock) {
         if(running == null) {
            running = Thread.currentThread();
            return true;
         }
         return false;
      }
   }

   private boolean unlock() {
      String otherThreadName;
      synchronized(lock) {
         if(running == null || Thread.currentThread().equals(running)) {
            running = null;
            return true;
         }
         otherThreadName = running != null ? running.getName() : "<not locked>";
      }
      logger.debug("Attempted to unlock consumer from thread [{}] which does not own the lock, lock held by [{}]", Thread.currentThread().getName(), otherThreadName);
      return false;
   }

   private void queue(DeferredEvent event) {
      if(!events.offer(event)) {
         throw new IllegalStateException("Internal queue at capacity!");
      }
   }

   private DeferredEvent poll() {
      synchronized(lock) {
         Preconditions.checkState(running == Thread.currentThread(), "Attempt to poll() from non-dispatch thread");
         if(stopped) {
            DeferredEvent event = events.poll();
            if(event == null) {
               return null;
            }

            if(canExecute(event)) {
               return event;
            }
            else if(event.event instanceof PlatformMessage) {
               return new CancelledMessageEvent((PlatformMessage) event.event);
            }
            else {
               // dropping it
               return null;
            }
         }
         else if(canExecute(events.peek())) {
            return events.poll();
         }
         return null;
      }
   }

   private boolean canExecute(DeferredEvent deferred) {
      if(deferred == null || deferred.event == null) {
         return false;
      }
      Object event = deferred.event;
      if(event instanceof PlatformMessage) {
         return canExecute((PlatformMessage) event);
      }
      if(event instanceof ProtocolMessage) {
         return canExecute((ProtocolMessage) event);
      }
      return true;
   }

   /**
    * Can't run a new platform message while we're still processing the old one.
    * @param message
    * @return
    */
   private boolean canExecute(PlatformMessage message) {
      if(context.hasMessageContext()) {
         context.getLogger().debug("Blocking message processing on pending message: [{}]", context.getCorrelationId());
         return false;
      }
      return true;
   }

   private boolean canExecute(ProtocolMessage message) {
      return true;
   }

   private void deliver(Object event) {
      try {
         // this synchronized is to make sure that all of context is up to date
         // on the current thread, we might be able to achieve the same memory
         // barrier by passing context through an AtomicReference, but this
         // locking in a single thread should be really cheap and may
         // make up for any errors elsewhere
         // NOTE this is NOT lock that is being locked and furthermore we don't
         //      have lock synchronized, so queue will still move forward
         synchronized (context) {
            DriverExecutors.dispatch(event, this);
         }
      }
      catch(Exception e) {
         context.getLogger().warn("Error processing event [{}]", event, e);
      }
   }
   
   private void cancelNamedEvents() {
	   Set<String> allEventNames = namedEvents.keySet();
	   if(allEventNames.size() > 0) {
		   context.getLogger().warn("These future events will be cancelled [{}]", allEventNames);
	   }	   
	   for(String curName : allEventNames) {
		   cancel(curName);
	   }
	}

   private class DeferredEvent extends AbstractFuture<Void> implements Runnable {
      private final Object event;

      DeferredEvent(Object event) {
         this.event = event;
      }
      
      void assertNotExecutorThread() {
         if(isDone()) {
            return;
         }
         if(Thread.currentThread() == getExecutorThread()) {
            throw new IllegalStateException("Can't block on  the result while on the executor thread");
         }
      }

      public void deliver() {
         if(isCancelled()) {
            return;
         }
         DefaultDriverExecutor.this.deliver(event);
         set(null);
      }

      /* (non-Javadoc)
       * @see com.google.common.util.concurrent.AbstractFuture#get(long, java.util.concurrent.TimeUnit)
       */
      @Override
      public Void get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
         assertNotExecutorThread();
         return super.get(timeout, unit);
      }

      /* (non-Javadoc)
       * @see com.google.common.util.concurrent.AbstractFuture#get()
       */
      @Override
      public Void get() throws InterruptedException, ExecutionException {
         assertNotExecutorThread();
         return super.get();
      }

      @Override
      public void run() {
         DefaultDriverExecutor.this.run(this);
      }

   }

   private class NamedEvent extends DeferredEvent {
      private final String name;

      NamedEvent(String name, Object event) {
         super(event);
         this.name = name;
      }

      /* (non-Javadoc)
       * @see com.iris.driver.service.executor.DefaultDriverExecutor.DeferredEvent#deliver()
       */
      @Override
      public void deliver() {    	 
    	 //The reason super.deliver() is called first before removing from namedEvents is otherwise, Scheduler.cancel in the driver will not have any effect
         super.deliver();        
         namedEvents.remove(name, this);
      }

      /* (non-Javadoc)
       * @see com.google.common.util.concurrent.AbstractFuture#set(java.lang.Object)
       */
      @Override
      protected boolean set(Void value) {
         namedEvents.remove(name, this);
         return super.set(value);
      }

      /* (non-Javadoc)
       * @see com.google.common.util.concurrent.AbstractFuture#setException(java.lang.Throwable)
       */
      @Override
      protected boolean setException(Throwable throwable) {
         namedEvents.remove(name, this);
         return super.setException(throwable);
      }
      
   }

   private class CancelledMessageEvent extends DeferredEvent {

      CancelledMessageEvent(PlatformMessage request) {
         super(request);
      }

      /* (non-Javadoc)
       * @see com.iris.driver.service.executor.DefaultDriverExecutor.DeferredEvent#deliver()
       */
      @Override
      public void deliver() {
         context.respondToPlatform(Errors.requestCancelled());
      }
      
   }
   
   // currently sorts protocol messages over platform messages
   // TODO maybe sort order should just be canExecute?
   static class EventComparator implements Comparator<DeferredEvent> {

      @Override
      public int compare(DeferredEvent event1, DeferredEvent event2) {
         Object o1 = event1.event;
         Object o2 = event2.event;
         // TODO separate out platform requests from platform responses
         if(o1 == o2) {
            return 0;
         }
         if(o1 == null) {
            return -1;
         }
         if(o2 == null) {
            return 1;
         }
         if(o1.getClass().equals(o2.getClass())) {
            // we could sort by timestamp here, but we should be
            // receiving events in order already...
            return 0;
         }
         // TODO prioritize DriverEvents?
         return o1 instanceof PlatformMessage ? 1 : -1;
      }

   }

}

