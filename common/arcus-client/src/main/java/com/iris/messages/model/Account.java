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
package com.iris.messages.model;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;

public class Account extends BaseEntity<UUID, Account> {

  	private String state;

   public static interface AccountState {
      public static final String SIGN_UP_1 = "signUp1";
      public static final String SIGN_UP_2 = "signUp2";
      public static final String COMPLETE = "COMPLETE";
      public static final String ABOUT_YOU = "ABOUT_YOU";
      public static final String ABOUT_YOUR_HOME = "ABOUT_YOUR_HOME";
      public static final String SECURITY_QUESTIONS = "SECURITY_QUESTIONS";
      public static final String PREMIUM_PLAN = "PREMIUM_PLAN";
      public static final String BILLING_INFO = "BILLING_INFO";
      
   }

	private boolean billable = true;
	private boolean taxExempt = false;
	private String billingFirstName;
	private String billingLastName;
	private String billingCCType;
	private String billingCCLast4;
	private String billingStreet1;
	private String billingStreet2;
	private String billingCity;
	private String billingState;
	private String billingZip;
	private String billingZipPlusFour;
	private Map<ServiceLevel, String> subscriptionIDs;
	private Set<UUID> placeIDs;
	private UUID owner;
	private Date trialEnd;

	public boolean        isBillable()       { return billable; }
   public String         getState()         { return state; }

	public boolean getTaxExempt() {
      return taxExempt;
   }

   public String getBillingFirstName() {
      return billingFirstName;
   }

   public String getBillingLastName() {
      return billingLastName;
   }

   public String getBillingCCType() {
      return billingCCType;
   }

   public String getBillingCCLast4() {
      return billingCCLast4;
   }

   public String getBillingStreet1() {
      return billingStreet1;
   }

   public String getBillingStreet2() {
      return billingStreet2;
   }

   public String getBillingCity() {
      return billingCity;
   }

   public String getBillingState() {
      return billingState;
   }

   public String getBillingZip() {
      return billingZip;
   }

   public String getBillingZipPlusFour() {
      return billingZipPlusFour;
   }

   public Map<ServiceLevel, String> getSubscriptionIDs() {
   	if (subscriptionIDs == null) {
   		return Collections.<ServiceLevel, String>emptyMap();
   	}

   	return subscriptionIDs;
   }

   public Set<UUID> getPlaceIDs() {
   	if (placeIDs == null) {
   		return Collections.<UUID>emptySet();
   	}

   	return placeIDs;
   }

   public UUID getOwner() {
      return owner;
   }

   public void setBillable(boolean billable)               { this.billable = billable; }
   public void setState(String state)                      { this.state = state; }

   public void setTaxExempt(boolean taxExempt) {
      this.taxExempt = taxExempt;
   }

   public void setBillingFirstName(String billingFirstName) {
      this.billingFirstName = billingFirstName;
   }

   public void setBillingLastName(String billingLastName) {
      this.billingLastName = billingLastName;
   }

   public void setBillingCCType(String billingCCType) {
      this.billingCCType = billingCCType;
   }

   public void setBillingCCLast4(String billingCCLast4) {
      this.billingCCLast4 = billingCCLast4;
   }

   public void setBillingStreet1(String billingStreet1) {
      this.billingStreet1 = billingStreet1;
   }

   public void setBillingStreet2(String billingStreet2) {
      this.billingStreet2 = billingStreet2;
   }

   public void setBillingCity(String billingCity) {
      this.billingCity = billingCity;
   }

   public void setBillingState(String billingState) {
      this.billingState = billingState;
   }

   public void setBillingZip(String billingZip) {
      this.billingZip = billingZip;
   }

   public void setBillingZipPlusFour(String billingZipPlusFour) {
      this.billingZipPlusFour = billingZipPlusFour;
   }

   public void setSubscriptionIDs(Map<ServiceLevel, String> subscriptionIDs) {
   	this.subscriptionIDs = subscriptionIDs;
   }

   public void setPlaceIDs(Set<UUID> placeIDs) {
   	this.placeIDs = placeIDs;
   }

   public void setOwner(UUID owner) {
      this.owner = owner;
   }

   @Override
   public String getType() {
      return PlatformConstants.SERVICE_ACCOUNTS;
   }

   @Override
   public String getAddress() {
      return MessageConstants.SERVICE + ":" + PlatformConstants.SERVICE_ACCOUNTS + ":" + getId();
   }

   @Override
   public Set<String> getCaps() {
      return ImmutableSet.of("base", getType());
   }

	public Date getTrialEnd() {
      return trialEnd;
   }

   public void setTrialEnd(Date trialEnd) {
      this.trialEnd = trialEnd;
   }

