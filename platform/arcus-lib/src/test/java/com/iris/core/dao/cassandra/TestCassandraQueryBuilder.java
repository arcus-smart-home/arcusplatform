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

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraDeleteBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraInsertBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraSelectBuilder;
import com.iris.core.dao.cassandra.CassandraQueryBuilder.CassandraUpdateBuilder;
import com.iris.platform.rule.cassandra.RuleEnvironmentTable;

public class TestCassandraQueryBuilder {
   private static final String HUB_MAC_INDEX_TABLE = "hub_macaddr";
   private static final String HUB_ACCOUNT_INDEX_TABLE = "hub_accountid";
   private static final String HUB_PLACE_INDEX_TABLE = "hub_placeid";

	@Test
	public void testAccountListAll() {
		test("SELECT * FROM account").select("account").verify();
	}

	@Test
	public void testAuthGrantUpsert() {
		String UPSERT = "INSERT INTO authorization_grant (" +
	         "entityId, placeId, accountId, accountOwner, permissions, placeName) VALUES (" +
	         "?, ?, ?, ?, ?, ?)";
		test(UPSERT)
			.insert("authorization_grant")
			.addColumn("entityId")
			.addColumn("placeId")
			.addColumn("accountId")
			.addColumn("accountOwner")
			.addColumn("permissions")
			.addColumn("placeName")
			.verify();
	}

	@Test
	public void testAuthGrantUpsertByPlace() {
		String UPSERT_BY_PLACE = "INSERT INTO authorization_grant_by_place (" +
	         "placeId, entityId, accountId, accountOwner, permissions, placeName) VALUES (" +
	         "?, ?, ?, ?, ?, ?)";
		String[] cols = { "placeId", "entityId", "accountId", "accountOwner", "permissions", "placeName" };
		test(UPSERT_BY_PLACE)
			.insert("authorization_grant_by_place")
			.addColumns(cols)
			.verify();
	}

	@Test
	public void testAuthGrantFindForEntity() {
		String FIND_FOR_ENTITY = "SELECT * FROM authorization_grant WHERE entityId = ?";
		test(FIND_FOR_ENTITY)
			.select("authorization_grant")
			.addWhereColumnEquals("entityId")
			.verify();
	}

	@Test
	public void testAuthGrantFindForPlace() {
		String FIND_FOR_PLACE = "SELECT * FROM authorization_grant_by_place WHERE placeId = ?";
		test(FIND_FOR_PLACE)
			.select("authorization_grant_by_place")
			.addWhereColumnEquals("placeId")
			.verify();
	}

	@Test
	public void testAuthGrantRemoveGrant() {
		String REMOVE_GRANT = "DELETE FROM authorization_grant WHERE entityId = ? AND placeId = ?";
		test(REMOVE_GRANT)
			.delete("authorization_grant")
			.addWhereColumnEquals("entityId")
			.addWhereColumnEquals("placeId")
			.verify();
	}

	@Test
	public void testAuthGrantRemoveForPlace() {
		String REMOVE_FOR_PLACE = "DELETE FROM authorization_grant_by_place WHERE placeId = ?";
		test(REMOVE_FOR_PLACE)
			.delete("authorization_grant_by_place")
			.addWhereColumnEquals("placeId")
			.verify();
	}

	@Test
	public void testAuthGrantRemoveForEntity() {
		String REMOVE_FOR_ENTITY = "DELETE FROM authorization_grant WHERE entityId = ?";
		test(REMOVE_FOR_ENTITY)
			.delete("authorization_grant")
			.addWhereColumnEquals("entityId")
			.verify();
	}

	@Test
	public void testHubInsertMacaddrIndex() {
		String INSERT_MACADDR_INDEX =
	         "INSERT INTO " + HUB_MAC_INDEX_TABLE + " (macaddr_0_7, macaddr, hubid) " +
	               "VALUES (?, ?, ?)";
		test(INSERT_MACADDR_INDEX)
			.insert(HUB_MAC_INDEX_TABLE)
			.addColumn("macaddr_0_7")
			.addColumn("macaddr")
			.addColumn("hubid")
			.verify();
	}

	@Test
	public void testHubFindIdByMacaddr() {
		String FIND_ID_BY_MACADDR =
	         "SELECT hubid FROM " + HUB_MAC_INDEX_TABLE + " WHERE macaddr_0_7 = ? AND macaddr = ?";
		test(FIND_ID_BY_MACADDR)
			.select(HUB_MAC_INDEX_TABLE)
			.addColumn("hubid")
			.addWhereColumnEquals("macaddr_0_7")
			.addWhereColumnEquals("macaddr")
			.verify();
	}

