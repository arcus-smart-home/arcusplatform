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
package com.iris.client.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.google.common.util.concurrent.SettableFuture;
import com.iris.client.ClientEvent;
import com.iris.client.ClientMessage;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Account;
import com.iris.client.capability.Account.ListDevicesResponse;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Place;
import com.iris.client.capability.Place.RegisterHubResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.service.AccountServiceImpl;
import com.iris.client.impl.I18NServiceImpl;
import com.iris.client.impl.netty.NettyIrisClientFactory;
import com.iris.client.model.AccountModel;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.PersonModel;
import com.iris.client.model.PlaceModel;
import com.iris.client.service.AccountService.CreateAccountResponse;
import com.iris.client.service.I18NService;
import com.iris.client.service.I18NService.LoadLocalizedStringsResponse;
import com.iris.client.session.SessionInfo;
import com.iris.client.session.SessionTokenCredentials;
import com.iris.client.session.UsernameAndPasswordCredentials;

@Ignore // Need to move to Int test project.
public class TestClientConnection {
	@Rule
	public Timeout timeoutRule = new Timeout(1033000);

	private static String connectURL = "ws://127.0.0.1:8081";
	private static SessionInfo sessionInfo;
	private static String username;
	private static String password;

	@BeforeClass
	public static void setUpClass() throws Exception {
		IrisClientFactory.init(new NettyIrisClientFactory());
		IrisClientFactory.getClient().setConnectionURL(connectURL);
		AccountServiceImpl impl = new AccountServiceImpl(IrisClientFactory.getClient());

		// Create the stores.
		IrisClientFactory.getStore(AccountModel.class).clear();
		IrisClientFactory.getStore(PlaceModel.class).clear();
		IrisClientFactory.getStore(PersonModel.class).clear();

		username = UUID.randomUUID().toString().replace("-","") + "@fakeemail.com";
		password = "password$1\\!";
		assertTrue(impl.createAccount(username, password, "true", null, null, null).get() instanceof CreateAccountResponse);

		assertEquals(1, IrisClientFactory.getStore(AccountModel.class).size());
		assertEquals(1, IrisClientFactory.getStore(PlaceModel.class).size());
		assertEquals(1, IrisClientFactory.getStore(PersonModel.class).size());

		TestClientConnection.login();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		assertTrue(Boolean.valueOf((Boolean) IrisClientFactory.getClient().logout().get()));
		
		IrisClientFactory.getClient().close();
	}

	private static void login() throws Exception {
		UsernameAndPasswordCredentials uapc = new UsernameAndPasswordCredentials();
		uapc.setConnectionURL(connectURL);
		uapc.setPassword(password.toCharArray()); // Test01Pa33$ord
		uapc.setUsername(username);

		sessionInfo = IrisClientFactory.getClient().login(uapc).get(3, TimeUnit.SECONDS);

		uapc.clearPassword();

		assertNotNull(sessionInfo);
		assertTrue(uapc.getPassword().matches("^\0+$"));
	}

	@Test
	public void testCreateAccountViaHTTPLoadsStore() throws Exception {
		IrisClientFactory.getClient().logout().get();

		IrisClientFactory.getClient().setConnectionURL(connectURL);
		AccountServiceImpl impl = new AccountServiceImpl(IrisClientFactory.getClient());

		// Create the stores.
		IrisClientFactory.getStore(AccountModel.class).clear();
		IrisClientFactory.getStore(PlaceModel.class).clear();
		IrisClientFactory.getStore(PersonModel.class).clear();

		assertTrue(impl.createAccount(UUID.randomUUID().toString().replace("-","") + "@fakeemail.com", "pass", "true", null, null, null).get() instanceof CreateAccountResponse);

		assertEquals(1, IrisClientFactory.getStore(AccountModel.class).size());
		assertEquals(1, IrisClientFactory.getStore(PlaceModel.class).size());
		assertEquals(1, IrisClientFactory.getStore(PersonModel.class).size());

		IrisClientFactory.getClient().logout().get();
		TestClientConnection.login();
	}
	
	@Test(expected=ExecutionException.class)
	public void testCreateDuplicateAccountReturnsError() throws Exception {
		IrisClientFactory.getClient().setConnectionURL(connectURL);
		AccountServiceImpl impl = new AccountServiceImpl(IrisClientFactory.getClient());
		
		assertTrue(impl.createAccount("test@test.com", "PassWord$1", "true", null, null, null).get() instanceof CreateAccountResponse);
	}
	
