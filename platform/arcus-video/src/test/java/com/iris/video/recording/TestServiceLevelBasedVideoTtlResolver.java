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
package com.iris.video.recording;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.ServiceLevel;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.video.VideoDaoConfig;

@RunWith(value = Parameterized.class)
@Mocks({PlaceDAO.class})
public class TestServiceLevelBasedVideoTtlResolver extends IrisMockTestCase {
	@Inject
	private PlaceDAO mockPlaceDao;
	@Inject
	private PlaceServiceLevelCache serviceLevelCache;
	@Inject
	private VideoDaoConfig config;
	
	private ServiceLevelBasedVideoTtlResolver ttlResolver;

	@Parameters(name="serviceLevel[{0}], stream[{1}],expectedExpirationInDays[{2}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { ServiceLevel.BASIC, false, 1},
            new Object [] { ServiceLevel.PREMIUM, false, 30},
            new Object [] { ServiceLevel.PREMIUM_ANNUAL, false, 30},
            new Object [] { ServiceLevel.PREMIUM_FREE, false, 30},
            new Object [] { ServiceLevel.PREMIUM_PROMON, false, 30},
            new Object [] { ServiceLevel.PREMIUM_PROMON_ANNUAL, false, 30},
            new Object [] { ServiceLevel.PREMIUM_PROMON_FREE, false, 30},
            new Object [] { null, false, 1},
            new Object [] { ServiceLevel.BASIC, true, 1},
            new Object [] { ServiceLevel.PREMIUM, true, 1},
            new Object [] { ServiceLevel.PREMIUM_ANNUAL, true, 1},
            new Object [] { ServiceLevel.PREMIUM_FREE, true, 1},
            new Object [] { ServiceLevel.PREMIUM_PROMON, true, 1},
            new Object [] { ServiceLevel.PREMIUM_PROMON_ANNUAL, true, 1},
            new Object [] { ServiceLevel.PREMIUM_PROMON_FREE, true, 1},
            new Object [] { null, true, 1}
      );
   }
   
   private final ServiceLevel serviceLevel;
   private final int expectedExpirationInDays;
   private final boolean stream;
   
   public TestServiceLevelBasedVideoTtlResolver(ServiceLevel serviceLevel, boolean stream, int expectedExpirationInDays) {
   	this.serviceLevel = serviceLevel;
   	this.expectedExpirationInDays = expectedExpirationInDays;
   	this.stream = stream;
   }

	@Before
	public void init() throws Exception {
		ttlResolver = new ServiceLevelBasedVideoTtlResolver(serviceLevelCache, config);
		ttlResolver.initMap();
	}

	@Test
	public void testResolveTtlInSeconds() {
		UUID placeId = UUID.randomUUID();
		if(!stream) {
			EasyMock.expect(mockPlaceDao.getServiceLevelById(placeId)).andReturn(serviceLevel);
		}
		replay();
		
		long ttl = ttlResolver.resolveTtlInSeconds(placeId, stream);
		assertEquals(TimeUnit.DAYS.toSeconds(expectedExpirationInDays)+TimeUnit.MILLISECONDS.toSeconds(config.getPurgeDelay()), ttl);
		
		verify();		
	}

}