	@Test
	public void testHubDeleteMacaddrIndex() {
		String DELETE_MACADDR_INDEX =
	         "DELETE FROM " + HUB_MAC_INDEX_TABLE + " WHERE macaddr_0_7 = ? AND macaddr = ?";
		test(DELETE_MACADDR_INDEX)
			.delete(HUB_MAC_INDEX_TABLE)
			.addWhereColumnEquals("macaddr_0_7")
			.addWhereColumnEquals("macaddr")
			.verify();
	}

	@Test
	public void testHubInsertAccountIndex() {
		String INSERT_ACCOUNT_INDEX =
	         "INSERT INTO " + HUB_ACCOUNT_INDEX_TABLE + " (accountId, hubid) VALUES (?, ?)";
		test(INSERT_ACCOUNT_INDEX)
			.insert(HUB_ACCOUNT_INDEX_TABLE)
			.addColumn("accountId")
			.addColumn("hubid")
			.verify();
	}

	@Test
	public void testHubFindIdsByAccount() {
		String FIND_IDS_BY_ACCOUNT =
	         "SELECT hubid FROM " + HUB_ACCOUNT_INDEX_TABLE + " WHERE accountId = ?";
		test(FIND_IDS_BY_ACCOUNT)
			.select(HUB_ACCOUNT_INDEX_TABLE)
			.addColumn("hubid")
			.addWhereColumnEquals("accountId")
			.verify();
	}

	@Test
	public void testHubDeleteAccountIndex() {
		String DELETE_ACCOUNT_INDEX =
	         "DELETE FROM " + HUB_ACCOUNT_INDEX_TABLE + " WHERE hubid = ? AND accountId = ?";
		test(DELETE_ACCOUNT_INDEX)
			.delete(HUB_ACCOUNT_INDEX_TABLE)
			.addWhereColumnEquals("hubid")
			.addWhereColumnEquals("accountId")
			.verify();
	}

	@Test
	public void testHubInsertPlaceIndex() {
		String INSERT_PLACE_INDEX = "INSERT INTO " + HUB_PLACE_INDEX_TABLE + " (placeid, hubid) VALUES (?, ?)";
		test(INSERT_PLACE_INDEX)
			.insert(HUB_PLACE_INDEX_TABLE)
			.addColumn("placeid")
			.addColumn("hubid")
			.verify();
	}

	@Test
	public void testHubFindIdForPlace() {
		String FIND_ID_FOR_PLACE = "SELECT hubid FROM "+ HUB_PLACE_INDEX_TABLE + " WHERE placeid = ?";
		test(FIND_ID_FOR_PLACE)
		   .select(HUB_PLACE_INDEX_TABLE)
			.addColumn("hubid")
			.addWhereColumnEquals("placeid")
			.verify();
	}

	@Test
	public void testHubDeletePlaceIndex() {
		String DELETE_PLACE_INDEX = "DELETE FROM " + HUB_PLACE_INDEX_TABLE + " WHERE hubid = ? AND placeid = ?";
		test(DELETE_PLACE_INDEX)
			.delete(HUB_PLACE_INDEX_TABLE)
			.addWhereColumnEquals("hubid")
			.addWhereColumnEquals("placeid")
			.verify();
	}

	/********************************************************************************/
	/*** PersonDaoIMPL                                                             **/
	/********************************************************************************/

	private static class Person {
		public final static String TABLE = "person";
	}

	@Test
	public void testPersonInsertLogin() {
		String INSERT_LOGIN =
	         "INSERT INTO login (domain, user_0_3, user, password, password_salt, personid, reset_token) " +
	         "VALUES (?, ?, ?, ?, ?, ?, ?) " +
	         "IF NOT EXISTS";
		test(INSERT_LOGIN).insert("login")
			.addColumn("domain")
			.addColumn("user_0_3")
			.addColumn("user")
			.addColumn("password")
			.addColumn("password_salt")
			.addColumn("personid")
			.addColumn("reset_token")
			.ifNotExists()
			.verify();
	}

	@Test
	public void testPersonFindLoginByEmail() {
		String FIND_LOGIN_BY_EMAIL =
	         "SELECT * FROM login WHERE domain = ? AND user_0_3 = ? AND user = ?";
		test(FIND_LOGIN_BY_EMAIL).select("login")
			.addWhereColumnEquals("domain")
			.addWhereColumnEquals("user_0_3")
			.addWhereColumnEquals("user")
			.verify();
	}

