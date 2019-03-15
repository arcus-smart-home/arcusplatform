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
package com.iris.core.dao.cassandra;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Account;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.PagedResults;
import com.iris.platform.model.ModelEntity;
import com.iris.util.IrisUUID;

@Singleton
public class AccountDAOImpl extends BaseCassandraCRUDDao<UUID, Account> implements AccountDAO {

   private static final String TABLE = "account";

   private static final Timer listAccountsTimer = DaoMetrics.readTimer(AccountDAO.class, "listAccounts");
   private static final Timer streamAllTimer = DaoMetrics.readTimer(AccountDAO.class, "streamAll");

   static class AccountEntityColumns {
      final static String STATE = "state";
      final static String BILLABLE = "billable";
      final static String TAX_EXEMPT = "taxExempt";
      final static String BILLING_FIRST_NAME = "billingFirstName";
      final static String BILLING_LAST_NAME = "billingLastName";
      final static String BILLING_CC_TYPE = "billingCCType";
      final static String BILLING_CC_LAST4 = "billingCCLast4";
      final static String BILLING_STREET1 = "billingStreet1";
      final static String BILLING_STREET2 = "billingStreet2";
      final static String BILLING_CITY = "billingCity";
      final static String BILLING_STATE = "billingState";
      final static String BILLING_ZIP = "billingZip";
      final static String PLACE_IDS = "placeids";
      final static String SUBSCRIPTION_ID_MAP = "subscriptionids";
      final static String BILLING_ZIP_PLUS4 = "billingZipPlusFour";
      final static String OWNER = "owner";
      final static String TRIAL_END = "trialEnd";
   };

   private static final String[] COLUMN_ORDER = {
      AccountEntityColumns.STATE,
      AccountEntityColumns.BILLABLE,
      AccountEntityColumns.TAX_EXEMPT,
      AccountEntityColumns.BILLING_FIRST_NAME,
      AccountEntityColumns.BILLING_LAST_NAME,
      AccountEntityColumns.BILLING_CC_TYPE,
      AccountEntityColumns.BILLING_CC_LAST4,
      AccountEntityColumns.BILLING_STREET1,
      AccountEntityColumns.BILLING_STREET2,
      AccountEntityColumns.BILLING_CITY,
      AccountEntityColumns.BILLING_STATE,
      AccountEntityColumns.BILLING_ZIP,
      AccountEntityColumns.BILLING_ZIP_PLUS4,
      AccountEntityColumns.PLACE_IDS,
      AccountEntityColumns.SUBSCRIPTION_ID_MAP,
      AccountEntityColumns.OWNER,
      AccountEntityColumns.TRIAL_END
   };

   private final PreparedStatement listPaged;
   private final PreparedStatement listAll;

   @Inject
   public AccountDAOImpl(Session session) {
      super(session, TABLE, COLUMN_ORDER);

      listPaged =
              CassandraQueryBuilder
                 .select(TABLE)
                 .addColumns(BASE_COLUMN_ORDER).addColumns(COLUMN_ORDER)
                 .where("token(" + BaseEntityColumns.ID + ") >= token(?) LIMIT ?")
                 .prepare(session);

      listAll = CassandraQueryBuilder
      				.select(TABLE)
      				.addColumns(BASE_COLUMN_ORDER).addColumns(COLUMN_ORDER)
      				.prepare(session);
   }

   @Override
   protected List<Object> getValues(Account entity) {
      List<Object> values = new LinkedList<Object>();
      values.add(entity.getState());
      values.add(entity.isBillable());
      values.add(entity.getTaxExempt());
      values.add(entity.getBillingFirstName());
      values.add(entity.getBillingLastName());
      values.add(entity.getBillingCCType());
      values.add(entity.getBillingCCLast4());
      values.add(entity.getBillingStreet1());
      values.add(entity.getBillingStreet2());
      values.add(entity.getBillingCity());
      values.add(entity.getBillingState());
      values.add(entity.getBillingZip());
      values.add(entity.getBillingZipPlusFour());
      values.add(entity.getPlaceIDs());

      // Add subscription ID's as Map<String, String> to DB
      Map<String, String> subIDs = null;
      if (entity.getSubscriptionIDs() != null && !entity.getSubscriptionIDs().isEmpty()) {
      	subIDs = new HashMap<String, String>();
	      for (Map.Entry<ServiceLevel, String> item : entity.getSubscriptionIDs().entrySet()) {
	      	subIDs.put(item.getKey().name(), item.getValue());
	      }
      }
      values.add(subIDs);
      values.add(entity.getOwner());
      values.add(entity.getTrialEnd());

      return values;
   }

   @Override
   protected Account createEntity() {
      return new Account();
   }

