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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PairingSubsystemCapability.DismissAllResponse;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.iris.messages.capability.PairingSubsystemCapability.ListHelpStepsResponse;
import com.iris.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.iris.messages.capability.PairingSubsystemCapability.StopSearchingResponse;
import com.iris.messages.model.Model;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;
import com.iris.prodcat.ProductCatalogEntry;

public class TestSearching_Hub extends PairingSubsystemTestCase {
	private Model hub;
	
	@Before
	public void stagePairingSteps() throws Exception {
		ProductCatalogEntry curProduct = productBuilder().zigbee().withPairingIdleTimeoutMs(240000).build();
		expectLoadProductAndReturn(curProduct).anyTimes();
		expectCurrentProductAndReturn(curProduct).anyTimes();
		replay();
		
		hub = stageSearchingHub();
		assertSearchingHubNotFound(productAddress);
		assertNoRequests();
	}
	
	@Test
	public void testSearch() throws Exception {
		// send the request
		{
			Optional<MessageBody> response = search();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertSearchingHubNotFound(productAddress);
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onResponse(context, buildStartPairingResponse(hub.getAddress()));
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(SearchResponse.NAME, response.getMessageType());
			assertEquals(SearchResponse.MODE_HUB, SearchResponse.getMode(response));
			assertSearchingHubNotFound(productAddress);
			// FIXME verify timeouts were reset
		}
	}
	
	@Test
	public void testSearchHubOffline() throws Exception {
		// send the request
		{
			Optional<MessageBody> response = search();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertSearchingHubNotFound(productAddress);
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onTimeout(context);
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertError(SearchResponse.CODE_HUB_OFFLINE, response);
			assertSearchingHubNotFound(productAddress);
		}
	}
	
	@Test
	public void testListHelpSteps() {
		MessageBody response = listHelpSteps().get();
		assertEquals(ListHelpStepsResponse.NAME, response.getMessageType());
		assertSearchingHubNotFound(productAddress);
	}

	@Test
	public void testDismissAll() {
		MessageBody response = dismissAll().get();
		assertEquals(DismissAllResponse.NAME, response.getMessageType());
		assertEquals(ImmutableList.of(), response.getAttributes().get(DismissAllResponse.ATTR_ACTIONS));
		assertStopPairingRequestSent(hub.getAddress());
		assertIdle();
	}

	@Test
	public void testFactoryReset() {
		MessageBody response = factoryReset().get();
		assertEquals(FactoryResetResponse.NAME, response.getMessageType());
		assertStopPairingRequestSent(hub.getAddress());
		assertFactoryResetIdle(productAddress);
	}

	@Test
	public void testStopSearching() {
		MessageBody response = stopSearching().get();
		assertEquals(StopSearchingResponse.instance(), response);
		assertStopPairingRequestSent(hub.getAddress());
		assertIdle();
	}

	@Test
	public void testTimeout() {
		sendTimeout();
		assertNoRequests();
		assertSearchingHubIdle(productAddress);
		
		sendTimeout();
		assertStopPairingRequestSent(hub.getAddress());
		assertIdle();
	}

}

