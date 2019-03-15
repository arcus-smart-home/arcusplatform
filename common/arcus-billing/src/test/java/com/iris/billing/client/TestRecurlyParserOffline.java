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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.List;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.AccountNotes;
import com.iris.billing.client.model.Address;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.client.model.Plan;
import com.iris.billing.client.model.PlanAddon;
import com.iris.billing.client.model.PlanAddons;
import com.iris.billing.client.model.Plans;
import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.client.model.RecurlyErrors;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.TaxDetail;
import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.TransactionError;
import com.iris.billing.client.model.Transactions;
import com.iris.billing.deserializer.RecurlyDeserializer;
import com.iris.messages.model.ServiceLevel;

public class TestRecurlyParserOffline {
	private String listPlanResponse = "/xml/plans/listPlansResponse.xml";
	private String getAccountNotesResponse = "/xml/accounts/getAccountNotesResponse.xml";
	private String updateAccountResponse = "/xml/accounts/updateAccountResponse.xml";
	private String listPlanAddonResponse = "/xml/plans/addons/listAddonsForPlanResponse.xml";
	private String errorResponse = "/xml/errors/errorResponse.xml";
	private String transactionErrorResponse = "/xml/errors/transactionError.xml";
	private String multipleErrorResponse = "/xml/errors/multipleErrorResponse.xml";
	private String billingInfoResponse = "/xml/billing/getBillingInfoResponse.xml";
	private String getAccountsSubscriptionResponse = "/xml/subscriptions/getAccountsSubscriptionResponse.xml";
	private String getAccountsSubscriptionResponse2 = "/xml/subscriptions/getAccountsSubscriptionResponse2.xml";
	private String listAccountsSubscriptions  = "/xml/subscriptions/listAccountsSubscriptionsResponse.xml";
	private String adjustmentOne = "/xml/adjustments/adjustment1.xml";
	private String transactionsXML = "/xml/transactions/listAccountTransactionsRequest.xml";
	private String invoiceResponse = "/xml/invoices/listAccountsInvoicesResponse.xml";

	private RecurlyDeserializer deserializer = new RecurlyDeserializer();
	