	@Test
	public void testPersonDeleteIndex() {
		String DELETE_INDEX =
	         "DELETE FROM login WHERE domain = ? AND user_0_3 = ? AND user = ?";
		test(DELETE_INDEX).delete("login")
			.addWhereColumnEquals("domain")
			.addWhereColumnEquals("user_0_3")
			.addWhereColumnEquals("user")
			.verify();
	}

	@Test
	public void testPersonUpdatePassword() {
		String UPDATE_PASSWORD =
	         "UPDATE login SET password = ?, password_salt = ? WHERE domain = ? AND user_0_3 = ? AND user = ?";
		test(UPDATE_PASSWORD).update("login")
			.addColumn("password")
			.addColumn("password_salt")
			.addWhereColumnEquals("domain")
			.addWhereColumnEquals("user_0_3")
			.addWhereColumnEquals("user")
			.verify();
	}

	@Test
	public void testPersonDeleteMobileDevices() {
		String DELETE_MOBILEDEVICES = "DELETE FROM mobiledevices WHERE personId = ?";
		test(DELETE_MOBILEDEVICES).delete("mobiledevices")
			.addWhereColumnEquals("personId")
			.verify();
	}

	@Test
	public void testPersonInitMobileDeviceSequence() {
		String INIT_MOBILEDEVICE_SEQUENCE = "UPDATE person SET mobileDeviceSequence=0 WHERE id = ?";
		test(INIT_MOBILEDEVICE_SEQUENCE).update("person")
			.set("mobileDeviceSequence=0")
			.addWhereColumnEquals("id")
			.verify();
	}

	@Test
	public void testPersonSetUpdateFlag() {
		String SET_UPDATEFLAG = "UPDATE " + Person.TABLE + " SET updateflag = ? WHERE id = ?";
		test(SET_UPDATEFLAG).update(Person.TABLE)
			.addColumn("updateflag")
			.addWhereColumnEquals("id")
			.verify();
	}

	@Test
	public void testPersonGetUpdateFlag() {
		String GET_UPDATEFLAG = "SELECT updateflag FROM " + Person.TABLE + " WHERE id = ?";
		test(GET_UPDATEFLAG).select(Person.TABLE)
			.addColumn("updateflag")
			.addWhereColumnEquals("id")
			.verify();
	}

	@Test
	public void testFindAllPeople() {
		String FIND_ALL_PEOPLE = "SELECT * FROM " + Person.TABLE;
		test(FIND_ALL_PEOPLE).select(Person.TABLE)
			.verify();
	}

	@Test
	public void testFindAllPeopleLimit() {
		String FIND_ALL_PEOPLE_LIMIT = "SELECT * FROM " + Person.TABLE + " LIMIT ?";
		test(FIND_ALL_PEOPLE_LIMIT).select(Person.TABLE)
			.boundLimit()
			.verify();
	}

	/********************************************************************************/
	/*** Rules Daos                                                                **/
	/********************************************************************************/

	private static class AD {
		static final String TYPE = "action";

		static final String [] UPSERT_COLUMNS = new String [] {
		   "created",
	      "modified",
	      "name",
	      "description",
	      "tags",
	      "action"
	   };
	}

	private static class RET {
		static final String NAME = "RuleEnvironment";

		static class Col {
			static final String PLACE_ID = "placeId";
			static final String TYPE = "type";
			static final String ID = "id";
		}
	}

	private static String SEQ = "sequenceFieldName";

	private static String whereIdEq(String type) {
      return
         "placeId" + " = ? "
               + "AND " + "type" + " = '"  + type + "' "
               + "AND " + "id" + " = ?";
   }

	@Test
	public void testActionUpsert() {
		String UPSERT = "UPDATE " + RuleEnvironmentTable.NAME + " " +
            "SET " + StringUtils.join(AD.UPSERT_COLUMNS, " = ?, ") + " = ?" +
            " WHERE " + whereIdEq(AD.TYPE);
		test(UPSERT).update(RuleEnvironmentTable.NAME)
				.addColumns(AD.UPSERT_COLUMNS)
				.where(whereIdEq(AD.TYPE))
				.verify();
	}

	@Test
	public void testUpdateLastExecution() {
		String UPDATE_LAST_EXECUTION = "UPDATE " + RuleEnvironmentTable.NAME + " " +
            "SET " + "actionLastExecuted" + " = ?" +
            " WHERE " + whereIdEq(AD.TYPE);
		test(UPDATE_LAST_EXECUTION).update(RuleEnvironmentTable.NAME)
				.addColumn("actionLastExecuted")
				.where(whereIdEq(AD.TYPE))
				.verify();
	}

