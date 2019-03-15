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
package com.iris.video;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.util.IrisUUID;
import com.iris.video.recording.ConstantVideoTtlResolver;

public class TestVideoUtil {
	UUID placeId = UUID.randomUUID();
	PlatformServiceAddress ruleAddress = Address.platformService(placeId, RuleCapability.NAMESPACE, 1);
	PlatformServiceAddress sceneAddress = Address.platformService(placeId, SceneCapability.NAMESPACE, 2);
	PlatformServiceAddress incidentAddress = Address.platformService(IrisUUID.timeUUID(), AlarmIncidentCapability.NAMESPACE);
	PlatformServiceAddress personAddress = Address.platformService(IrisUUID.randomUUID(), PersonCapability.NAMESPACE);
	
	@Test
	public void testActor() {
		assertEquals(null, VideoUtil.getActorFromPersonId(placeId, null));
		assertEquals(ruleAddress, VideoUtil.getActorFromPersonId(placeId, new UUID(0, ruleAddress.getContextQualifier())));
		assertEquals(sceneAddress, VideoUtil.getActorFromPersonId(placeId, new UUID(1, sceneAddress.getContextQualifier())));
		assertEquals(incidentAddress, VideoUtil.getActorFromPersonId(placeId, (UUID) incidentAddress.getId()));
		assertEquals(personAddress, VideoUtil.getActorFromPersonId(placeId, (UUID) personAddress.getId()));
	}
	
	@Test
	public void testPurgeTime() {
		ConstantVideoTtlResolver ttlResolver = new ConstantVideoTtlResolver(new VideoDaoConfig());
		Date purgeAt = VideoUtil.getPurgeTimestamp(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)+ttlResolver.resolveTtlInSeconds(null, false)*1000, TimeUnit.MILLISECONDS);     
		System.out.println(purgeAt);
		assertTrue(purgeAt.after(new Date()));
		purgeAt = VideoUtil.getPurgeTimestamp(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS)+ttlResolver.resolveTtlInSeconds(null, true)*1000, TimeUnit.MILLISECONDS);     
		System.out.println(purgeAt);
		assertTrue(purgeAt.after(new Date()));
	}
}