	@Test
	public void testParseInvoices() throws Exception {
	   DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
	   df.setTimeZone(TimeZone.getTimeZone("GMT"));
	   Invoices invoices = deserializer.parse(getClass().getResourceAsStream("/xml/invoices/invoices.xml"), Invoices.class);
	   assertNotNull(invoices);
	   
	   Invoice invoice = invoices.get(0);
	   assertEquals("https://fakesubdomain.recurly.com/v2/invoices/15099", invoice.getUrl());
	   assertEquals("3296436477505f6a8cb3614938b0fb24", invoice.getInvoiceID());
	   assertEquals("collected", invoice.getState());
	   assertEquals("15099", invoice.getInvoiceNumber());
	   assertNull(invoice.getInvoiceNumberPrefix());
	   assertNull(invoice.getPoNumber());
	   assertNull(invoice.getVatNumber());
	   assertEquals("1498", invoice.getSubtotalInCents());
	   assertEquals("127", invoice.getTaxInCents());
	   assertEquals("1625", invoice.getTotalInCents());
	   assertEquals("USD", invoice.getCurrency());
	   assertEquals("11/21/15", df.format(invoice.getCreatedAt()));
	   assertEquals("11/21/15", df.format(invoice.getClosedAt()));
	   assertEquals("usst", invoice.getTaxType());
	   assertEquals("KS", invoice.getTaxRegion());
	   assertEquals("0.085", invoice.getTaxRate());
	   assertEquals("0", invoice.getNetTerms());
	   assertEquals("automatic", invoice.getCollectionMethod());
	   
	   Address address = invoice.getAddress();
	   assertEquals("123 Fake St.", address.getAddress1());
	   assertNull(address.getAddress2());
	   assertEquals("Overland Park", address.getCity());
	   assertEquals("KS", address.getState());
	   assertEquals("66210", address.getZip());
	   assertEquals("US", address.getCountry());
	   assertNull(address.getPhoneNumber());
	   
	   List<Adjustment> adjustments = invoice.getAdjustments();
	   assertNotNull(adjustments);
	   assertEquals(2, adjustments.size());
	   
	   Adjustment adjustment = adjustments.get(0);
	   assertEquals("charge", adjustment.getAdjustmentType());
	   assertEquals("329643643a91fbbc5ceb394802bf634c", adjustment.getAdjustmentID());
	   assertNull(adjustment.getOriginalAdjustmentUuid());
	   assertEquals("invoiced", adjustment.getState());
	   assertEquals("Premium", adjustment.getDescription());
	   assertEquals("0012345", adjustment.getAccountingCode());
	   assertEquals("premium", adjustment.getProductCode());
	   assertEquals("plan", adjustment.getOrigin());
	   assertEquals("999", adjustment.getUnitAmountInCents());
	   assertEquals("1", adjustment.getQuantity());
	   assertEquals("0", adjustment.getDiscountInCents());
	   assertEquals("85", adjustment.getTaxInCents());
	   assertEquals("1084", adjustment.getTotalInCents());
	   assertEquals("USD", adjustment.getCurrency());
	   assertEquals("false", adjustment.getTaxable());
	   assertEquals("usst", adjustment.getTaxType());
	   assertEquals("false", adjustment.getTaxExempt());
	   assertEquals("KS", adjustment.getTaxRegion());
	   assertEquals("0.085", adjustment.getTaxRate());
	   assertNull(adjustment.getTaxCode());
	   assertEquals("11/21/15", df.format(adjustment.getStartDate()));
	   assertEquals("11/28/15", df.format(adjustment.getEndDate()));
	   assertEquals("11/21/15", df.format(adjustment.getCreatedAt()));
	   
	   adjustment = adjustments.get(1);
      assertEquals("charge", adjustment.getAdjustmentType());
      assertEquals("329643646421fed4f0f0fa4f19b6ab48", adjustment.getAdjustmentID());
      assertNull(adjustment.getOriginalAdjustmentUuid());
      assertEquals("invoiced", adjustment.getState());
      assertEquals("1 Care Add On", adjustment.getDescription());
      assertNull(adjustment.getAccountingCode());
      assertEquals("care", adjustment.getProductCode());
      assertEquals("add_on", adjustment.getOrigin());
      assertEquals("499", adjustment.getUnitAmountInCents());
      assertEquals("1", adjustment.getQuantity());
      assertEquals("0", adjustment.getDiscountInCents());
      assertEquals("42", adjustment.getTaxInCents());
      assertEquals("541", adjustment.getTotalInCents());
      assertEquals("USD", adjustment.getCurrency());
      assertEquals("false", adjustment.getTaxable());
      assertEquals("usst", adjustment.getTaxType());
      assertEquals("false", adjustment.getTaxExempt());
      assertEquals("KS", adjustment.getTaxRegion());
      assertEquals("0.085", adjustment.getTaxRate());
      assertNull(adjustment.getTaxCode());
      assertEquals("11/21/15", df.format(adjustment.getStartDate()));
      assertEquals("11/28/15", df.format(adjustment.getEndDate()));
      assertEquals("11/21/15", df.format(adjustment.getCreatedAt()));  
      
      List<Transaction> transactions = invoice.getTransactions();
      assertNotNull(transactions);
      assertEquals(1, transactions.size());
      
      Transaction transaction = transactions.get(0);
      assertEquals("32964365fdd8935b46da904ab3b5fc6c", transaction.getTransactionID());
      assertEquals("purchase", transaction.getAction());
      assertEquals("1625", transaction.getAmountInCents());
      assertEquals("127", transaction.getTaxInCents());
      assertEquals("USD", transaction.getCurrency());
      assertEquals("success", transaction.getStatus());
      assertEquals("credit_card", transaction.getPaymentMethod());
      assertEquals("8444366", transaction.getReference());
      assertEquals("subscription", transaction.getSource());
      assertEquals("true", transaction.getRecurring());
      assertEquals("true", transaction.getTestPayment());
      assertEquals("false", transaction.getVoidable());
      assertEquals("true", transaction.getRefundable());
      assertNull(transaction.getCvvResult());
      assertEquals("Street address and postal code match.", transaction.getAvsResult());
      assertNull(transaction.getAvsResultStreet());
      assertNull(transaction.getAvsResultPostal());
      assertEquals("11/21/15", df.format(transaction.getCreatedAt()));
      assertNull(transaction.getTransactionError());
      assertEquals("", transaction.getCvvResultCode());
      assertEquals("D", transaction.getAvsResultCode());
      
      assertNotNull(transaction.getAccount());
      Account account = transaction.getAccount();
      assertEquals("a3af48ee-a9ee-4950-b209-ec5573cfd951", account.getAccountID());
      assertNull(account.getUsername());
      assertEquals("William.E.Larson@fakeemail.com", account.getEmail());
      assertNull(account.getFirstName());
      assertNull(account.getLastName());
      assertNull(account.getCompanyName());
      assertNull(account.getVatNumber());
      assertNull(account.isTaxExempt());
      assertNull(account.getAcceptLanguage());
      
      assertNull(account.getAddress());
      
      assertNotNull(account.getBillingInfo());
      BillingInfo billingInfo = account.getBillingInfo();
      assertNull(billingInfo.getTokenID());
      assertEquals("John", billingInfo.getFirstName());
      assertEquals("Doe", billingInfo.getLastName());
      assertEquals("123 Fake St.", billingInfo.getAddress1());
      assertNull(billingInfo.getAddress2());
      assertEquals("Overland Park", billingInfo.getCity());
      assertEquals("KS", billingInfo.getState());
      assertEquals("66210", billingInfo.getZip());
      assertEquals("US", billingInfo.getCountry());
      assertNull(billingInfo.getPhone());
      assertNull(billingInfo.getVatNumber());
      assertNull(billingInfo.getIpAddress());
      assertEquals("2018", billingInfo.getYear());
      assertEquals("7", billingInfo.getMonth());
      assertNull(billingInfo.getPaypalBillingAgreementId());
      assertNull(billingInfo.getAmazonBillingAgreementId());
      assertNull(billingInfo.getIpAddressCountry());
      assertEquals("Visa", billingInfo.getCardType());
      assertEquals("411111", billingInfo.getFirstSix());
      assertEquals("1111", billingInfo.getLastFour());
      assertNull(billingInfo.getCompanyName());
	}

