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

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.type.Population;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.video.VideoDao;
import com.iris.video.cql.VideoConstants;
@Mocks({VideoDao.class, PlaceServiceLevelCache.class})
@Modules({ InMemoryMessageModule.class} )
public class TestPlaceServiceLevelDowngradeListener extends IrisMockTestCase {

	@Inject
	private InMemoryPlatformMessageBus platformBus;
	@Inject
	private VideoDao mockVideoDao;
	@Inject
	private PlaceServiceLevelCache mockCache;
	
	
	private PlaceServiceLevelDowngradeListener theListener;
	private UUID curPlaceId;


	@Test
	public void testDoOnMessageServiceLevelBasic() {
		PlatformMessage message = createListenerAndMessage(ServiceLevel.BASIC);
		//setup mocks
		EasyMock.expect(mockCache.isServiceLevelChange(message)).andReturn(true);
		EasyMock.expect(mockVideoDao.countByTag(curPlaceId, VideoConstants.TAG_FAVORITE)).andReturn(5l);
		Capture<Date> deleteTimeCapture = EasyMock.newCapture(CaptureType.LAST);
		
		mockVideoDao.addToPurgePinnedRecording(EasyMock.eq(curPlaceId), EasyMock.capture(deleteTimeCapture));
		EasyMock.expectLastCall();
		replay();
		
		long now = System.currentTimeMillis();
		theListener.onMessage(message);
		Date deleteTime = deleteTimeCapture.getValue();
		int deleteDelay = (int) TimeUnit.MILLISECONDS.toDays(deleteTime.getTime() - now);
		
		assertEquals(theListener.getPurgePinnedVideosAfterDays(), deleteDelay);
		verify();
	}
	
	@Test
	public void testDoOnMessageServiceLevelPremium() {
		PlatformMessage message = createListenerAndMessage(ServiceLevel.PREMIUM);
		//setup mocks
		EasyMock.expect(mockCache.isServiceLevelChange(message)).andReturn(true);		
		replay();		
		theListener.onMessage(message);
		verify();
	}
	
	@Test
	public void testDoOnMessageServiceLevelBasicNoPinnedVideos() {
		PlatformMessage message = createListenerAndMessage(ServiceLevel.BASIC);
		//setup mocks
		EasyMock.expect(mockCache.isServiceLevelChange(message)).andReturn(true);		
		EasyMock.expect(mockVideoDao.countByTag(curPlaceId, VideoConstants.TAG_FAVORITE)).andReturn(0l);
		replay();		
		theListener.onMessage(message);
		verify();
	}
	
	private PlatformMessage createListenerAndMessage(ServiceLevel sl) {
		//create listener
		theListener = new PlaceServiceLevelDowngradeListener(mockCache, platformBus, mockVideoDao);
		//create PlatformMessage		
		curPlaceId = UUID.randomUUID();
		MessageBody msgbody = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, 
				ImmutableMap.<String, Object>of(PlaceCapability.ATTR_SERVICELEVEL, sl.isPremiumOrPromon()?PlaceCapability.SERVICELEVEL_PREMIUM:PlaceCapability.SERVICELEVEL_BASIC));
		PlatformMessage message = PlatformMessage.builder()
					.from(Address.platformService(curPlaceId, PlaceCapability.NAMESPACE))
					.withPayload(msgbody)
					.withPlaceId(curPlaceId)
					.withPopulation(Population.NAME_GENERAL)
					.create();
		return message;
	}

}

