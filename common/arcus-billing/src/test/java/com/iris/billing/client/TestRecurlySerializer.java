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
package com.iris.billing.client;

import static org.junit.Assert.assertNotNull;

import com.iris.billing.client.model.request.AdjustmentRequest;
import org.junit.Before;
import org.junit.Test;

import com.iris.billing.client.model.RecurlyCurrencyFormats;
import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.billing.client.model.request.SubscriptionRequest.SubscriptionTimeframe;
import com.iris.billing.serializer.RecurlyObjectSerializer;

public class TestRecurlySerializer {
	private boolean dumpOutput = false;
	private RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();

	@Before
	public void setUp() throws Exception {
		serializer.enableFormatting(dumpOutput);
	}

	@Test
	public void testGenerateUpdateAccountWithBillingToken() throws Exception {
		AccountRequest account = new AccountRequest();
		//serializer.enableFormatting(true);
		//dumpOutput = true;

		{ // Account props
			account.setFirstName("New First Name");
			account.setLastName("New Last Name");
			account.setCompanyName("Company Name Here");
			account.setEmail("My Email @ email.com");
			account.setEntityUseCode("Avalara Use Code");
			account.setTaxExempt(true);
			account.setAccountID("4aa63ac6-df4f-48d6-9e04-a4c6aa98a276");
			account.setBillingTokenID("Idn_fMeD6U5RxSis7nCGcA");
		}

		serializer.setRoot(account);

		String data = serializer.serialize();
		checkNullAndDump(data);
	}

	@Test
	public void testCreateSubscription() throws Exception {
		// In the create subscription request, this is added in for you. (if you pass by string, subscriptionRequest)
		// Or you can pass accountRequest, subscriptionRequest
		AccountRequest account = new AccountRequest();
		account.setAccountID("MY_ACCOUNT_ID");
		account.setBillingTokenID("With a billing token too!");
		account.setFirstName("My changed first name...");
		serializer.addNestedModel(account);

		SubscriptionRequest subscription = new SubscriptionRequest();
		subscription.setPlanCode("premium");
		subscription.setQuantity(10);
		subscription.setCurrency(RecurlyCurrencyFormats.getDefaultCurrency());

		serializer.setRoot(subscription);
		String data = serializer.serialize();
		checkNullAndDump(data);
	}

	@Test
	public void updateSubscriptionRequest() throws Exception {
		SubscriptionRequest subscription = new SubscriptionRequest();
		subscription.setPlanCode("premium"); // Don't have to specify if not changing.
		subscription.setSubscriptionID("ID_PREVIOUSLY_FETCHED");
		subscription.setTimeframe(SubscriptionTimeframe.RENEWAL); // Default is "NOW" if not present.

		// If the subscription is already established, don't need to set currency.
		SubscriptionAddon addon = new SubscriptionAddon();
		SubscriptionAddon addon2 = new SubscriptionAddon();
		{
			addon.setAddonCode("addon code name");
			addon.setQuantity(10000);
	
			addon2.setAddonCode("addon code name 2");
			addon2.setQuantity(1);
		}
		subscription.addSubscriptionAddon(addon);
		subscription.addSubscriptionAddon(addon2);

		serializer.setRoot(subscription);
		String data = serializer.serialize();
		checkNullAndDump(data);
	}

	@Test
	public void issueCreditRequest() throws Exception {
		AdjustmentRequest request = new AdjustmentRequest();
		request.setQuantity("1");
		request.setCurrency("USD");
		request.setAmountInCents("-2000"); // $20 credit
		request.setDescription("Test credit");
		serializer.setRoot(request);
		String data = serializer.serialize();
		checkNullAndDump(data);
	}

	private void checkNullAndDump(String data) throws Exception {
		assertNotNull(data);

		if (dumpOutput) {
			System.out.println("----------------------------------------------------");
			System.out.println(data);
			System.out.println("----------------------------------------------------");
		}
	}
}