	@Test
	public void testParseUpdateAccountResponse() throws Exception {
		Account account = deserializer.parse(getClass().getResourceAsStream(updateAccountResponse), Account.class);
		assertNotNull(account);

		assertEquals("1", account.getAccountID());
		assertEquals("active", account.getState());
		assertNull(account.getUsername());
		assertEquals("verena@example.com", account.getEmail());
		assertEquals("Verena",account.getFirstName());
		assertEquals("Example",account.getLastName());
		assertEquals("New Company Name",account.getCompanyName());
		assertNull(account.getVatNumber());
		assertFalse(Boolean.valueOf(account.getTaxExempt()));
		assertNotNull(account.getAddress());
		assertEquals("123 Main St.",account.getAddress().getAddress1());
		assertNull(account.getAddress().getAddress2());
		assertEquals("San Francisco",account.getAddress().getCity());
		assertEquals("CA",account.getAddress().getState());
		assertEquals("94105",account.getAddress().getZip());
		assertEquals("US",account.getAddress().getCountry());
		assertNull(account.getAddress().getPhoneNumber());
		assertNull(account.getAcceptLanguage());
		assertEquals("a92468579e9c4231a6c0031c4716c01d",account.getHostedLoginToken());
		assertNotNull(account.getCreatedAt());
	}

	@Test
	public void testParseUpdateAccountResponseAsString() throws Exception {
		String stringResource = Resources.toString(
				Resources.getResource(
						"xml/accounts/updateAccountResponse.xml"),
						Charsets.UTF_8);
		Account account = deserializer.parse(stringResource, Account.class);
		assertNotNull(account);

		assertEquals("1", account.getAccountID());
		assertEquals("active", account.getState());
		assertNull(account.getUsername());
		assertEquals("verena@example.com", account.getEmail());
		assertEquals("Verena",account.getFirstName());
		assertEquals("Example",account.getLastName());
		assertEquals("New Company Name",account.getCompanyName());
		assertNull(account.getVatNumber());
		assertFalse(Boolean.valueOf(account.getTaxExempt()));
		assertNotNull(account.getAddress());
		assertEquals("123 Main St.",account.getAddress().getAddress1());
		assertNull(account.getAddress().getAddress2());
		assertEquals("San Francisco",account.getAddress().getCity());
		assertEquals("CA",account.getAddress().getState());
		assertEquals("94105",account.getAddress().getZip());
		assertEquals("US",account.getAddress().getCountry());
		assertNull(account.getAddress().getPhoneNumber());
		assertNull(account.getAcceptLanguage());
		assertEquals("a92468579e9c4231a6c0031c4716c01d",account.getHostedLoginToken());
		assertNotNull(account.getCreatedAt());
	}

	@Test
	public void testParseUpdateAccountResponseAsFile() throws Exception {
		File fileResource = new File("src/test/resources/xml/accounts/updateAccountResponse.xml");
		Account account = deserializer.parse(fileResource, Account.class);
		assertNotNull(account);

		assertEquals("1", account.getAccountID());
		assertEquals("active", account.getState());
		assertNull(account.getUsername());
		assertEquals("verena@example.com", account.getEmail());
		assertEquals("Verena",account.getFirstName());
		assertEquals("Example",account.getLastName());
		assertEquals("New Company Name",account.getCompanyName());
		assertNull(account.getVatNumber());
		assertFalse(Boolean.valueOf(account.getTaxExempt()));
		assertNotNull(account.getAddress());
		assertEquals("123 Main St.",account.getAddress().getAddress1());
		assertNull(account.getAddress().getAddress2());
		assertEquals("San Francisco",account.getAddress().getCity());
		assertEquals("CA",account.getAddress().getState());
		assertEquals("94105",account.getAddress().getZip());
		assertEquals("US",account.getAddress().getCountry());
		assertNull(account.getAddress().getPhoneNumber());
		assertNull(account.getAcceptLanguage());
		assertEquals("a92468579e9c4231a6c0031c4716c01d",account.getHostedLoginToken());
		assertNotNull(account.getCreatedAt());
	}

