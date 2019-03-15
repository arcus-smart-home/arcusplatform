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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecurlyErrors extends RecurlyModels<RecurlyError> {
   private static final long serialVersionUID = 1L;
   
   private final List<TransactionError> transactionErrors = new ArrayList<>();
   private final List<Transaction> transactions = new ArrayList<>();
   
   public static class Tags {
      public static final String TAG_NAME = "errors";
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   public boolean hasTransactionError() {
      return !transactionErrors.isEmpty();
   }
   
   public List<TransactionError> getTransactionErrors() {
      return Collections.unmodifiableList(transactionErrors);
   }
   
   public void addTransactionError(TransactionError transactionError) {
      transactionErrors.add(transactionError);
   }
   
   public List<Transaction> getTransactions() {
      return Collections.unmodifiableList(transactions);
   }
   
   public void addTransaction(Transaction transaction) {
      transactions.add(transaction);
   }
}

