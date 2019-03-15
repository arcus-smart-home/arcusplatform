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

import com.iris.messages.model.Copyable;

public class Transaction extends RecurlyModel implements Copyable<Transaction> {
   public static final String TRANSACTION_TYPE_PURCHASE = "purchase";
   public static final String TRANSACTION_TYPE_REFUND = "refund";
   public static final String TRANSACTION_TYPE_VERIFY = "verify";
   
   public static final String TRANSACTION_STATUS_SUCCESS = "success";
   public static final String TRANSACTION_STATUS_DECLINED = "declined";
   public static final String TRANSACTION_STATUS_VOID = "void";
   
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/transactions";

      public static final String TAG_NAME = "transaction";

      public static final String TRANSACTION_ID = "uuid";
      public static final String ACTION = "action";
      public static final String AMOUNT_IN_CENTS = "amount_in_cents";
      public static final String TAX_IN_CENTS = "tax_in_cents";
      public static final String CURRENCY = "currency";
      public static final String STATUS = "status";
      public static final String PAYMENT_METHOD = "payment_method";
      public static final String   REFERENCE = "reference";
      public static final String SOURCE = "source";
      public static final String RECURRING = "recurring";
      public static final String IS_TEST = "test";
      public static final String VOIDABLE = "voidable";
      public static final String   REFUNDABLE = "refundable";
      public static final String   CVV_RESULT = "cvv_result";
      public static final String   CVV_RESULT_CODE = "code";
      public static final String AVS_RESULT = "avs_result";
      public static final String AVS_RESULT_CODE = "code";
      public static final String AVS_RESULT_STREET = "avs_result_street";
      public static final String AVS_RESULT_POSTAL = "avs_result_postal";
      public static final String CREATED_AT = "created_at";
      public static final String TRANSACTION_ERROR = "transaction_error";
      public static final String DETAILS = "details";