	@Test
	public void testParseUpdateAccountResponseAsInputSource() throws Exception {
		String stringResource = Resources.toString(
				Resources.getResource(
						"xml/accounts/updateAccountResponse.xml"),
						Charsets.UTF_8);
		InputSource inputSource = new InputSource(new StringReader(stringResource));
		Account account = deserializer.parse(inputSource, Account.class);
		assertNotNull(account);

		assertEquals("1", account.getAccountID());
		assertEquals("active", account.getState());
		assertNull(account.getUsername());
		assertEquals("verena@example.com", account.getEmail());
		assertEquals("Verena",account.getFirstName());
		assertEquals("Example",account.getLastName());
		assertEquals("New Company Name",account.getCompanyName());
		assertNull(account.getVatNumber());
		assertFalse(Boolean.valueOf(account.getTaxExempt()));
		assertNotNull(account.getAddress());
		assertEquals("123 Main St.",account.getAddress().getAddress1());
		assertNull(account.getAddress().getAddress2());
		assertEquals("San Francisco",account.getAddress().getCity());
		assertEquals("CA",account.getAddress().getState());
		assertEquals("94105",account.getAddress().getZip());
		assertEquals("US",account.getAddress().getCountry());
		assertNull(account.getAddress().getPhoneNumber());
		assertNull(account.getAcceptLanguage());
		assertEquals("a92468579e9c4231a6c0031c4716c01d",account.getHostedLoginToken());
		assertNotNull(account.getCreatedAt());
	}

	@Test
	public void testParseErrorResponse() throws Exception {
		RecurlyErrors errors = deserializer.parse(getClass().getResourceAsStream(errorResponse), RecurlyErrors.class);

		assertNotNull(errors);
		Assert.assertEquals(1, errors.size());
		
		RecurlyError error = errors.get(0);
		assertFalse(errors.hasTransactionError());

		Assert.assertEquals("model_name.field_name", error.getErrorField());
		Assert.assertEquals("$ &24&\\\\ Special Chars [] in this string.", error.getErrorSymbol());
		Assert.assertEquals("en-US", error.getLanguage());
	}

	@Test
	public void testParseMultipleErrorResponse() throws Exception {
		RecurlyErrors errors = deserializer.parse(getClass().getResourceAsStream(multipleErrorResponse), RecurlyErrors.class);

		assertNotNull(errors);
		assertEquals(7, errors.size());
		assertFalse(errors.hasTransactionError());

		Assert.assertEquals("first_error_name", errors.get(0).getErrorField());
		Assert.assertEquals("long_name_here_instead_of_a_short_name", errors.get(1).getErrorField());
		Assert.assertEquals("short_name", errors.get(2).getErrorField());
		Assert.assertEquals("model_name", errors.get(3).getErrorField());
		Assert.assertEquals("model_name.that_field", errors.get(4).getErrorField());
		Assert.assertEquals("model_name.this_field", errors.get(5).getErrorField());
		Assert.assertEquals("model_name.other_field_name", errors.get(6).getErrorField());

		for (RecurlyError errorIndy : errors) {
			Assert.assertEquals("not_a_number", errorIndy.getErrorSymbol());
			Assert.assertEquals("en-US", errorIndy.getLanguage());
		}
	}

	@Test
	public void testParseTransactionError() throws Exception {
		RecurlyErrors errors = deserializer.parse(getClass().getResourceAsStream(transactionErrorResponse), RecurlyErrors.class);

		assertNotNull(errors);
		assertEquals(1, errors.size());
		assertTrue(errors.hasTransactionError());
		assertNotNull(errors.getTransactionErrors());
		assertEquals(1, errors.getTransactionErrors().size());
		assertNotNull(errors.getTransactions());
		assertEquals(1, errors.getTransactions().size());

		TransactionError error = errors.getTransactionErrors().get(0);
		assertEquals("fraud_gateway", error.getErrorCode());
		assertEquals("fraud", error.getErrorCategory());
		assertEquals("The payment gateway declined the transaction due to fraud filters enabled in your gateway.", error.getMerchantMessage());
		assertEquals("The transaction was declined. Please use a different card, contact your bank, or contact support.", error.getCustomerMessage());

		Transaction trans = errors.getTransactions().get(0);
		assertEquals("2e48cf3e028493e183c633430595ec12",trans.getTransactionID());
		assertEquals("verify",trans.getAction());
		assertEquals("0",trans.getAmountInCents());
		assertEquals("0",trans.getTaxInCents());
		assertEquals("USD",trans.getCurrency());
		assertEquals("declined",trans.getStatus());
		assertEquals("credit_card",trans.getPaymentMethod());
		assertEquals("8734156",trans.getReference());
		assertTrue(Boolean.valueOf(trans.getTestPayment()));
		assertFalse(Boolean.valueOf(trans.getVoidable()));
		assertFalse(Boolean.valueOf(trans.getRefundable()));
		assertEquals("C",trans.getCvvResultCode());
		assertEquals("C_CODE",trans.getCvvResult());
		assertEquals("A",trans.getAvsResultCode());
		assertEquals("A_CODE",trans.getAvsResult());
		assertNull(trans.getAvsResultStreet());
		assertNull(trans.getAvsResultPostal());
		assertNotNull(trans.getCreatedAt());

		assertNotNull(trans.getAccount());
		Account act = trans.getAccount();
		assertEquals("4aa63ac6-df4f-48d6-9e04-a4c6aa98a276",act.getAccountID());
		assertEquals("Example",act.getFirstName());
		assertEquals("Account",act.getLastName());
		assertEquals("Not required",act.getCompanyName());
		assertEquals("null@cybersource.com",act.getEmail());

		assertNotNull(act.getBillingInfo());
		BillingInfo bi = act.getBillingInfo();
		assertEquals("Name on",bi.getFirstName());
		assertEquals("Credit Card",bi.getLastName());
		assertEquals("123 Address Drive", bi.getAddress1());
		assertNull(bi.getAddress2());
		assertEquals("City Name",bi.getCity());
		assertNull(bi.getState());
		assertEquals("12345",bi.getZip());
		assertEquals("UsA",bi.getCountry());
		assertNull(bi.getPhone());
		assertNull(bi.getVatNumber());
		assertEquals("Visa",bi.getCardType());
		assertEquals("2015",bi.getYear());
		assertEquals("11",bi.getMonth());
		assertEquals("400000",bi.getFirstSix());
		assertEquals("0085",bi.getLastFour());
	}