	@Test
	public void testRuleEnvListByPlace() {
		String LIST_BY_PLACE = "SELECT * FROM " + RET.NAME +
            " WHERE " + RET.Col.PLACE_ID + " = ?"
            + " AND " + RET.Col.TYPE + " = '"  + AD.TYPE + "'";
		test(LIST_BY_PLACE).select(RET.NAME)
				.addWhereColumnEquals(RET.Col.PLACE_ID)
				.where(RET.Col.TYPE + " = '" + AD.TYPE + "'")
				.verify();
	}

	@Test
	public void testRuleEnvFindById() {
		String FIND_BY_ID = "SELECT * FROM " + RET.NAME + " " +
            "WHERE " + RET.Col.PLACE_ID + " = ? "
            + "AND " + RET.Col.TYPE + " = '"  + AD.TYPE + "' "
            + "AND " + RET.Col.ID + " = ?";
		test(FIND_BY_ID).select(RET.NAME)
				.where(whereIdEq(AD.TYPE))
				.verify();
	}

	@Test
	public void testRuleEnvCurrentSequenceNumber() {
		String CURRENT_SEQ_NUMBER = "SELECT " + SEQ + " " +
            "FROM place " +
            "WHERE id = ?";
		test(CURRENT_SEQ_NUMBER).select("place")
				.addColumn(SEQ)
				.addWhereColumnEquals("id")
				.verify();
	}

	@Test
	public void testRuleEnvIncrementSequenceIf() {
		String INCREMENT_SEQUENCE_IF = "UPDATE place " +
            "SET " + SEQ + " = ? " +
            "WHERE id = ? " +
            "IF " + SEQ + " = ?";
		test(INCREMENT_SEQUENCE_IF).update("place")
				.addColumn(SEQ)
				.addWhereColumnEquals("id")
				.ifClause(SEQ + " = ?")
				.verify();
	}

	@Test
	public void testRuleEnvDeleteById() {
		String DELETE_BY_ID = "DELETE FROM " + RuleEnvironmentTable.NAME +
            " WHERE " + whereIdEq(AD.TYPE);
		test(DELETE_BY_ID).delete(RET.NAME)
				.where(whereIdEq(AD.TYPE))
				.verify();
	}
	
	private static class RD {
		static final String TYPE = "rule";

		static final String [] UPSERT_COLUMNS = new String [] {
		   "created", "modified", "name", "description", "tags", "disabled", "suspended", "expressions",
		   "template2", "variables", "actionconfig", "conditionalconfig" };
	}
	
	@Test
	public void testRuleUpsert() {
		String UPSERT = "UPDATE " + RuleEnvironmentTable.NAME + " " +
            "SET " + StringUtils.join(RD.UPSERT_COLUMNS, " = ?, ") + " = ?" +
            " WHERE " + whereIdEq(RD.TYPE);
		test(UPSERT).update(RuleEnvironmentTable.NAME)
			.addColumns(RD.UPSERT_COLUMNS)
			.where(whereIdEq(RD.TYPE))
			.verify();
	}
	
	@Test
	public void testRuleUpdateVars() {
		String UPDATE_VARS = "UPDATE " + RuleEnvironmentTable.NAME + " " +
            "SET " + "variables" + " = ?" +
            " WHERE " + whereIdEq(RD.TYPE) +
            " IF modified = ?";
		test(UPDATE_VARS).update(RuleEnvironmentTable.NAME)
				.addColumn("variables")
				.where(whereIdEq(RD.TYPE))
				.ifClause("modified = ?")
				.verify();
	}
	
	/*
	 * 
	 * MobileDeviceDAOImpl
	 * 
	 */
	
	@Test
   public void testMobileCurrentCount() {
	   String CURRENT_COUNT = "SELECT mobileDeviceSequence FROM person WHERE id = ?";
	   test(CURRENT_COUNT).select(Tables.PERSON)
	   		.addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
	   		.addWhereColumnEquals(Tables.PersonCols.ID)
	   		.verify();
   }
	
	@Test
   public void testMobileUpdateCurrent() {
	   String UPDATE_CURRENT = "UPDATE person SET mobileDeviceSequence = ? WHERE id = ? IF mobileDeviceSequence = ?";
	   test(UPDATE_CURRENT).update(Tables.PERSON)
	   		.addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
	   		.addWhereColumnEquals(Tables.PersonCols.ID)
	   		.ifClause(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE + " = ?")
	   		.verify();
   }
	