   @Override
   protected void populateEntity(Row row, Account entity) {
      entity.setBillable(row.getBool(AccountEntityColumns.BILLABLE));
      entity.setState(row.getString(AccountEntityColumns.STATE));
      entity.setTaxExempt(row.getBool(AccountEntityColumns.TAX_EXEMPT));
      entity.setBillingFirstName(row.getString(AccountEntityColumns.BILLING_FIRST_NAME));
      entity.setBillingLastName(row.getString(AccountEntityColumns.BILLING_LAST_NAME));
      entity.setBillingCCType(row.getString(AccountEntityColumns.BILLING_CC_TYPE));
      entity.setBillingCCLast4(row.getString(AccountEntityColumns.BILLING_CC_LAST4));
      entity.setBillingStreet1(row.getString(AccountEntityColumns.BILLING_STREET1));
      entity.setBillingStreet2(row.getString(AccountEntityColumns.BILLING_STREET2));
      entity.setBillingCity(row.getString(AccountEntityColumns.BILLING_CITY));
      entity.setBillingState(row.getString(AccountEntityColumns.BILLING_STATE));
      entity.setBillingZip(row.getString(AccountEntityColumns.BILLING_ZIP));
      entity.setBillingZipPlusFour(row.getString(AccountEntityColumns.BILLING_ZIP_PLUS4));
      
      Set<UUID> placeIDs = row.getSet(AccountEntityColumns.PLACE_IDS, UUID.class);
      entity.setPlaceIDs(placeIDs == null || placeIDs.isEmpty() ? null : placeIDs);

      // Convert Subscription ID's to Map<ServiceLevel, String> if not empty
      Map<ServiceLevel, String> subIDs = null;
      Map<String, String> rowMap = row.getMap(AccountEntityColumns.SUBSCRIPTION_ID_MAP, String.class, String.class);
      if (rowMap != null && !rowMap.isEmpty()) {
      	subIDs = new HashMap<ServiceLevel, String>();
      	for (Map.Entry<String, String> item : rowMap.entrySet()) {
      		subIDs.put(ServiceLevel.valueOf(item.getKey()), item.getValue());
      	}
      }
      entity.setSubscriptionIDs(subIDs);
      entity.setOwner(row.getUUID(AccountEntityColumns.OWNER));
      entity.setTrialEnd(row.getDate(AccountEntityColumns.TRIAL_END));
   }

   @Override
   public Account create(Account account) {
      UUID id = account.getId();
      if(id == null) {
         id = nextId(account);
      }
      return doInsert(id, account);
   }

   @Override
  protected UUID getIdFromRow(Row row) {
      return row.getUUID(BaseEntityColumns.ID);
   }

   @Override
   protected UUID nextId(Account account) {
      return UUID.randomUUID();
   }

   @Override
   public ModelEntity findAccountModelById(UUID id) {
      Account account = findById(id);
      return toModel(account);
   }

   private ModelEntity toModel(Account account) {
      if(account == null) {
         return null;
      }
      ModelEntity entity = new ModelEntity(toAttributes(account));
      entity.setCreated(account.getCreated());
      entity.setModified(account.getModified());
      return entity;
   }

   private Map<String,Object> toAttributes(Account account) {
      Map<String,Object> attrs = new HashMap<>();
      setIf(AccountCapability.ATTR_BILLINGCCLAST4, account.getBillingCCLast4(), attrs);
      setIf(AccountCapability.ATTR_BILLINGCCTYPE, account.getBillingCCType(), attrs);
      setIf(AccountCapability.ATTR_BILLINGCITY, account.getBillingCity(), attrs);
      setIf(AccountCapability.ATTR_BILLINGFIRSTNAME, account.getBillingFirstName(), attrs);
      setIf(AccountCapability.ATTR_BILLINGLASTNAME, account.getBillingLastName(), attrs);
      setIf(AccountCapability.ATTR_BILLINGSTATE, account.getBillingState(), attrs);
      setIf(AccountCapability.ATTR_BILLINGSTREET1, account.getBillingStreet1(), attrs);
      setIf(AccountCapability.ATTR_BILLINGSTREET2, account.getBillingStreet2(), attrs);
      setIf(AccountCapability.ATTR_BILLINGZIP, account.getBillingZip(), attrs);
      setIf(AccountCapability.ATTR_BILLINGZIPPLUSFOUR, account.getBillingZipPlusFour(), attrs);
      setIf(AccountCapability.ATTR_OWNER, account.getOwner(), attrs);
      setIf(AccountCapability.ATTR_STATE, account.getState(), attrs);
      setIf(AccountCapability.ATTR_TAXEXEMPT, account.getTaxExempt(), attrs);
      setIf(AccountCapability.ATTR_CREATED,account.getCreated(),attrs);
      setIf(AccountCapability.ATTR_MODIFIED,account.getModified(),attrs);
      setIf(Capability.ATTR_ADDRESS, account.getAddress(), attrs);
      setIf(Capability.ATTR_CAPS, account.getCaps(), attrs);
      setIf(Capability.ATTR_ID, account.getId(), attrs);
      setIf(Capability.ATTR_TAGS, account.getTags(), attrs);
      setIf(Capability.ATTR_TYPE, account.getType(), attrs);
      return attrs;
   }

   private void setIf(String key, Object val, Map<String,Object> attrs) {
      if(val != null) {
         if(val instanceof UUID) {
            val = val.toString();
         }
         attrs.put(key, val);
      }
   }

   @Override
   public PagedResults<Account> listAccounts(AccountQuery query) {
	   BoundStatement bs = null;
	   if (query.getToken() != null) {
		   bs = listPaged.bind(UUID.fromString(query.getToken()), query.getLimit() + 1);
	   } else {
		   bs = listPaged.bind(IrisUUID.nilUUID(), query.getLimit() + 1);
	   }
	   try(Context ctxt = listAccountsTimer.time()) {
         return doList(bs, query.getLimit());
      }
   }

   @Override
   public Stream<Account> streamAll() {
      try(Context ctxt = streamAllTimer.time()) {
         Iterator<Row> rows = session.execute(new BoundStatement(listAll)).iterator();
         Iterator<Account> result = Iterators.transform(rows, (row) -> buildEntity(row));
         Spliterator<Account> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
         return StreamSupport.stream(stream, false);
      }
   }
}