	@Test
	public void testParseBillingInfoResponse() throws Exception {
		BillingInfo info = deserializer.parse(getClass().getResourceAsStream(billingInfoResponse), BillingInfo.class);

		assertNotNull(info);
		assertEquals("Verena",info.getFirstName());
		assertEquals("Example",info.getLastName());
		assertNull(info.getCompanyName());
		assertEquals("123 Main St.",info.getAddress1());
		assertNull(info.getAddress2());
		assertEquals("San Francisco",info.getCity());
		assertEquals("CA", info.getState());
		assertEquals("94105", info.getZip());
		assertEquals("US", info.getCountry());
		assertEquals("US1234567890", info.getVatNumber());
		assertEquals("127.0.0.1", info.getIpAddress());
		assertEquals("US", info.getIpAddressCountry());
		assertEquals("B-1234567890", info.getPaypalBillingAgreementId());
		assertEquals("A-1234567890", info.getAmazonBillingAgreementId());
	}

	@Test
	public void testParseListPlanResponse() throws Exception {
		Plans plans = deserializer.parse(getClass().getResourceAsStream(listPlanResponse), Plans.class);

		assertNotNull(plans);
		assertEquals(1, plans.size());

		Plan info = plans.get(0);
		assertEquals("gold",info.getPlanCode());
		assertEquals("Gold plan",info.getName());
		assertNull(info.getDescription());
		assertNull(info.getSuccessUrl());
		assertNull(info.getCancelUrl());
		assertFalse(Boolean.valueOf(info.getDisplayQuantity()));
		assertEquals("unit",info.getUnitName());
		assertNull(info.getPlanTOSLink());
		assertNull(info.getPlanIntervalLength());
		assertEquals("months",info.getPlanIntervalUnit());
		assertEquals("0",info.getTrialIntervalLength());
		assertEquals("days",info.getTrialIntervalUnit());
		assertNull(info.getAccountingCode());
		assertFalse(Boolean.valueOf(info.getTaxExempt()));

		assertNotNull(info.getUnitAmountInCents());
		assertEquals(2, info.getUnitAmountInCents().getAllCosts().size());
		assertEquals("1000", info.getUnitAmountInCents().getCostForCurrency("USD"));
		assertEquals("800", info.getUnitAmountInCents().getCostForCurrency("EUR"));
		assertEquals(2, info.getSetupFeeInCents().getAllCosts().size());
		assertEquals("6000", info.getSetupFeeInCents().getCostForCurrency("USD"));
		assertEquals("4500", info.getSetupFeeInCents().getCostForCurrency("EUR"));
		assertNotNull(info.getCreatedAt());
		// Unhandled:
		// display_donation_amounts
		// display_phone_number
		// bypass_hosted_confirmation
	}

	@Test
	public void testParseListPlanAddonResponse() throws Exception {
		PlanAddons addons = deserializer.parse(getClass().getResourceAsStream(listPlanAddonResponse), PlanAddons.class);

		assertNotNull(addons);
		assertEquals(1, addons.size());

		PlanAddon info = addons.get(0);
		assertEquals("IP Addresses",info.getName());
		assertFalse(Boolean.valueOf(info.getDisplayQuantityOnHostedPage()));
		assertEquals("1",info.getDefaultQuantity());
		assertEquals("ipaddresses",info.getAddOnCode());

		assertNotNull(info.getUnitAmountInCents());
		assertEquals(1, info.getUnitAmountInCents().getAllCosts().size());
		assertEquals("200", info.getUnitAmountInCents().getCostForCurrency("USD"));
		// Omitted created_at
	}

	@Test
	public void testParseGetAccountNotesResponse() throws Exception {
		AccountNotes info = deserializer.parse(getClass().getResourceAsStream(getAccountNotesResponse), AccountNotes.class);

		assertNotNull(info);
		assertEquals(2, info.size());

		Assert.assertEquals("This is my first note", info.get(0).getMessage());
		Assert.assertEquals("This is my second note", info.get(1).getMessage());
	}