	@Test
   public void testMobileInitialUpdateCurrent() {
	   String INITIAL_UPDATE_CURRENT = "UPDATE person SET mobileDeviceSequence = ? WHERE id = ?";
	   test(INITIAL_UPDATE_CURRENT).update(Tables.PERSON)
	   		.addColumn(Tables.PersonCols.MOBILE_DEVICE_SEQUENCE)
	   		.addWhereColumnEquals(Tables.PersonCols.ID)
	   		.verify();
   }
	
	@Test
   public void testMobileUpsert() {
	   String UPSERT = "INSERT INTO mobiledevices (personId, deviceIndex, "
            + "associated, osType, osVersion, formFactor, phoneNumber, deviceIdentifier, deviceModel, "
            + "deviceVendor, resolution, notificationToken, lastLatitude, lastLongitude, lastLocationTime, name, appVersion) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	   test(UPSERT).insert(Tables.MOBILE_DEVICES)
	   	.addColumn(Tables.MobileDevicesCols.PERSON_ID)
	   	.addColumn(Tables.MobileDevicesCols.DEVICE_INDEX)
	   	.addColumn(Tables.MobileDevicesCols.ASSOCIATED)
	   	.addColumn(Tables.MobileDevicesCols.OS_TYPE)
	   	.addColumn(Tables.MobileDevicesCols.OS_VERSION)
	   	.addColumn(Tables.MobileDevicesCols.FORM_FACTOR)
	   	.addColumn(Tables.MobileDevicesCols.PHONE_NUMBER)
	   	.addColumn(Tables.MobileDevicesCols.DEVICE_IDENTIFIER)
	   	.addColumn(Tables.MobileDevicesCols.DEVICE_MODEL)
	   	.addColumn(Tables.MobileDevicesCols.DEVICE_VENDOR)
	   	.addColumn(Tables.MobileDevicesCols.RESOLUTION)
	   	.addColumn(Tables.MobileDevicesCols.NOTIFICATION_TOKEN)
	   	.addColumn(Tables.MobileDevicesCols.LAST_LATITUDE)
	   	.addColumn(Tables.MobileDevicesCols.LAST_LONGITUDE)
	   	.addColumn(Tables.MobileDevicesCols.LAST_LOCATION_TIME)
         .addColumn(Tables.MobileDevicesCols.NAME)	   
         .addColumn(Tables.MobileDevicesCols.APP_VERSION)         
	   	.verify();
   }
	
	@Test
   public void testMobileDelete() {
	   String DELETE = "DELETE FROM mobiledevices WHERE personId = ? AND deviceIndex = ?";
	   test(DELETE).delete(Tables.MOBILE_DEVICES)
	   	.addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
	   	.addWhereColumnEquals(Tables.MobileDevicesCols.DEVICE_INDEX)
	   	.verify();
   }
	
	@Test
   public void testMobileFindOne() {
	   String FIND_ONE = "SELECT * FROM mobiledevices WHERE personId = ? AND deviceIndex = ?";
	   test(FIND_ONE).select(Tables.MOBILE_DEVICES)
	   	.addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
	   	.addWhereColumnEquals(Tables.MobileDevicesCols.DEVICE_INDEX)
	   	.verify();
   }
	
	@Test
   public void testMobileListForPerson() {
	   String LIST_FOR_PERSON = "SELECT * FROM mobiledevices WHERE personId = ?";
	   test(LIST_FOR_PERSON).select(Tables.MOBILE_DEVICES)
	   	.addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
	   	.verify();
   }
	
