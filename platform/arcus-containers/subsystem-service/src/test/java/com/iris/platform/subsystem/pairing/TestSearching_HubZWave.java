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

import com.iris.messages.MessageBody;
import com.iris.messages.capability.PairingSubsystemCapability.FactoryResetResponse;
import com.iris.messages.model.Model;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;

public class TestSearching_HubZWave extends PairingSubsystemTestCase {
	private Model hub;
	
	@Before
	public void stagePairingSteps() throws Exception {
		expectLoadProductAndReturn(productZWave()).anyTimes();
		expectCurrentProductAndReturn(productZWave()).anyTimes();
		replay();
		
		hub = stageSearchingHub();
		assertSearchingHubNotFound(productAddress);
		assertNoRequests();
	}
	
	@Test
	public void testFactoryReset() {
		// send the request
		{
			Optional<MessageBody> response = factoryReset();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertSearchingHubNotFound(productAddress);
			assertStopPairingRequestSent(hub.getAddress());
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartUnpairingRequestSent(hub.getAddress(), request);
			request.getAction().onResponse(context, buildStartUnpairingResponse(hub.getAddress()));
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(FactoryResetResponse.NAME, response.getMessageType());
			assertEquals(1, FactoryResetResponse.getSteps(response).size());
			assertFactoryResetZWave(productAddress);
		}
	}

	@Test
	public void testFactoryResetHubOffline() {
		// send the request
		{
			Optional<MessageBody> response = factoryReset();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertSearchingHubNotFound(productAddress);
			assertStopPairingRequestSent(hub.getAddress());
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartUnpairingRequestSent(hub.getAddress(), request);
			request.getAction().onTimeout(context);
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertError(FactoryResetResponse.CODE_HUB_OFFLINE, response);
			assertSearchingHubNotFound(productAddress);
		}
	}

}