	@Test
	public void testParseGetAccountsSubscriptionResponse() throws Exception {
		Subscription info = deserializer.parse(getClass().getResourceAsStream(getAccountsSubscriptionResponse), Subscription.class);

		assertNotNull(info.getPlanDetails());

		assertEquals("44f83d7cba354d5b84812419f923ea96", info.getSubscriptionID());
		assertEquals("active", info.getState());
		assertEquals("800", info.getUnitAmountInCents());
		assertEquals("EUR", info.getCurrency());
		assertEquals(1, info.getQuantity());
		assertNull(info.getCanceledAt());
		assertNull(info.getExpiresAt());
		assertNotNull(info.getCurrentPeriodStartedAt());
		assertNotNull(info.getCurrentPeriodEndsAt());
		assertNull(info.getTrialStartedAt());
		assertNull(info.getTrialEndsAt());
		assertEquals("80", info.getTaxInCents());
		assertEquals("usst", info.getTaxType());
		assertEquals("CA", info.getTaxRegion());
		assertEquals("0.0875", info.getTaxRate());
		assertNull(info.getPoNumber());
		assertEquals("0", info.getNetTerms());

		Plan plan = info.getPlanDetails();
		assertEquals("gold", plan.getPlanCode());
		assertEquals("Gold plan", plan.getName());

		assertNotNull(info.getSubscriptionAddOns());
		List<SubscriptionAddon> addons = info.getSubscriptionAddOns();
		assertFalse(addons.isEmpty());
		assertEquals(2, addons.size());
		assertEquals("extra_users", addons.get(0).getAddonCode());
		assertEquals(new Integer(2), addons.get(0).getQuantity());
		assertEquals("1000",addons.get(0).getUnitAmountInCents());
		assertEquals("extra_ip",addons.get(1).getAddonCode());
		assertEquals(new Integer(3),addons.get(1).getQuantity());
		assertEquals("200", addons.get(1).getUnitAmountInCents());


		// Nested subscription should be similar to outer
		assertNotNull(info.getPendingSubscription());

		Subscription pending = info.getPendingSubscription();
		assertNotNull(pending.getPlanDetails());

		assertEquals("NESTED_SUB_ID", pending.getSubscriptionID());
		assertEquals("active", pending.getState());
		assertEquals("800", pending.getUnitAmountInCents());
		assertEquals("EUR", pending.getCurrency());
		assertEquals(1, pending.getQuantity());
		assertNull(pending.getCanceledAt());
		assertNull(pending.getExpiresAt());
		assertNull(pending.getCurrentPeriodStartedAt());
		assertNull(pending.getCurrentPeriodEndsAt());
		assertNull(pending.getTrialStartedAt());
		assertNull(pending.getTrialEndsAt());
		assertEquals("80", pending.getTaxInCents());
		assertEquals("usst", pending.getTaxType());
		assertEquals("CA", pending.getTaxRegion());
		assertEquals("0.0875", pending.getTaxRate());
		assertNull(pending.getPoNumber());
		assertEquals("0", pending.getNetTerms());

		Plan planNested = pending.getPlanDetails();
		assertEquals("gold", planNested.getPlanCode());
		assertEquals("Gold plan", planNested.getName());

		assertNotNull(info.getSubscriptionAddOns());
		List<SubscriptionAddon> addonsNested = pending.getSubscriptionAddOns();
		assertFalse(addonsNested.isEmpty());
		assertEquals(2, addons.size());
		assertEquals("extra_users", addonsNested.get(0).getAddonCode());
		assertEquals(new Integer(2), addonsNested.get(0).getQuantity());
		assertEquals("1000",addonsNested.get(0).getUnitAmountInCents());
		assertEquals("extra_ip",addonsNested.get(1).getAddonCode());
		assertEquals(new Integer(3),addonsNested.get(1).getQuantity());
		assertEquals("200", addonsNested.get(1).getUnitAmountInCents());
	}

	@Test
   public void testParseGetAccountsSubscriptionResponse2() throws Exception {
      Subscriptions subs = deserializer.parse(getClass().getResourceAsStream(getAccountsSubscriptionResponse2), Subscriptions.class);

      assertNotNull(subs);
      assertEquals(1, subs.size());

      Subscription info = subs.get(0);

      assertEquals(ServiceLevel.PREMIUM, info.getServiceLevel());

      assertNotNull(info.getPlanDetails());

      assertEquals("301bc612b47fc9befbb4d44d789dfca4", info.getSubscriptionID());
      assertEquals("active", info.getState());
      assertEquals("999", info.getUnitAmountInCents());
      assertEquals("USD", info.getCurrency());
      assertEquals(1, info.getQuantity());
      assertNull(info.getCanceledAt());
      assertNull(info.getExpiresAt());
      assertNotNull(info.getCurrentPeriodStartedAt());
      assertNotNull(info.getCurrentPeriodEndsAt());
      assertNull(info.getTrialStartedAt());
      assertNull(info.getTrialEndsAt());
      assertNull(info.getTaxInCents());
      assertNull(info.getTaxType());
      assertNull(info.getTaxRegion());
      assertNull(info.getTaxRate());
      assertNull(info.getPoNumber());
      assertEquals("0", info.getNetTerms());

      Plan plan = info.getPlanDetails();
      assertEquals("premium", plan.getPlanCode());
      assertEquals("Premium", plan.getName());

      assertNotNull(subs.get(0).getSubscriptionAddOns());
      List<SubscriptionAddon> addons = subs.get(0).getSubscriptionAddOns();
      assertTrue(addons.isEmpty());
   }

