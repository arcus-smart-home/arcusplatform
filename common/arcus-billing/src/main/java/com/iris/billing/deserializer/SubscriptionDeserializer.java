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
package com.iris.billing.deserializer;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Plan;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.SubscriptionAddons;
import com.iris.billing.client.model.Subscriptions;

class SubscriptionDeserializer extends AbstractRecurlyDeserializer<Subscription> {
	private Subscription subscription;

	public SubscriptionDeserializer() {
		this(null);
	}

	SubscriptionDeserializer(SAXParser saxParser) {
		super(saxParser, Subscription.Tags.TAG_NAME);
		subscription = new Subscription();
	}

	@Override
	protected Subscription getResult() {
		return subscription;
	}
	
	@Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {
      if (childDeserializer instanceof PlanDeserializer) {
         subscription.setPlanDetails((Plan)childDeserializer.getResult());
      }
      else if (childDeserializer instanceof SubscriptionsDeserializer) {
         Subscriptions subscriptions = (Subscriptions)childDeserializer.getResult();
         subscription.setPendingSubscription((subscriptions != null && !subscriptions.isEmpty()) ? subscriptions.get(0) : null);
      }
      else if (childDeserializer instanceof SubscriptionAddonsDeserializer) {
         subscription.setSubscriptionAddOns((SubscriptionAddons)childDeserializer.getResult());
      }
   }

	@Override
	public void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		switch(qName) {
			case SubscriptionAddons.Tags.TAG_NAME:
				SubscriptionAddonsDeserializer subscriptionAddonsDeserializer = new SubscriptionAddonsDeserializer(getParser());
				subscriptionAddonsDeserializer.setParentHandler(this);
				setContentHandler(subscriptionAddonsDeserializer);
				break;
			case Subscription.Tags.PENDING_SUBSCRIPTION:
				SubscriptionsDeserializer subscriptionsDeserializer 
				   = new SubscriptionsDeserializer(getParser(), Subscription.Tags.PENDING_SUBSCRIPTION);
				subscriptionsDeserializer.setParentHandler(this);
				setContentHandler(subscriptionsDeserializer);
				break;
			case Plan.Tags.TAG_NAME:
				PlanDeserializer planDeserializer = new PlanDeserializer(getParser());
				planDeserializer.setParentHandler(this);
				setContentHandler(planDeserializer);
				break;

				// HREF's
			case Account.Tags.TAG_NAME:
			case Invoice.Tags.TAG_NAME:
				break;
			default:
            // No-Op
            break;
		}
	}

	@Override
	public void onEndElement (String uri, String localName, String qName) throws SAXException {
		switch(qName) {
				// Subscription Details
			case Subscription.Tags.UUID:
				subscription.setSubscriptionID(getCurrentTextValue());
				break;
			case Subscription.Tags.STATE:
				subscription.setState(getCurrentTextValue());
				break;
			case Subscription.Tags.UNIT_AMOUNT_IN_CENTS:
					subscription.setUnitAmountInCents(getCurrentTextValue());
				break;
			case Subscription.Tags.QUANTITY:
					subscription.setQuantity(getCurrentTextValue());
				break;
			case Subscription.Tags.CURRENCY:
				subscription.setCurrency(getCurrentTextValue());
				break;
			case Subscription.Tags.ACTIVATED_AT:
				subscription.setActivatedAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.CANCELED_AT:
				subscription.setCanceledAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.EXPIRES_AT:
				subscription.setExpiresAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.CURRENT_PERIOD_STARTED_AT:
				subscription.setCurrentPeriodStartedAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.CURRENT_PERIOD_ENDS_AT:
				subscription.setCurrentPeriodEndsAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.TRIAL_STARTED_AT:
				subscription.setTrialStartedAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.TRIAL_ENDS_AT:
				subscription.setTrialEndsAt(getDateFromString(getCurrentTextValue()));
				break;
			case Subscription.Tags.TAX_IN_CENTS:
				subscription.setTaxInCents(getCurrentTextValue());
				break;
			case Subscription.Tags.TAX_TYPE:
				subscription.setTaxType(getCurrentTextValue());
				break;
			case Subscription.Tags.TAX_REGION:
				subscription.setTaxRegion(getCurrentTextValue());
				break;
			case Subscription.Tags.TAX_RATE:
				subscription.setTaxRate(getCurrentTextValue());
				break;
			case Subscription.Tags.PO_NUMBER:
				subscription.setPoNumber(getCurrentTextValue());
				break;
			case Subscription.Tags.NET_TERMS:
				subscription.setNetTerms(getCurrentTextValue());
				break;
			case Subscription.Tags.COLLECTION_METHOD:
				subscription.setCollectionMethod(getCurrentTextValue());
				break;
			case Subscription.Tags.TERMS_AND_CONDITIONS:
				subscription.setTermsAndConditions(getCurrentTextValue());
				break;
			case Subscription.Tags.CUSTOMER_NOTES:
				subscription.setCustomerNotes(getCurrentTextValue());
				break;
			default:
				// No-Op
				break;
		}
	}
}