	@Test
	public void testLoginWithPreviousSessionToken() throws Exception {
		if (sessionInfo != null) {
			assertTrue(Boolean.valueOf((Boolean) IrisClientFactory.getClient().logout().get()));
		}

		IrisClientFactory.init(new NettyIrisClientFactory());
		SessionTokenCredentials creds = new SessionTokenCredentials();
		creds.setConnectionURL(connectURL);
		creds.setToken(sessionInfo.getSessionToken());

		sessionInfo = null;
		sessionInfo = IrisClientFactory.getClient().login(creds).get(3, TimeUnit.SECONDS);

		assertNotNull(sessionInfo);
		assertEquals("Unknown", sessionInfo.getUsername());
	}

	@Test
	public void testCustomRequest() throws Exception {
		IrisClientFactory.getClient().addMessageListener(new Listener<ClientMessage>() {
			@Override
			public void onEvent(ClientMessage event) {
				System.out.println("Message: " + event);
			}
		});

		ClientRequest request = new ClientRequest();
		request.setAddress("SERV:place:" + sessionInfo.getPlaces().get(0).getPlaceId());
		request.setCommand(Place.CMD_GETHUB);

		ClientEvent eventFuture = IrisClientFactory.getClient().request(request).get();
		assertNotNull(eventFuture);
	}

	@Test
	public void testListDevices() throws Exception {
		if (sessionInfo == null) {
			TestClientConnection.login();
		}

		Map<String, Object> bag = new HashMap<String, Object>();
		bag.put(Capability.ATTR_ID, sessionInfo.getPlaces().get(0).getAccountId());
		bag.put(Capability.ATTR_TYPE, Account.NAMESPACE);
		bag.put(Capability.ATTR_ADDRESS, "SERV:account:" + sessionInfo.getPlaces().get(0).getAccountId());

		IrisClientFactory.getStore(AccountModel.class).clear();
		AccountModel accountModel = (AccountModel) IrisClientFactory.getModelCache().addOrUpdate(bag);
		ClientFuture<ListDevicesResponse> response = accountModel.listDevices();
		assertNotNull(response.get().getDevices());
		assertEquals(1, IrisClientFactory.getStore(AccountModel.class).size());
		
		for (AccountModel models : IrisClientFactory.getStore(AccountModel.class).values()) {
			models.refresh().get();

			assertEquals(1, IrisClientFactory.getStore(AccountModel.class).size());
		}

		List<Map<String, Object>> devices = response.get().getDevices();
		for (Map<String, Object> dev : devices) {
			IrisClientFactory.getModelCache().addOrUpdate(dev);
		}

		assertEquals(Account.CMD_LISTDEVICES + "Response", response.get().getType());
		assertEquals(0, IrisClientFactory.getStore(DeviceModel.class).size());
	}

	@Ignore
	@Test
	public void testRegisterHub() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(Capability.ATTR_ID, sessionInfo.getPlaces().get(0).getPlaceId());
		attributes.put(Capability.ATTR_TYPE, Place.NAMESPACE);
		attributes.put(Capability.ATTR_ADDRESS, "SERV:place:" + sessionInfo.getPlaces().get(0).getPlaceId());

		PlaceModel m = (PlaceModel) IrisClientFactory.getModelCache().addOrUpdate(attributes);
		ClientFuture<RegisterHubResponse> actual = m.registerHub("ABC-2345");
		assertEquals(Capability.EVENT_ADDED, actual.get().getType());
		assertNotNull(actual.get());
	}

	@Test
	public void testGetSecurityQuestions() throws Exception {
		IrisClientFactory.init(new NettyIrisClientFactory());
		IrisClientFactory.getClient().setConnectionURL(connectURL);
		I18NService service = new I18NServiceImpl(IrisClientFactory.getClient());
		LoadLocalizedStringsResponse response = service.loadLocalizedStrings(null, "en-US").get();

		assertNotNull(response);
		assertNotNull(response.getLocalizedStrings());
	}

	@Test
	public void testRequestListener() throws Exception {
		final SettableFuture<String> future = SettableFuture.<String>create();
		Listener<ClientRequest> listener = new Listener<ClientRequest>() {
			@Override
			public void onEvent(ClientRequest event) {
				future.set(event.toString());
			}
		};
		ListenerRegistration listenerRegistration = IrisClientFactory.getClient().addRequestListener(listener);

		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(Capability.ATTR_ID, sessionInfo.getPlaces().get(0).getPlaceId());
		attributes.put(Capability.ATTR_TYPE, Place.NAMESPACE);
		attributes.put(Capability.ATTR_ADDRESS, "SERV:place:" + sessionInfo.getPlaces().get(0).getPlaceId());

		PlaceModel m = (PlaceModel) IrisClientFactory.getModelCache().addOrUpdate(attributes);

		ClientFuture<Place.ListDevicesResponse> actual = m.listDevices();
		actual.get();

		listenerRegistration.remove();
		assertNotNull(future.get());
		assertFalse(listenerRegistration.isRegistered());
	}
}

