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
package com.iris.billing.client.model.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.Address;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.RecurlyModel;
import com.iris.billing.client.model.RecurlyModels;

public class AccountRequest extends RecurlyModel {
	private String accountID;
	private String entityUseCode;
	private String username;
	private String email;
	private String firstName;
	private String lastName;
	private String companyName;
	private String vatNumber;
	private Boolean taxExempt;
	private String acceptLanguage;
	private String billingTokenID;
	private Address address;

	private static final String sMAX_CHARS = "Max of 50 characters";
	private Map<String, Object> mappings = new HashMap<>();

	@Override
	public Map<String, Object> getXMLMappings() {
		return Collections.unmodifiableMap(mappings);
	}

	@Override
	public String getTagName() {
		return Account.Tags.TAG_NAME;
	}
	
	@Override
   public RecurlyModels<?> createContainer() {
      // This is no container for AccountRequest
      return null;
   }

   public final String getAccountID() {
		return accountID;
	}

	public final void setAccountID(String accountCode) {
		this.accountID = accountCode;
		mappings.put(Account.Tags.ACCOUNT_CODE, accountCode);
	}

	public final String getUsername() {
		return username;
	}

	public final void setUsername(String username) {
		this.username = username;
		mappings.put(Account.Tags.USERNAME, username);
	}

	public final String getEmail() {
		return email;
	}

	public final void setEmail(String email) {
		this.email = email;
		mappings.put(Account.Tags.EMAIL, email);
	}

	public final String getFirstName() {
		return firstName;
	}

	public final void setFirstName(String firstName) {
		if (!Strings.isNullOrEmpty(firstName) && firstName.length() > 50) {
			throw new IllegalArgumentException(sMAX_CHARS);
		}

		this.firstName = firstName;
		mappings.put(Account.Tags.FIRST_NAME, firstName);
	}

	public final String getLastName() {
		return lastName;
	}

	public final void setLastName(String lastName) {
		if (!Strings.isNullOrEmpty(lastName) && lastName.length() > 50) {
			throw new IllegalArgumentException(sMAX_CHARS);
		}

		this.lastName = lastName;
		mappings.put(Account.Tags.LAST_NAME, lastName);
	}

	public final String getCompanyName() {
		return companyName;
	}

	public final void setCompanyName(String companyName) {
		if (!Strings.isNullOrEmpty(companyName) && companyName.length() > 50) {
			throw new IllegalArgumentException(sMAX_CHARS);
		}

		this.companyName = companyName;
		mappings.put(Account.Tags.COMPANY_NAME, companyName);
	}

	public final String getVatNumber() {
		return vatNumber;
	}

	public final void setVatNumber(String vatNumber) {
		this.vatNumber = vatNumber;
		mappings.put(Account.Tags.VAT_NUMBER, vatNumber);
	}

	public final boolean isTaxExempt() {
		return taxExempt;
	}

	public final String getAcceptLanguage() {
		return acceptLanguage;
	}

	public final void setAcceptLanguage(String acceptLanguage) {
		this.acceptLanguage = acceptLanguage;
		mappings.put(Account.Tags.ACCEPT_LANGUAGE, acceptLanguage);
	}

	public final String getBillingTokenID() {
		return billingTokenID;
	}

	public final void setBillingTokenID(String billingTokenID) {
		this.billingTokenID = billingTokenID;
		BillingInfo info = new BillingInfo();
		info.setTokenID(billingTokenID);
		mappings.put(Account.Tags.BILLING_INFO, info);
	}

	public final Address getAddress() {
		return address;
	}

	public final void setAddress(Address address) {
		this.address = address;
		mappings.put(Address.Tags.TAG_NAME, address);
	}
	
	public String getEntityUseCode() {
		return this.entityUseCode;
	}

	public final Boolean getTaxExempt() {
		return taxExempt;
	}

	public final void setTaxExempt(Boolean taxExempt) {
		this.taxExempt = taxExempt;
		if (taxExempt) {
			mappings.put(Account.Tags.TAX_EXEMPT, String.valueOf(taxExempt));
		}
	}

	public final void setEntityUseCode(String entityUseCode) {
		this.entityUseCode = entityUseCode;
		mappings.put(Account.Tags.ENTITY_USE_CODE, this.entityUseCode);
	}
}

