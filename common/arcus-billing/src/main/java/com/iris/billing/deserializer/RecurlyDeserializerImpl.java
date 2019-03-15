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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.AccountBalance;
import com.iris.billing.client.model.AccountNote;
import com.iris.billing.client.model.AccountNotes;
import com.iris.billing.client.model.Accounts;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.BaseRecurlyModel;
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
import com.iris.billing.client.model.SubscriptionAddons;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.TaxDetail;
import com.iris.billing.client.model.TaxDetails;
import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.Transactions;

class RecurlyDeserializerImpl extends AbstractRecurlyDeserializer<BaseRecurlyModel> {
   private Logger logger = LoggerFactory.getLogger(RecurlyDeserializerImpl.class);
   private volatile AbstractRecurlyDeserializer<?> deserializer;
   private volatile BaseRecurlyModel result;

   public RecurlyDeserializerImpl() {
      super(null, null);
   }

   @Override
   protected BaseRecurlyModel getResult() {
      return result;
   }
   
   @Override
   protected void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer) {  
   }
   
   @Override
   public void redirectStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      logger.trace("[{}] Processing Element: [{}]", getClass().getSimpleName(), qName);

      if (deserializer == null) {
         switch (qName) {
            case Account.Tags.TAG_NAME:
               deserializer = new AccountDeserializer(getParser());
               break;
            case Accounts.Tags.TAG_NAME:
               deserializer = new AccountsDeserializer(getParser());
               break;
            case AccountNote.Tags.TAG_NAME:
               deserializer = new AccountNoteDeserializer(getParser());
               break;
            case AccountNotes.Tags.TAG_NAME:
               deserializer = new AccountNotesDeserializer(getParser());
               break;
            case AccountBalance.Tags.TAG_NAME:
               deserializer = new AccountBalanceDeserializer(getParser());
               break;
            case Adjustment.Tags.TAG_NAME:
               deserializer = new AdjustmentDeserializer(getParser(), attributes);
               break;
            case Adjustments.Tags.TAG_NAME:
               deserializer = new AdjustmentsDeserializer(getParser(), Adjustments.Tags.TAG_NAME);
               break;
            case Invoice.Tags.LINE_ITEMS:
               deserializer = new AdjustmentsDeserializer(getParser(), Invoice.Tags.LINE_ITEMS);
               break;
            case BillingInfo.Tags.TAG_NAME:
               deserializer = new BillingInfoDeserializer(getParser());
               break;
            case Invoice.Tags.TAG_NAME:
               deserializer = new InvoiceDeserializer(getParser(), attributes);
               break;
            case Invoices.Tags.TAG_NAME:
               deserializer = new InvoicesDeserializer(getParser());
               break;
            case RecurlyError.Tags.TAG_NAME:
               deserializer = new ErrorDeserializer(getParser(), attributes);
               break;
            case RecurlyErrors.Tags.TAG_NAME:
               deserializer = new ErrorsDeserializer(getParser());
               break;
            case PlanAddon.Tags.TAG_NAME:
               deserializer = new PlanAddonDeserializer(getParser());
               break;
            case PlanAddons.Tags.TAG_NAME:
               deserializer = new PlanAddonsDeserializer(getParser());
               break;
            case Plan.Tags.TAG_NAME:
               deserializer = new PlanDeserializer(getParser());
               break;
            case Plans.Tags.TAG_NAME:
               deserializer = new PlansDeserializer(getParser());
               break;
            case SubscriptionAddon.Tags.TAG_NAME:
               deserializer = new SubscriptionAddonDeserializer(getParser());
               break;
            case SubscriptionAddons.Tags.TAG_NAME:
               deserializer = new SubscriptionAddonsDeserializer(getParser());
               break;
            case Subscription.Tags.TAG_NAME:
               deserializer = new SubscriptionDeserializer(getParser());
               break;
            case Subscriptions.Tags.TAG_NAME:
               deserializer = new SubscriptionsDeserializer(getParser(), Subscriptions.Tags.TAG_NAME);
               break;
            case TaxDetail.Tags.TAG_NAME:
               deserializer = new TaxDetailDeserializer(getParser());
               break;
            case TaxDetails.Tags.TAG_NAME:
               deserializer = new TaxDetailsDeserializer(getParser());
               break;
            case Transaction.Tags.TAG_NAME:
               deserializer = new TransactionDeserializer(getParser());
               break;
            case Transactions.Tags.TAG_NAME:
               deserializer = new TransactionsDeserializer(getParser());
               break;
            default:
               // No-Op
               break;
         }

         // Checking to see we were able to create an instance of a specific class.
         if (deserializer != null) {
            // Prime the target for the first tag.
            deserializer.onStartElement(uri, localName, qName, attributes);
            deserializer.setParentHandler(this);
            setContentHandler(deserializer);
         } else {
            throw new UnknownError("Error parsing " + getRequestedClass() + " - No Deserializers found for Tag: " + qName);
         }
      } else {
         // We're already primed, send it back since we're not at the end of the document.
         deserializer.onStartElement(uri, localName, qName, attributes);
         deserializer.setParentHandler(this);
         setContentHandler(deserializer);
      }
   }

   @Override
   protected void onStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // No-Op, This class is used to pass the stream to the correct parser to simplify deserialization
   }

   @Override
   protected void onEndElement(String uri, String localName, String qName) throws SAXException {
      // No-Op, This class is used to pass the stream to the correct parser to simplify deserialization
   }
   
   @Override
   public void endDocument() {
      if (deserializer != null) {
         result = deserializer.castResult(getRequestedClass());
      }
   }
}

