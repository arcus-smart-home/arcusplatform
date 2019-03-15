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
package com.iris.core.dao;
import java.util.UUID;
import java.util.stream.Stream;

import com.iris.messages.model.Account;
import com.iris.platform.PagedQuery;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;

public interface AccountDAO extends CRUDDao<UUID, Account> {

   ModelEntity findAccountModelById(UUID id);
   Account create(Account account);

   PagedResults<Account> listAccounts(AccountQuery query);
   public static class AccountQuery extends PagedQuery { }
   public Stream<Account> streamAll();
}

