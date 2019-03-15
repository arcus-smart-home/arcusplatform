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
package com.iris.core.dao.file;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.messages.model.Account;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;

@Singleton
public class AccountDAOFileImpl extends BaseFileDAOImpl<UUID, Account> implements AccountDAO {

   public AccountDAOFileImpl() {
      super("accountdata.json");
   }

   @Override
   public Account create(Account account) {
      UUID id = account.getId();
      Date created = account.getCreated();
      if(id == null) {
         id = nextId();
      }
      if(created == null) {
         created = new Date();
      }
      return save(account);
   }

   @Override
   protected Account[] parseJSON(Gson gson, String json) throws IOException {
      return gson.fromJson(json, Account[].class);
   }

   @Override
   protected UUID nextId() {
      return UUID.randomUUID();
   }

   @Override
   public ModelEntity findAccountModelById(UUID id) {
      throw new UnsupportedOperationException();
   }

   @Override
   public PagedResults<Account> listAccounts(AccountQuery query) {
	   throw new RuntimeException("Operation not supported on AccountDAOFileImpl"); 
   }
   
   @Override
   public Stream<Account> streamAll() {
	   throw new RuntimeException("Operation not supported on AccountDAOFileImpl");
   }

}

