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
package com.iris.platform.rule.environment;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.iris.core.dao.exception.DaoException;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

/**
 * 
 */
@Mocks({ Clock.class, PlaceExecutorFactory.class, PlaceEnvironmentExecutor.class })
public class TestDefaultPlaceExecutorRegistry extends IrisMockTestCase {

   // unit under test
   @Inject DefaultPlaceExecutorRegistry registry;
   
   // mocks
   @Inject Clock mockClock;
   @Inject PlaceExecutorFactory mockExecutorFactory;
   @Inject PlaceEnvironmentExecutor mockExecutor;

   // fixture data
   UUID placeId;
   
   /* (non-Javadoc)
    * @see com.iris.test.IrisTestCase#setUp()
    */
   @Override
   public void setUp() throws Exception {
      super.setUp();
      reset(); // clear any startup behavior
      
      this.placeId = UUID.randomUUID();
      
      Capture<Runnable> runnable = Capture.newInstance(CaptureType.LAST);
      EasyMock
         .expect(mockExecutor.submit(EasyMock.capture(runnable)))
         .andAnswer(() -> {
            try {
               runnable.getValue().run();
               return Futures.immediateFuture(null);
            }
            catch(Exception e) {
               return Futures.immediateFailedFuture(e);
            }
         })
         .anyTimes();
      Capture<Callable<Object>> callable = Capture.newInstance(CaptureType.LAST);
      EasyMock
         .expect(mockExecutor.submit(EasyMock.capture(callable)))
         .andAnswer(() -> {
            try {
               return Futures.immediateFuture(callable.getValue().call());
            }
            catch(Exception e) {
               return Futures.immediateFailedFuture(e);
            }
         })
         .anyTimes();
   }
   
   @Override
   public void tearDown() throws Exception {
      reset(); // clear any shutdown behavior
      super.tearDown();
   }
   
   @Override
   protected void replay() {
      super.replay();
      setTime(Instant.now());
   }