   public boolean hasBillingAccount() {
      return !StringUtils.isBlank(billingCCLast4);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (billable ? 1231 : 1237);
      result = prime * result
            + ((billingCCLast4 == null) ? 0 : billingCCLast4.hashCode());
      result = prime * result
            + ((billingCCType == null) ? 0 : billingCCType.hashCode());
      result = prime * result
            + ((billingCity == null) ? 0 : billingCity.hashCode());
      result = prime * result
            + ((billingFirstName == null) ? 0 : billingFirstName.hashCode());
      result = prime * result
            + ((billingLastName == null) ? 0 : billingLastName.hashCode());
      result = prime * result
            + ((billingState == null) ? 0 : billingState.hashCode());
      result = prime * result
            + ((billingStreet1 == null) ? 0 : billingStreet1.hashCode());
      result = prime * result
            + ((billingStreet2 == null) ? 0 : billingStreet2.hashCode());
      result = prime * result
            + ((billingZip == null) ? 0 : billingZip.hashCode());
      result = prime
            * result
            + ((billingZipPlusFour == null) ? 0 : billingZipPlusFour.hashCode());
      result = prime * result + ((owner == null) ? 0 : owner.hashCode());
      result = prime * result + ((placeIDs == null) ? 0 : placeIDs.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result
            + ((subscriptionIDs == null) ? 0 : subscriptionIDs.hashCode());
      result = prime * result + (taxExempt ? 1231 : 1237);
      result = prime * result + ((trialEnd == null) ? 0 : trialEnd.hashCode());
      return result;
   }

	@Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      Account other = (Account) obj;
      if (billable != other.billable)
         return false;
      if (billingCCLast4 == null) {
         if (other.billingCCLast4 != null)
            return false;
      } else if (!billingCCLast4.equals(other.billingCCLast4))
         return false;
      if (billingCCType == null) {
         if (other.billingCCType != null)
            return false;
      } else if (!billingCCType.equals(other.billingCCType))
         return false;
      if (billingCity == null) {
         if (other.billingCity != null)
            return false;
      } else if (!billingCity.equals(other.billingCity))
         return false;
      if (billingFirstName == null) {
         if (other.billingFirstName != null)
            return false;
      } else if (!billingFirstName.equals(other.billingFirstName))
         return false;
      if (billingLastName == null) {
         if (other.billingLastName != null)
            return false;
      } else if (!billingLastName.equals(other.billingLastName))
         return false;
      if (billingState == null) {
         if (other.billingState != null)
            return false;
      } else if (!billingState.equals(other.billingState))
         return false;
      if (billingStreet1 == null) {
         if (other.billingStreet1 != null)
            return false;
      } else if (!billingStreet1.equals(other.billingStreet1))
         return false;
      if (billingStreet2 == null) {
         if (other.billingStreet2 != null)
            return false;
      } else if (!billingStreet2.equals(other.billingStreet2))
         return false;
      if (billingZip == null) {
         if (other.billingZip != null)
            return false;
      } else if (!billingZip.equals(other.billingZip))
         return false;
      if (billingZipPlusFour == null) {
         if (other.billingZipPlusFour != null)
            return false;
      } else if (!billingZipPlusFour.equals(other.billingZipPlusFour))
         return false;
      if (owner == null) {
         if (other.owner != null)
            return false;
      } else if (!owner.equals(other.owner))
         return false;
      if (placeIDs == null) {
         if (other.placeIDs != null)
            return false;
      } else if (!placeIDs.equals(other.placeIDs))
         return false;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (subscriptionIDs == null) {
         if (other.subscriptionIDs != null)
            return false;
      } else if (!subscriptionIDs.equals(other.subscriptionIDs))
         return false;
      if (taxExempt != other.taxExempt)
         return false;
      if (trialEnd == null) {
         if (other.trialEnd != null)
            return false;
      } else if (!trialEnd.equals(other.trialEnd))
         return false;
      return true;
   }

	@Override
   public String toString() {
      return "Account [state=" + state + ", billable=" + billable
            + ", taxExempt=" + taxExempt + ", billingFirstName="
            + billingFirstName + ", billingLastName=" + billingLastName
            + ", billingCCType=" + billingCCType + ", billingCCLast4="
            + billingCCLast4 + ", billingStreet1=" + billingStreet1
            + ", billingStreet2=" + billingStreet2 + ", billingCity="
            + billingCity + ", billingState=" + billingState + ", billingZip="
            + billingZip + ", billingZipPlusFour=" + billingZipPlusFour
            + ", subscriptionIDs=" + subscriptionIDs + ", placeIDs=" + placeIDs
            + ", owner=" + owner + ", trialEnd=" + trialEnd + "]";
   }
	@Override
   protected Object clone() throws CloneNotSupportedException {
      Account account = (Account) super.clone();
      account.trialEnd = this.trialEnd == null ? null : (Date) this.trialEnd.clone();
      return account;
   }
}