	@Test
	public void testParseListAccountsSubscriptionResponse() throws Exception {
		Subscriptions info = deserializer.parse(getClass().getResourceAsStream(listAccountsSubscriptions), Subscriptions.class);

		assertNotNull(info);
		assertEquals(2, info.size());

		Subscription subscription1 = info.get(0);
		assertEquals("44f83d7cba354d5b84812419f923ea96", subscription1.getSubscriptionID());
		assertEquals("active", subscription1.getState());
		assertEquals("800", subscription1.getUnitAmountInCents());
		assertEquals("EUR", subscription1.getCurrency());
		assertEquals(1, subscription1.getQuantity());
		assertNotNull(subscription1.getActivatedAt());
		assertNull(subscription1.getCanceledAt());
		assertNull(subscription1.getExpiresAt());
		assertNotNull(subscription1.getCurrentPeriodEndsAt());
		assertNotNull(subscription1.getCurrentPeriodStartedAt());
		assertNull(subscription1.getTrialEndsAt());
		assertNull(subscription1.getTrialStartedAt());
		assertEquals("80", subscription1.getTaxInCents());
		assertEquals("usst", subscription1.getTaxType());
		assertEquals("0.1111", subscription1.getTaxRate());
		assertEquals("CA", subscription1.getTaxRegion());
		assertNull(subscription1.getPoNumber());
		assertEquals("0", subscription1.getNetTerms());
		assertTrue(subscription1.getSubscriptionAddOns().isEmpty());

		assertNotNull(subscription1.getPlanDetails());

		Plan sub1Plan = subscription1.getPlanDetails();
		assertEquals("gold", sub1Plan.getPlanCode());
		assertEquals("Gold plan", sub1Plan.getName());


		Subscription subscription2 = info.get(1);
		assertEquals("SOME_UUID", subscription2.getSubscriptionID());
		assertEquals("INACTIVE", subscription2.getState());
		assertEquals("900", subscription2.getUnitAmountInCents());
		assertEquals("EUR", subscription2.getCurrency());
		assertEquals(1, subscription2.getQuantity());
		assertNotNull(subscription2.getActivatedAt());
		assertNull(subscription2.getCanceledAt());
		assertNull(subscription2.getExpiresAt());
		assertNotNull(subscription2.getCurrentPeriodEndsAt());
		assertNotNull(subscription2.getCurrentPeriodStartedAt());
		assertNull(subscription2.getTrialEndsAt());
		assertNull(subscription2.getTrialStartedAt());
		assertEquals("80", subscription2.getTaxInCents());
		assertEquals("usst", subscription2.getTaxType());
		assertEquals("0.0875", subscription2.getTaxRate());
		assertEquals("CA", subscription2.getTaxRegion());
		assertNull(subscription2.getPoNumber());
		assertEquals("0", subscription2.getNetTerms());
		assertTrue(subscription2.getSubscriptionAddOns().isEmpty());

		assertNotNull(subscription2.getPlanDetails());

		Plan sub2Plan = subscription2.getPlanDetails();
		assertEquals("silver", sub2Plan.getPlanCode());
		assertEquals("Silver plan", sub2Plan.getName());
	}

	@Test
	public void testParseAdjustmentOne()  throws Exception {
		Adjustments adjustments = deserializer.parse(getClass().getResourceAsStream(adjustmentOne), Adjustments.class);

		assertNotNull(adjustments);
		assertEquals(3, adjustments.size());

		for (int i = 1; i < 4; i++) {
			Adjustment adj = adjustments.get(i - 1);
			assertEquals("ADJ_" + i + "_ID", adj.getAdjustmentID());
			assertEquals("ADJ_" + i + "_STATE", adj.getState());
			assertEquals("ADJ_" + i + "_DESC", adj.getDescription());
			assertNull(adj.getAccountingCode());
			assertEquals("ADJ_" + i + "_PROD_CODE", adj.getProductCode());
			assertEquals("ADJ_" + i + "_ORIGIN", adj.getOrigin());
			assertEquals("2000", adj.getUnitAmountInCents());
			assertEquals("ADJ_" + i + "_ORIG_ADJ_ID", adj.getOriginalAdjustmentUuid());
			assertEquals("0", adj.getDiscountInCents());
			assertEquals("180", adj.getTaxInCents());
			assertEquals("2180", adj.getTotalInCents());
			assertEquals("USD", adj.getCurrency());
			assertFalse(Boolean.valueOf(adj.getTaxable()));
			assertFalse(Boolean.valueOf(adj.getTaxExempt()));
			assertNull(adj.getTaxCode());
			assertNotNull(adj.getStartDate());
			assertNull(adj.getEndDate());
			assertNotNull(adj.getCreatedAt());

			assertNotNull(adj.getTaxDetails());
			assertEquals(2, adj.getTaxDetails().size());

			List<TaxDetail> detailsDetails = adj.getTaxDetails();
			assertEquals("ADJ_" + i + "_TD_NAME", detailsDetails.get(0).getName());
			assertEquals("ADJ_" + i + "_TD_STATE", detailsDetails.get(0).getTaxType());
			assertEquals("1", detailsDetails.get(0).getTaxRate());
			assertEquals("1", detailsDetails.get(0).getTaxInCents());
			assertNull(detailsDetails.get(1).getName());
			assertEquals("ADJ_" + i + "_TD_STATE", detailsDetails.get(1).getTaxType());
			assertEquals("2", detailsDetails.get(1).getTaxRate());
			assertEquals("2", detailsDetails.get(1).getTaxInCents());
		}
	}