   @Test
   public void testGetPlace() {
      expectLoadPlace(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      assertTrue(executorRef.isPresent());
      assertTrue(registry.isCached(placeId));
      
      // additional access should return the same instance
      assertEquals(executorRef, registry.getExecutor(placeId));
      
      verify();
   }
   
   @Test
   public void testGetPlaceNotFound() {
      expectPlaceNotFound(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      assertFalse(executorRef.isPresent());
      assertTrue(registry.isCached(placeId));
      
      // additional access should return the same instance
      assertEquals(executorRef, registry.getExecutor(placeId));
      
      verify();
   }
   
   @Test
   public void testGetPlaceThrowsError() {
      expectLoadPlaceThrowsError(placeId, new DaoException("BOOM"));
      replay();
      
      assertFalse(registry.isCached(placeId));
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      assertFalse(executorRef.isPresent());
      assertTrue(registry.isCached(placeId));
      
      // additional access should return the same instance
      assertEquals(executorRef, registry.getExecutor(placeId));
      
      verify();
   }
   
   @Test
   public void testStopRunningPlace() {
      expectLoadPlace(placeId);
      expectExecutorStop();
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      
      assertTrue(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      assertFalse(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      verify();
   }
   
   @Test
   public void testStopUnstartedPlace() {
      replay();
      
      assertFalse(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      verify();
   }
   
   @Test
   public void testStopEmptyPlace() {
      expectPlaceNotFound(placeId);
      replay();
      
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      assertFalse(executorRef.isPresent());
      assertTrue(registry.isCached(placeId));
      
      // a cache entry was technically cleared...
      assertTrue(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      assertFalse(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      verify();
   }
   
   @Test
   public void testStopErroredPlace() {
      expectLoadPlaceThrowsError(placeId, new DaoException("Cassandra timed out"));
      replay();
      
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      assertFalse(executorRef.isPresent());
      assertTrue(registry.isCached(placeId));
      
      // a cache entry was technically cleared...
      assertTrue(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      assertFalse(registry.stop(placeId));
      assertFalse(registry.isCached(placeId));
      
      verify();
   }
   
   @Test
   public void testRestartRunningPlace() {
      expectLoadPlace(placeId);
      expectReloadPlace(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertTrue(registry.getExecutor(placeId).isPresent());
      
      verify();
   }
   
   @Test
   public void testRestartResultsInNewEnvironment() {
      PlaceEnvironmentExecutor nu = EasyMock.createMock(PlaceEnvironmentExecutor.class);
      expectLoadPlace(placeId);
      expectReloadPlaceAndReturn(placeId, nu);
      replay();
      EasyMock.replay(nu);
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertTrue(registry.getExecutor(placeId).isPresent());
      PlaceEnvironmentExecutor actual = registry.getExecutor(placeId).get();
      assertSame(nu, actual);
      assertNotSame(mockExecutor, actual);
      
      verify();
      EasyMock.verify(nu);
   }
   
   @Test
   public void testRestartStoppedPlace() {
      expectLoadPlace(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertTrue(registry.getExecutor(placeId).isPresent());
      
      verify();
   }
   
   @Test
   public void testRestartMissingPlace() {
      expectPlaceNotFound(placeId);
      expectLoadPlace(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertTrue(registry.getExecutor(placeId).isPresent());
      
      verify();
   }
   
   @Test
   public void testRestartErroredPlace() {
      expectLoadPlaceThrowsError(placeId, new NullPointerException("Oops"));
      expectLoadPlace(placeId);
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertTrue(registry.getExecutor(placeId).isPresent());
      
      verify();
   }
   
   @Test
   public void testRestartThenPlaceNotFound() {
      expectLoadPlace(placeId);
      expectExecutorStop();
      expectReloadPlaceAndReturn(placeId, null);
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertTrue(registry.isCached(placeId));
      assertFalse(registry.getExecutor(placeId).isPresent());
      
      verify();
   }
   
   @Test
   public void testRestartLoadError() {
      expectLoadPlace(placeId);
      expectReloadPlaceThrowsError(placeId, new NullPointerException("Oops"));
      replay();
      
      assertFalse(registry.isCached(placeId));
      registry.start(placeId);
      assertTrue(registry.isCached(placeId));
      registry.reload(placeId);
      assertFalse(registry.isCached(placeId));
      
      verify();
   }
   
   @Test
   public void testClear() {
      expectLoadPlace(placeId);
      expectExecutorStop();
      replay();
      
      registry.start(placeId);
      registry.clear();
      
      verify();
   }
   
   /**
    * Test case
    * Time firstTick) attempt to load the place returns not found
    * Time cachedTick) a second, very quick, request gets the same negative cache entry
    * Time expiredTick) a bit later a new request is made, the cache entry is expired and re-loaded, this time with data
    * Time recachedTick) a bit later another request is made, this time returning the same entry 
    */
   @Test
   public void testEnvironmentNotFoundThenFound() {
      Instant firstTick = Instant.now();
      Instant cachedTick = firstTick.plusMillis(100);
      Instant expiredTick = cachedTick.plus(10, ChronoUnit.MINUTES);
      Instant recachedTick = expiredTick.plusMillis(100);
      
      Optional<PlaceEnvironmentExecutor> executorRef;
      
      {
         expectPlaceNotFound(placeId);
         replay();
         
         setTime(firstTick);
         executorRef = registry.getExecutor(placeId);
         assertFalse(executorRef.isPresent());
         assertTrue(registry.isCached(placeId));
         
         setTime(cachedTick);
         executorRef = registry.getExecutor(placeId);
         assertFalse(executorRef.isPresent());
         assertTrue(registry.isCached(placeId));
         
         verify();
      }
      
      reset();
      
      {
         expectLoadPlace(placeId);
         replay();
      
         setTime(expiredTick);
         executorRef = registry.getExecutor(placeId);
         assertTrue(executorRef.isPresent());
         assertTrue(registry.isCached(placeId));
         
         // additional access should return the same instance
         setTime(recachedTick);
         assertEquals(executorRef, registry.getExecutor(placeId));
         
         verify();
      }
   }

   private void setTime(Instant tick) {
      EasyMock.reset(mockClock);
      EasyMock
         .expect(mockClock.instant())
         .andReturn(tick)
         .anyTimes();
      EasyMock.replay(mockClock);
   }

   private void expectLoadPlace(UUID placeId) {
      EasyMock
         .expect(mockExecutorFactory.load(placeId))
         .andReturn(mockExecutor)
         .once();
      mockExecutor.start();
      EasyMock
         .expectLastCall()
         .once();
   }
   
   private void expectReloadPlace(UUID placeId) {
      expectReloadPlaceAndReturn(placeId, mockExecutor);
   }

   private void expectReloadPlaceAndReturn(UUID placeId, @Nullable PlaceEnvironmentExecutor executor) {
      mockExecutor.stop();
      EasyMock
         .expectLastCall()
         .once();
      EasyMock
         .expect(mockExecutorFactory.reload(placeId, mockExecutor))
         .andReturn(executor)
         .once();
      if(executor != null) {
         executor.start();
         EasyMock
            .expectLastCall()
            .once();
      }
   }

   private void expectReloadPlaceThrowsError(UUID placeId, Throwable cause) {
      mockExecutor.stop();
      EasyMock
         .expectLastCall()
         .once();
      EasyMock
         .expect(mockExecutorFactory.reload(placeId, mockExecutor))
         .andThrow(cause)
         .once();
   }

   private void expectPlaceNotFound(UUID placeId) {
      EasyMock
         .expect(mockExecutorFactory.load(placeId))
         .andReturn(null)
         .once();
   }

   private void expectLoadPlaceThrowsError(UUID placeId, Throwable cause) {
      EasyMock
         .expect(mockExecutorFactory.load(placeId))
         .andThrow(cause)
         .once();
   }

   private void expectExecutorStop() {
      mockExecutor.stop();
      EasyMock
         .expectLastCall()
         .once();
   }

}

