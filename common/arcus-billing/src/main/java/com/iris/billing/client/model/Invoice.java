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
package com.iris.billing.client.model;

import java.util.Date;
import java.util.List;

import com.iris.messages.model.Copyable;
import com.iris.util.IrisCollections;

public class Invoice extends RecurlyModel implements Copyable<Invoice> {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/invoices";

      public static final String TAG_NAME = "invoice";

      public static final String INVOICE_ID = "uuid";
      public static final String STATE = "state";
      public static final String INVOICE_NUMBER = "invoice_number";
      public static final String INVOICE_NUM_PREFIX = "invoice_number_prefix";
      public static final String PO_NUM = "po_number";
      public static final String VAT_NUM = "vat_number";
      public static final String SUBTOTAL_IN_CENTS = "subtotal_in_cents";
      public static final String TAX_IN_CENTS = "tax_in_cents";
      public static final String TOTAL_IN_CENTS = "total_in_cents";
      public static final String CURRENCY = "currency";
      public static final String CREATED = "created_at";
      public static final String CLOSED = "closed_at";
      public static final String TAX_TYPE = "tax_type";
      public static final String TAX_REGION = "tax_region";
      public static final String TAX_RATE = "tax_rate";
      public static final String NET_TERMS = "net_terms";
      public static final String COLLECTION_METHOD = "collection_method";
      public static final String LINE_ITEMS = "line_items";
   }
   
   public static class Attributes {
      private Attributes() {}
      public static final String HREF = "href";
   }

   private String invoiceID;
   private String state;
   private String invoiceNumber;
   private String poNumber;
   private String vatNumber;
   private String subtotalInCents;
   private String taxInCents;
   private String totalInCents;

   private String currency;
   private Date createdAt;
   private Date closedAt;
   private String taxType;
   private String taxRegion;
   private String taxRate;
   private String netTerms;
   private String collectionMethod;
   private List<Adjustment> adjustments;
   private List<Transaction> transactions;
   private String url;

   private String invoiceNumberPrefix;
   private Address address;

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      Invoices invoices = new Invoices();
      invoices.add(this);
      return invoices;
   }


   public final String getInvoiceID() {
      return invoiceID;
   }

   public final void setInvoiceID(String invoiceID) {
      this.invoiceID = invoiceID;
   }

   public final String getState() {
      return state;
   }

   public final void setState(String state) {
      this.state = state;
   }

   public final String getInvoiceNumber() {
      return invoiceNumber;
   }

   public final void setInvoiceNumber(String invoiceNumber) {
      this.invoiceNumber = invoiceNumber;
   }

   public final String getPoNumber() {
      return poNumber;
   }

   public final void setPoNumber(String poNumber) {
      this.poNumber = poNumber;
   }

   public final String getVatNumber() {
      return vatNumber;
   }

   public final void setVatNumber(String vatNumber) {
      this.vatNumber = vatNumber;
   }

   public final String getSubtotalInCents() {
      return subtotalInCents;
   }

   public final void setSubtotalInCents(String subtotalInCents) {
      this.subtotalInCents = subtotalInCents;
   }

   public final String getTaxInCents() {
      return taxInCents;
   }

   public final void setTaxInCents(String taxInCents) {
      this.taxInCents = taxInCents;
   }

   public final String getTotalInCents() {
      return totalInCents;
   }

   public final void setTotalInCents(String totalInCents) {
      this.totalInCents = totalInCents;
   }

   public final String getCurrency() {
      return currency;
   }

   public final void setCurrency(String currency) {
      this.currency = currency;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public final Date getClosedAt() {
      return closedAt;
   }

   public final void setClosedAt(Date closedAt) {
      this.closedAt = closedAt;
   }

   public final String getTaxType() {
      return taxType;
   }

   public final void setTaxType(String taxType) {
      this.taxType = taxType;
   }

   public final String getTaxRegion() {
      return taxRegion;
   }

   public final void setTaxRegion(String taxRegion) {
      this.taxRegion = taxRegion;
   }

   public final String getTaxRate() {
      return taxRate;
   }

   public final void setTaxRate(String taxRate) {
      this.taxRate = taxRate;
   }

   public final String getNetTerms() {
      return netTerms;
   }

   public final void setNetTerms(String netTerms) {
      this.netTerms = netTerms;
   }

   public final String getCollectionMethod() {
      return collectionMethod;
   }

   public final void setCollectionMethod(String collectionMethod) {
      this.collectionMethod = collectionMethod;
   }

   public final List<Adjustment> getAdjustments() {
      return adjustments;
   }

   public final void setAdjustments(List<Adjustment> adjustments) {
      this.adjustments = adjustments;
   }

   public final String getInvoiceNumberPrefix() {
      return invoiceNumberPrefix;
   }

   public final void setInvoiceNumberPrefix(String invoiceNumberPrefix) {
      this.invoiceNumberPrefix = invoiceNumberPrefix;
   }

   public final Address getAddress() {
      return address;
   }

   public final void setAddress(Address address) {
      this.address = address;
   }

   public final List<Transaction> getTransactions() {
      return transactions;
   }

   public final void setTransactions(List<Transaction> transactions) {
      this.transactions = transactions;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   @Override
   public Invoice copy() {
      try {
         return (Invoice)this.clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Invoice clone = new Invoice();
      clone.setInvoiceID(invoiceID);
      clone.setState(state);
      clone.setInvoiceNumber(invoiceNumber);
      clone.setPoNumber(poNumber);
      clone.setVatNumber(vatNumber);
      clone.setSubtotalInCents(subtotalInCents);
      clone.setTaxInCents(taxInCents);
      clone.setTotalInCents(subtotalInCents);
      clone.setCurrency(currency);
      clone.setCreatedAt((Date)createdAt.clone());
      clone.setClosedAt((Date)closedAt.clone());
      clone.setTaxType(taxType);
      clone.setTaxRegion(taxRegion);
      clone.setTaxRate(taxRate);
      clone.setNetTerms(netTerms);
      clone.setCollectionMethod(collectionMethod);
      clone.setAdjustments(IrisCollections.deepCopyList(adjustments));
      clone.setTransactions(IrisCollections.deepCopyList(transactions));
      clone.setUrl(url);
      clone.setInvoiceNumberPrefix(invoiceNumberPrefix);
      clone.setAddress(address.copy());
      return clone;
   }

   @Override
   public String toString() {
      return "Invoice [invoiceID=" + invoiceID + ", state=" + state
            + ", invoiceNumber=" + invoiceNumber + ", poNumber=" + poNumber
            + ", vatNumber=" + vatNumber + ", subtotalInCents="
            + subtotalInCents + ", taxInCents=" + taxInCents
            + ", totalInCents=" + totalInCents + ", currency=" + currency
            + ", createdAt=" + createdAt + ", closedAt=" + closedAt
            + ", taxType=" + taxType + ", taxRegion=" + taxRegion
            + ", taxRate=" + taxRate + ", netTerms=" + netTerms
            + ", collectionMethod=" + collectionMethod + ", adjustments="
            + adjustments + ", transactions=" + transactions + ", url=" + url
            + ", invoiceNumberPrefix=" + invoiceNumberPrefix + ", address="
            + address + "]";
   }
}

