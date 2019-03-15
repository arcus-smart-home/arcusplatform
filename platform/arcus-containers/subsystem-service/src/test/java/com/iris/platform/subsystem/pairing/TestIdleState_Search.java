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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.DeviceCapability;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.SearchResponse;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.service.BridgeService.RegisterDeviceRequest;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;
import com.iris.prodcat.ProductCatalogEntry;

public class TestIdleState_Search extends PairingSubsystemTestCase {

	@Before
	public void stage() throws Exception {
		// the subsystem will set itself to idle on added
		start();
		assertIdle();
	}
	
	@Test
	public void testGenericSearchNoHub() {
		replay();
		
		MessageBody response = search().get();

		assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
		assertEquals(PairingSubsystemCapability.HubMissingException.CODE_HUB_MISSING, ((ErrorEvent) response).getCode());
		
		assertIdle();
	}

	@Test
	public void testGenericSearchHubTimeout() {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = search();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingSearching();
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onTimeout(context);
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
			assertEquals(PairingSubsystemCapability.HubOfflineException.CODE_HUB_OFFLINE, ((ErrorEvent) response).getCode());
			assertIdle();
		}
	}

	@Test
	public void testGenericSearchWithHub() {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = search();
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingSearching();
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onResponse(context, buildStartPairingResponse(hub.getAddress()));
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(SearchResponse.NAME, response.getMessageType());
			assertEquals(SearchResponse.MODE_HUB, SearchResponse.getMode(response));
			assertSearchingHubNotFound("");
		}
	}

	@Test
	public void testSearchBridgedDevice() {
		addModel(ModelFixtures.buildDeviceAttributes(BridgeCapability.NAMESPACE).put(DeviceCapability.ATTR_PRODUCTID, "aeda43").create());
		expectLoadProductAndReturn(productBridged()).anyTimes();

		replay();

		MessageBody response = search(productAddress, null).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_OAUTH, SearchResponse.getMode(response));

		assertContainsRequestMessageWithAttrs(BridgeCapability.StartPairingRequest.NAME, new HashMap<>());
	}

	@Test
	public void testSearchIpcd() {
		expectLoadProductAndReturn(productIpcd()).anyTimes();

		replay();
		
		Map<String, String> form = ProductFixtures.ipcdForm();
		MessageBody response = search(productAddress, form).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_CLOUD, SearchResponse.getMode(response));
		
		SendAndExpect pairingRequest = popRequest();
		assertEquals(Address.fromString("BRDG::IPCD"), pairingRequest.getRequestAddress());
		assertEquals(RegisterDeviceRequest.NAME, pairingRequest.getMessage().getMessageType());
		assertEquals(form, pairingRequest.getMessage().getAttributes().get(RegisterDeviceRequest.ATTR_ATTRS));
		
		assertSearchingCloudNotFound(productAddress);
	}

	@Test
	public void testSearchIpcdMissingFormParams() {
		expectLoadProductAndReturn(productIpcd());
		replay();
		
		MessageBody response = search(productAddress, ImmutableMap.of()).get();
		assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
		assertEquals(PairingSubsystemCapability.SearchResponse.CODE_REQUEST_PARAM_INVALID, ((ErrorEvent) response).getCode());
		
		assertIdle();
	}

	@Test
	public void testSearchOauthDevice() {
		expectLoadProductAndReturn(productOauth()).anyTimes();

		replay();
		
		MessageBody response = search(productAddress, ImmutableMap.of()).get();
		assertEquals(SearchResponse.NAME, response.getMessageType());
		assertEquals(SearchResponse.MODE_OAUTH, SearchResponse.getMode(response));
		
		
	}

	@Test
	public void testSearchHubDeviceNoHub() {
		expectLoadProductAndReturn(productZigbee());
		replay();
		
		MessageBody response = search(productAddress, ImmutableMap.of()).get();
		assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
		assertEquals(PairingSubsystemCapability.HubMissingException.CODE_HUB_MISSING, ((ErrorEvent) response).getCode());
		
		assertIdle();
	}

	@Test
	public void testSearchHubDeviceWithHubOffline() {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		expectLoadProductAndReturn(productZigbee());
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = search(productAddress, ImmutableMap.of());
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingSearching();
		}
		// complete the action
		{
			SendAndExpect request = popRequest();
			assertStartPairingRequestSent(hub.getAddress(), request);
			request.getAction().onTimeout(context);
			MessageBody response = responses.getValues().get(responses.getValues().size() - 1);
			assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
			assertEquals(PairingSubsystemCapability.HubOfflineException.CODE_HUB_OFFLINE, ((ErrorEvent) response).getCode());
			assertIdle();
		}
	}
	
	@Test
	public void testSearchHubDeviceWithHub() {
		Model hub = addModel(ModelFixtures.createHubAttributes());
		expectLoadProductAndReturn(productZigbee()).anyTimes();
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = search(productAddress, ImmutableMap.of());
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingSearching();
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
		}
	}
	
	@Test
	public void testSearchHubDeviceWithHubWithCustomPairingTimeout() {
		int pairingTimeoutMs = 100000;
		Model hub = addModel(ModelFixtures.createHubAttributes());
		ProductCatalogEntry product = productZigbee();
		product.setPairingTimeoutMs(pairingTimeoutMs);
		expectLoadProductAndReturn(product).anyTimes();
		replay();
		
		// send the request
		{
			Optional<MessageBody> response = search(productAddress, ImmutableMap.of());
			// no response until the hub responds
			assertEquals(Optional.empty(), response);
			assertIdlePendingSearching();
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
		}
	}
	
}