	@Test
	public void testParseTransactions()  throws Exception {
		Transactions transactions = deserializer.parse(getClass().getResourceAsStream(transactionsXML), Transactions.class);
		
		assertNotNull(transactions);
		assertEquals(1, transactions.size());
		
		Transaction transaction = transactions.get(0);

		assertNotNull(transaction);
		assertNotNull(transaction.getTransactionError());

		assertEquals("fraud_security_code", transaction.getTransactionError().getErrorCode());

		assertEquals("a13acd8fe4294916b79aec87b7ea441f", transaction.getTransactionID());
		assertEquals("purchase", transaction.getAction());
		assertEquals("1000", transaction.getAmountInCents());
		assertEquals("0", transaction.getTaxInCents());
		assertEquals("USD", transaction.getCurrency());
		assertEquals("success", transaction.getStatus());
		assertEquals("credit_card", transaction.getPaymentMethod());
		assertNull(transaction.getReference());
		assertEquals("subscription", transaction.getSource());
		assertFalse(Boolean.valueOf(transaction.getRecurring()));
		assertTrue(Boolean.valueOf(transaction.getTestPayment()));
		assertTrue(Boolean.valueOf(transaction.getVoidable()));
		assertTrue(Boolean.valueOf(transaction.getRefundable()));
		assertNull(transaction.getCvvResult());
		assertNull(transaction.getAvsResult());
		assertNull(transaction.getAvsResultStreet());
		assertNull(transaction.getAvsResultPostal());
		assertNotNull(transaction.getCreatedAt());

		assertNotNull(transaction.getAccount());
		Account account = transaction.getAccount();
		assertEquals("verena100", account.getAccountID());
		assertEquals("Verena", account.getFirstName());
		assertEquals("Example", account.getLastName());
		assertNull(account.getCompanyName());
		assertEquals("verena@test.com", account.getEmail());

		assertNotNull(transaction.getAccount().getBillingInfo());
		BillingInfo info = transaction.getAccount().getBillingInfo();
		assertNull(info.getFirstName());
		assertNull(info.getLastName());
		assertNull(info.getAddress1());
		assertNull(info.getAddress2());
		assertNull(info.getCity());
		assertNull(info.getState());
		assertNull(info.getZip());
		assertNull(info.getCountry());
		assertNull(info.getPhone());
		assertNull(info.getVatNumber());
		assertEquals("Visa",info.getCardType());
		assertEquals("2015",info.getYear());
		assertEquals("11",info.getMonth());
		assertEquals("411111",info.getFirstSix());
		assertEquals("1111",info.getLastFour());
	}

	@Test
	public void testParseInvoice() throws Exception {
		Invoices invoices = deserializer.parse(getClass().getResourceAsStream(invoiceResponse), Invoices.class);

		assertNotNull(invoices);
		assertEquals(2, invoices.size());

		Invoice info = invoices.get(0);
		assertEquals("421f7b7d414e4c6792938e7c49d552e9",info.getInvoiceID());
		assertEquals("open",info.getState());
		assertEquals("1005",info.getInvoiceNumber());
		assertNull(info.getPoNumber());
		assertNull(info.getVatNumber());
		assertEquals("1200",info.getSubtotalInCents());
		assertEquals("0",info.getTaxInCents());
		assertEquals("1200",info.getTotalInCents());
		assertEquals("USD",info.getCurrency());
		assertNotNull(info.getCreatedAt());
		assertNull(info.getClosedAt());
		assertEquals("usst",info.getTaxType());
		assertEquals("CA",info.getTaxRegion());
		assertEquals("0",info.getTaxRate());
		assertEquals("0",info.getNetTerms());
		assertEquals("automatic",info.getCollectionMethod());

		// This doesn't have details, but it does have an HREF we can look at.
		assertNotNull(info.getAdjustments());
		assertEquals(1, info.getAdjustments().size());

		assertEquals(0, info.getTransactions().size());

		assertNotNull(info.getAddress());

		Address address = info.getAddress();
		assertEquals("123 Street Drive", address.getAddress1());
		assertNull(address.getAddress2());
		assertEquals("Mooresville", address.getCity());
		assertEquals("NC", address.getState());
		assertEquals("28677", address.getZip());
		assertEquals("US", address.getCountry());
		assertEquals("7047584000", address.getPhoneNumber());
	}
}