      public static final String A_NAME_REFUND = "refund";
   }

   private String transactionID;
   private String action;
   private String amountInCents;
   private String taxInCents;
   private String currency;
   private String status;
   private String paymentMethod;
   private String reference;
   private String source;
   private String recurring;
   private String testPayment;
   private String voidable;
   private String refundable;
   private String cvvResult;
   private String avsResult;
   private String cvvResultCode;
   private String avsResultCode;
   private String avsResultStreet;
   private String avsResultPostal;
   private Date createdAt;
   private Account account;
   private TransactionError transactionError;

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      Transactions transactions = new Transactions();
      transactions.add(this);
      return transactions;
   }

   public final String getTransactionID() {
      return transactionID;
   }

   public final void setTransactionID(String transactionID) {
      this.transactionID = transactionID;
   }

   public final String getAction() {
      return action;
   }

   public final void setAction(String action) {
      this.action = action;
   }

   public final String getAmountInCents() {
      return amountInCents;
   }

   public final void setAmountInCents(String amountInCents) {
      this.amountInCents = amountInCents;
   }

   public final String getTaxInCents() {
      return taxInCents;
   }

   public final void setTaxInCents(String taxInCents) {
      this.taxInCents = taxInCents;
   }

   public final String getCurrency() {
      return currency;
   }

   public final void setCurrency(String currency) {
      this.currency = currency;
   }

   public final String getStatus() {
      return status;
   }

   public final void setStatus(String status) {
      this.status = status;
   }

   public final String getPaymentMethod() {
      return paymentMethod;
   }

   public final void setPaymentMethod(String paymentMethod) {
      this.paymentMethod = paymentMethod;
   }

   public final String getReference() {
      return reference;
   }

   public final void setReference(String reference) {
      this.reference = reference;
   }

   public final String getSource() {
      return source;
   }

   public final void setSource(String source) {
      this.source = source;
   }

   public final String getRecurring() {
      return recurring;
   }

   public final void setRecurring(String recurring) {
      this.recurring = recurring;
   }

   public final String getTestPayment() {
      return testPayment;
   }

   public final void setTestPayment(String testPayment) {
      this.testPayment = testPayment;
   }

   public final String getVoidable() {
      return voidable;
   }

   public final void setVoidable(String voidable) {
      this.voidable = voidable;
   }

   public final String getRefundable() {
      return refundable;
   }

   public final void setRefundable(String refundable) {
      this.refundable = refundable;
   }

   public final String getCvvResult() {
      return cvvResult;
   }

   public final void setCvvResult(String cvvResult) {
      this.cvvResult = cvvResult;
   }

   public final String getAvsResult() {
      return avsResult;
   }

   public final void setAvsResult(String avsResult) {
      this.avsResult = avsResult;
   }

   public final String getAvsResultStreet() {
      return avsResultStreet;
   }

   public final void setAvsResultStreet(String avsResultStreet) {
      this.avsResultStreet = avsResultStreet;
   }

   public final String getAvsResultPostal() {
      return avsResultPostal;
   }

   public final void setAvsResultPostal(String avsResultPostal) {
      this.avsResultPostal = avsResultPostal;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public final Account getAccount() {
      return account;
   }

   public final void setAccount(Account account) {
      this.account = account;
   }

   public final TransactionError getTransactionError() {
      return transactionError;
   }

   public final void setTransactionError(TransactionError transactionError) {
      this.transactionError = transactionError;
   }

   public final String getCvvResultCode() {
      return cvvResultCode;
   }

   public final void setCvvResultCode(String cvvResultCode) {
      this.cvvResultCode = cvvResultCode;
   }

   public final String getAvsResultCode() {
      return avsResultCode;
   }

   public final void setAvsResultCode(String avsResultCode) {
      this.avsResultCode = avsResultCode;
   }

   @Override
   public Transaction copy() {
      try {
         return (Transaction)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Transaction clone = new Transaction();
      clone.setTransactionID(transactionID);
      clone.setAction(action);
      clone.setAmountInCents(amountInCents);
      clone.setTaxInCents(taxInCents);
      clone.setCurrency(currency);
      clone.setStatus(status);
      clone.setPaymentMethod(paymentMethod);
      clone.setReference(reference);
      clone.setSource(source);
      clone.setRecurring(recurring);
      clone.setTestPayment(testPayment);
      clone.setVoidable(voidable);
      clone.setRefundable(refundable);
      clone.setCvvResult(cvvResult);
      clone.setAvsResult(avsResult);
      clone.setCvvResultCode(cvvResultCode);
      clone.setAvsResultCode(avsResultCode);
      clone.setAvsResultStreet(avsResultStreet);
      clone.setAvsResultPostal(avsResultPostal);
      clone.setCreatedAt((Date)createdAt.clone());
      clone.setAccount(account.copy());
      clone.setTransactionError(transactionError.copy());
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Transaction [transactionID=").append(transactionID)
      .append(", action=").append(action).append(", amountInCents=")
      .append(amountInCents).append(", taxInCents=").append(taxInCents)
      .append(", currency=").append(currency).append(", status=")
      .append(status).append(", paymentMethod=").append(paymentMethod)
      .append(", reference=").append(reference).append(", source=")
      .append(source).append(", recurring=").append(recurring)
      .append(", testPayment=").append(testPayment).append(", voidable=")
      .append(voidable).append(", refundable=").append(refundable)
      .append(", cvvResult=").append(cvvResult).append(", avsResult=")
      .append(avsResult).append(", cvvResultCode=").append(cvvResultCode)
      .append(", avsResultCode=").append(avsResultCode)
      .append(", avsResultStreet=").append(avsResultStreet)
      .append(", avsResultPostal=").append(avsResultPostal)
      .append(", createdAt=").append(createdAt).append(", account=")
      .append(account).append(", transactionError=")
      .append(transactionError).append("]");
      return builder.toString();
   }
}