	@Test
   public void testMobileInsertTokenIndex() {
	   String INSERT_TOKEN_INDEX = "INSERT INTO notificationtoken_mobiledevice (notificationToken, personId, deviceIndex) VALUES (?, ?, ?)";
	   test(INSERT_TOKEN_INDEX).insert(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
	   	.addColumn(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
	   	.addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
	   	.addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
	   	.verify();
   }
	
   public void testMobileOptimisticTokenIndexInsert() {
      String INSERT_TOKEN_INDEX = "INSERT INTO notificationtoken_mobiledevice (notificationToken, personId, deviceIndex) VALUES (?, ?, ?) IF NOT EXISTS";
      test(INSERT_TOKEN_INDEX).insert(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
         .addColumn(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
         .addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
         .addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
         .ifNotExists()
         .verify();
   }

   public void testMobileOptimisticTokenIndexUpdate() {
      String UPDATE_TOKEN_INDEX = "UPDATE notificationtoken_mobiledevice SET personId = ?, deviceIndex =? WHERE notificationToken ? IF EXISTS";
      test(UPDATE_TOKEN_INDEX).update(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
      .addColumn(Tables.NotificationTokenMobileDeviceCols.PERSON_ID)
      .addColumn(Tables.NotificationTokenMobileDeviceCols.DEVICE_INDEX)
      .addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
      .ifExists()
         .verify();
   }
   
	@Test
   public void testMobileDeleteTokenIndex() {
	   String DELETE_TOKEN_INDEX = "DELETE FROM notificationtoken_mobiledevice WHERE notificationToken = ?";
	   test(DELETE_TOKEN_INDEX).delete(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
   	.addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
   	.verify();
   }
	
	@Test
   public void testMobileGetPersonAndIdForToken() {
	   String GET_PERSON_AND_ID = "SELECT * FROM notificationtoken_mobiledevice WHERE notificationToken = ?";
	   test(GET_PERSON_AND_ID).select(Tables.NOTIFICATION_TOKEN_MOBILE_DEVICE)
   	.addWhereColumnEquals(Tables.NotificationTokenMobileDeviceCols.NOTIFICATION_TOKEN)
   	.verify();
   }

   @Test
   public void testMapColumn() {
      String UPSERT = "UPDATE preferences SET prefs[?] = ? WHERE personId = ? AND placeId = ?";
      test(UPSERT)
         .update("preferences")
         .addMapColumn("prefs")
         .addWhereColumnEquals("personId")
         .addWhereColumnEquals("placeId")
         .verify();
   }

   @Test
   public void testDeleteMapEntry() {
      String DELETE = "DELETE prefs[?] FROM preferences WHERE personId = ? AND placeId = ?";
      test(DELETE)
         .delete("preferences")
         .addMapColumn("prefs")
         .addWhereColumnEquals("personId")
         .addWhereColumnEquals("placeId")
         .verify();
   }

	public static Tester test(String expected) {
		return new Tester(expected);
	}

	public static class Tester {
		private final String expected;
		private CassandraSelectBuilder sqb;
		private CassandraInsertBuilder iqb;
		private CassandraDeleteBuilder dqb;
		private CassandraUpdateBuilder uqb;

		public Tester(String expected) {
			this.expected = expected;
		}

		private CassandraQueryBuilder<?> qb() {
			if (sqb != null) {
				return sqb;
			}
			else if (iqb != null) {
				return iqb;
			}
			else if (dqb != null) {
				return dqb;
			}
			else if (uqb != null) {
				return uqb;
			}
			Assert.fail("Invalid Test: QueryBuilder expected.");
			return null;
		}

		public Tester select(String table) {
			sqb = CassandraQueryBuilder.select(table);
			return this;
		}

		public Tester insert(String table) {
			iqb = CassandraQueryBuilder.insert(table);
			return this;
		}

		public Tester delete(String table) {
			dqb = CassandraQueryBuilder.delete(table);
			return this;
		}

		public Tester update(String table) {
			uqb = CassandraQueryBuilder.update(table);
			return this;
		}

		public Tester addColumn(String col) {
			qb().addColumn(col);
			return this;
		}

		public Tester addMapColumn(String col) {
			qb().addMapColumn(col);
			return this;
		}

		public Tester addColumns(Collection<String> cols) {
			qb().addColumns(cols);
			return this;
		}

		public Tester addColumns(String... cols) {
			qb().addColumns(cols);
			return this;
		}

		public Tester set(String setClause) {
			uqb.set(setClause);
			return this;
		}

		public Tester addWhereColumnEquals(String col) {
			qb().addWhereColumnEquals(col);
			return this;
		}

		public Tester where(String clause) {
			qb().where(clause);
			return this;
		}

		public Tester boundLimit() {
			qb().boundLimit();
			return this;
		}

		public Tester ifClause(String ifClause) {
			uqb.ifClause(ifClause);
			return this;
		}

		public Tester ifNotExists() {
			iqb.ifNotExists();
			return this;
		}

      public Tester ifExists() {
         uqb.ifExists();
         return this;
      }		
		public void verify() {
			String generatedQuery = qb().toQuery().toString();
		   System.out.println("Generated Query: " + generatedQuery);
		   Assert.assertEquals(expected, generatedQuery);
		}
	}
}

