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
package com.iris.platform.subsystem.pairing;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllResponse;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.iris.messages.capability.PairingSubsystemCapability.ListHelpStepsResponse;
import com.iris.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.iris.messages.capability.PairingSubsystemCapability.StopSearchingResponse;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;

public class TestPairingSteps_OAuth extends PairingSubsystemTestCase {

	@Before
	public void stagePairingSteps() throws Exception {
		expectLoadProductAndReturn(productOauth()).anyTimes();
		expectCurrentProductAndReturn(productOauth()).anyTimes();

		replay();
		
		stagePairingStepsCloud();
		assertPairingStepsCloud(productAddress);
		assertNoRequests();
	}
	
	@Test
	public void testSearch() throws Exception {
		MessageBody response = search().get();		

		{
			assertEquals(SearchResponse.NAME, response.getMessageType());
			assertEquals(SearchResponse.MODE_OAUTH, SearchResponse.getMode(response));			
		}
	}
	
	@Test
	public void testListHelpSteps() {
		MessageBody response = listHelpSteps().get();
		assertEquals(ListHelpStepsResponse.NAME, response.getMessageType());
		assertNoRequests();
		assertPairingStepsCloud(productAddress);
	}

	@Test
	public void testDismissAll() {
		MessageBody response = dismissAll().get();
		assertEquals(DismissAllResponse.NAME, response.getMessageType());
		assertEquals(ImmutableList.of(), response.getAttributes().get(DismissAllResponse.ATTR_ACTIONS));
		assertNoRequests();
		assertIdle();
	}

	@Test
	public void testFactoryReset() {
		MessageBody response = factoryReset().get();
		assertEquals(FactoryResetResponse.NAME, response.getMessageType());
		assertNoRequests();
		assertFactoryResetIdle(productAddress);
	}

	@Test
	public void testStopSearching() {
		MessageBody response = stopSearching().get();
		assertEquals(StopSearchingResponse.instance(), response);
		assertNoRequests();
		assertIdle();
	}

	@Test
	public void testTimeout() {
		sendTimeout();
		assertNoRequests();
		assertIdle();
	}

}

