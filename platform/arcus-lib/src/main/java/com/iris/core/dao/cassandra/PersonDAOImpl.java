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

import static com.iris.core.dao.PersonDAO.ResetPasswordResult.FAILURE;
import static com.iris.core.dao.PersonDAO.ResetPasswordResult.SUCCESS;
import static com.iris.core.dao.PersonDAO.ResetPasswordResult.TOKEN_FAILURE;

import java.util.ArrayList;
import java.util.Date;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.Utils;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.util.PhoneNumber;
import com.iris.capability.util.PhoneNumbers;
import com.iris.capability.util.PhoneNumbers.PhoneNumberFormat;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.exception.EmailInUseException;
import com.iris.core.dao.exception.EmailMismatchException;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.core.dao.metrics.ColumnRepairMetrics;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.PagedResults;
import com.iris.security.Login;
import com.iris.security.ParsedEmail;
import com.iris.security.credentials.CredentialsHashingStrategy;
import com.iris.security.crypto.AES;
import com.iris.util.TokenUtil;

@Singleton
public class PersonDAOImpl extends BaseCassandraCRUDDao<UUID, Person> implements PersonDAO {
   private static final Logger logger = LoggerFactory.getLogger(PersonDAOImpl.class);

   private static final Timer findByEmailTimer = DaoMetrics.readTimer(PersonDAO.class, "findByEmail");
   private static final Timer updatePasswordTimer = DaoMetrics.updateTimer(PersonDAO.class, "updatePassword");
   private static final Timer findLoginTimer = DaoMetrics.readTimer(PersonDAO.class, "findLogin");
   private static final Timer generatePasswordResetTokenTimer = DaoMetrics.updateTimer(PersonDAO.class, "generatePasswordResetToken");
   private static final Timer resetPasswordTimer = DaoMetrics.updateTimer(PersonDAO.class, "resetPassword");
   private static final Timer updatePersonAndEmailTimer = DaoMetrics.upsertTimer(PersonDAO.class, "updatePersonAndEmail");
   private static final Timer updatePinAtPlaceTimer = DaoMetrics.updateTimer(PersonDAO.class, "updatePinAtPlace");
   private static final Timer setUpdateFlagTimer = DaoMetrics.updateTimer(PersonDAO.class, "setUpdateFlag");
   private static final Timer getUpdateFlagTimer = DaoMetrics.readTimer(PersonDAO.class, "getUpdateFlag");
   private static final Timer listPersonsTimer = DaoMetrics.readTimer(PersonDAO.class, "listPersons");
   private static final Timer listByPlaceIdTimer = DaoMetrics.readTimer(PersonDAO.class, "listByPlaceId");
   private static final Timer streamAllTimer = DaoMetrics.readTimer(PersonDAO.class, "streamAll");

   static final String TABLE = "person";
   private static final String UPDATEFLAG = "updateflag";

   private static class LoginColumns {
      final static String DOMAIN = "domain";
      final static String USER_0_3 = "user_0_3";
      final static String USER = "user";
      final static String PASSWORD = "password";
      final static String PASSWORD_SALT = "password_salt";
      final static String USERID = "personid";
      final static String RESET_TOKEN = "reset_token";
      final static String LAST_PASS_CHANGE = "lastPassChange";
   }

   static class PersonEntityColumns {
      final static String ACCOUNT_ID = "accountId";
      final static String FIRST_NAME = "firstName";
      final static String LAST_NAME = "lastName";
      final static String TERMS_AGREED = "termsAgreed";
      final static String PRIVACY_POLICY_AGREED = "privacyPolicyAgreed";
      final static String EMAIL = "email";
      final static String EMAIL_VERIFIED = "emailVerified";
      final static String MOBILE_NUMBER = "mobileNumber";
      final static String MOBILE_VERIFIED = "mobileVerified";
      final static String MOBILE_NOTIFICATION_ENDPOINTS = "mobileNotificationEndpoints";
      final static String CURRENT_PLACE = "currPlace";
      final static String CURRENT_PLACE_METHOD = "currPlaceMethod";
      final static String CURRENT_LOCATION = "currLocation";
      final static String CURRENT_LOCATION_TIME = "currLocationTime";
      final static String CURRENT_LOCATION_METHOD = "currLocationMethod";
      final static String CONSENT_OFFERSPROMOTIONS = "consentOffersPromotions";
      final static String CONSENT_STATEMENT = "consentStatement";
      @Deprecated
      final static String PIN = "pin";
      @Deprecated
      final static String PIN2 = "pin2";
      final static String PINPERPLACE = "pinPerPlace";
      final static String SECURITY_ANSWERS = "securityAnswers";
      final static String HAS_LOGIN = "hasLogin";
      final static String EMAIL_VERIFICATION_TOKEN = "emailVerificationToken";
   };

   private static final String[] COLUMN_ORDER = {
      PersonEntityColumns.ACCOUNT_ID,
      PersonEntityColumns.FIRST_NAME,
      PersonEntityColumns.LAST_NAME,
      PersonEntityColumns.TERMS_AGREED,
      PersonEntityColumns.PRIVACY_POLICY_AGREED,
      PersonEntityColumns.EMAIL,
      PersonEntityColumns.EMAIL_VERIFIED,
      PersonEntityColumns.MOBILE_NUMBER,
      PersonEntityColumns.MOBILE_VERIFIED,
      PersonEntityColumns.MOBILE_NOTIFICATION_ENDPOINTS,
      PersonEntityColumns.CURRENT_PLACE,
      PersonEntityColumns.CURRENT_PLACE_METHOD,
      PersonEntityColumns.CURRENT_LOCATION,
      PersonEntityColumns.CURRENT_LOCATION_TIME,
      PersonEntityColumns.CURRENT_LOCATION_METHOD,
      PersonEntityColumns.CONSENT_OFFERSPROMOTIONS,
      PersonEntityColumns.CONSENT_STATEMENT,
      PersonEntityColumns.SECURITY_ANSWERS,
      PersonEntityColumns.HAS_LOGIN,
      PersonEntityColumns.EMAIL_VERIFICATION_TOKEN
   };

   private static final String[] READ_ONLY_COLUMN_ORDER = {
      PersonEntityColumns.PIN2,
      PersonEntityColumns.PINPERPLACE
   };

   private static final String OPTIMISTIC_UPDATE =
         CassandraQueryBuilder
            .update(TABLE)
            .addColumn(BaseEntityColumns.MODIFIED)
            .addColumn(BaseEntityColumns.TAGS)
            .addColumn(BaseEntityColumns.IMAGES)
            .addColumns(COLUMN_ORDER)
            .addWhereColumnEquals(BaseEntityColumns.ID)
            .toQuery()
            // TODO lightweight transaction support to cassandra query builder
            .append(" IF ")
            .append(PersonEntityColumns.EMAIL)
            .append(" = ?")
            .toString();

   private PreparedStatement insertLogin;
   private PreparedStatement findLoginByEmail;
   private PreparedStatement deleteIndex;
   private PreparedStatement updatePersonOptimistic;
   private PreparedStatement updatePassword;
   private PreparedStatement updatePinAtPlace;
   private PreparedStatement updatePinAtPlaceAndPin2;
   private PreparedStatement initMobileDeviceSequence;
   private PreparedStatement deleteMobileDevices;
   private PreparedStatement setUpdateFlag;
   private PreparedStatement getUpdateFlag;
   private PreparedStatement findAllPeople;
   private PreparedStatement findAllPeopleLimit;

   private final PreparedStatement listPaged;

   private final AES aes;

   private final PersonPlaceAssocDAO personPlaceAssocDAO;

   @Inject(optional = true)
   @Named("login.reset.token_ttl_mins")
   private int tokenTTLMinutes = 15;

   @Inject(optional = true)
   @Named("dao.person.asynctimeoutms")
   private long asyncTimeoutMs = 30000;

   @Inject
   @Named("questions.aes.secret")
   private String questionsAesSecret;

   @Inject
   public PersonDAOImpl(Session session, AES aes, PersonPlaceAssocDAO personPlaceAssocDAO) {
      super(session, TABLE, COLUMN_ORDER, READ_ONLY_COLUMN_ORDER);
      insertLogin = prepareInsertLogin();
      findLoginByEmail = prepareFindLoginByEmail();
      deleteIndex = prepareDeleteIndex();
      updatePersonOptimistic = prepareOptimisticUpdate();
      updatePassword = prepareUpdatePassword();
      updatePinAtPlace = prepareUpdatePinAtPlace();
      updatePinAtPlaceAndPin2 = prepareUpdatePinAtPlaceAndPin2();
      initMobileDeviceSequence = prepareInitMobileDeviceSequence();
      deleteMobileDevices = prepareDeleteMobileDevices();
      setUpdateFlag = prepareSetUpdateFlag();
      getUpdateFlag = prepareGetUpdateFlag();
      findAllPeople = prepareFindAllPeople();
      findAllPeopleLimit = prepareFindAllPeopleLimit();

      this.aes = aes;
      this.personPlaceAssocDAO = personPlaceAssocDAO;

      listPaged = prepareListPaged();
   }

   @Override
   public Person findByAddress(Address addr) {
      if(addr == null) {
         return null;
      }
      if(!(addr instanceof PlatformServiceAddress)) {
         throw new ErrorEventException(Errors.CODE_GENERIC, addr.getRepresentation() + " is not a person");
      }
      PlatformServiceAddress platAddr = (PlatformServiceAddress) addr;
      if(!PlatformConstants.SERVICE_PEOPLE.equals(platAddr.getGroup())) {
         throw new ErrorEventException(Errors.CODE_GENERIC, addr.getRepresentation() + " is not a person");
      }
      Object id = addr.getId();
      if(id == null || !(id instanceof UUID)) {
         throw new ErrorEventException(Errors.CODE_GENERIC, addr.getRepresentation() + " does not contain a valid perosn id");
      }
      return findById((UUID) id);
   }

   @Override
   protected CassandraQueryBuilder selectNonEntityColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(PersonEntityColumns.PIN);
      return super.selectNonEntityColumns(queryBuilder);
   }

   @Override
   protected List<Object> getValues(Person entity) {
      List<Object> values = new LinkedList<Object>();
      values.add(entity.getAccountId());
      values.add(entity.getFirstName());
      values.add(entity.getLastName());
      values.add(entity.getTermsAgreed());
      values.add(entity.getPrivacyPolicyAgreed());
      values.add(entity.getEmail());
      values.add(entity.getEmailVerified());
      values.add(entity.getMobileNumber());
      values.add(entity.getMobileVerified());
      values.add(entity.getMobileNotificationEndpoints());
      values.add(entity.getCurrPlace());
      values.add(entity.getCurrPlaceMethod());
      values.add(entity.getCurrLocation());
      values.add(entity.getCurrLocationTime());
      values.add(entity.getCurrLocationMethod());
      values.add(entity.getConsentOffersPromotions());
      values.add(entity.getConsentStatement());

      if(entity.getSecurityAnswers() != null) {
         Map<String,String> securityAnswers = new HashMap<>(entity.getSecurityAnswers());
         securityAnswers.replaceAll((k,v) -> { return aes.encrypt(k, v); });
         values.add(securityAnswers);
      } else {
         values.add(null);
      }

      values.add(entity.getHasLogin());
      values.add(entity.getEmailVerificationToken());

      return values;
   }

   @Override
   protected Person createEntity() {
      return new Person();
   }

   @Override
   protected void populateEntity(Row row, Person entity) {
      entity.setAccountId(row.getUUID(PersonEntityColumns.ACCOUNT_ID));
      entity.setCurrLocation(row.getString(PersonEntityColumns.CURRENT_LOCATION));
      entity.setCurrLocationMethod(row.getString(PersonEntityColumns.CURRENT_LOCATION_METHOD));
      entity.setCurrLocationTime(row.getTimestamp(PersonEntityColumns.CURRENT_LOCATION_TIME));
      entity.setCurrPlace(row.getUUID(PersonEntityColumns.CURRENT_PLACE));
      entity.setCurrPlaceMethod(row.getString(PersonEntityColumns.CURRENT_PLACE_METHOD));
      entity.setEmailVerified(row.getTimestamp(PersonEntityColumns.EMAIL_VERIFIED));
      entity.setMobileNotificationEndpoints(row.getList(PersonEntityColumns.MOBILE_NOTIFICATION_ENDPOINTS, String.class));
      String mobileNumberStr = row.getString(PersonEntityColumns.MOBILE_NUMBER);
      try {
         PhoneNumber phone = PhoneNumbers.fromString(mobileNumberStr);
         if(phone != null) {
         	entity.setMobileNumber(PhoneNumbers.format(phone, PhoneNumberFormat.PARENS));
         }
      }catch(IllegalArgumentException e) {      	
      	logger.warn("Error retrieving person's mobile number from DB.", e);
      	//TODO - what do we with existing invalid phone numbers since we have not validated them until now?
      	entity.setMobileNumber(mobileNumberStr);
      }
      entity.setMobileVerified(row.getTimestamp(PersonEntityColumns.MOBILE_VERIFIED));
      entity.setFirstName(row.getString(PersonEntityColumns.FIRST_NAME));
      entity.setLastName(row.getString(PersonEntityColumns.LAST_NAME));

      Map<String,String> pinPerPlace = row.getMap(PersonEntityColumns.PINPERPLACE, String.class, String.class);
      if(pinPerPlace != null && !pinPerPlace.isEmpty()) {
         Map<String,String> decrypted = new HashMap<>(pinPerPlace);
         decrypted.replaceAll((k,v) -> { return aes.decrypt(entity.getId().toString(), v); });
         entity.setPinPerPlace(decrypted);
      } else {
         String pin = row.getString(PersonEntityColumns.PIN2);
         if(pin == null) {
            pin = row.getString(PersonEntityColumns.PIN);
            if(pin != null) {
               ColumnRepairMetrics.incPinCounter();
            }
         }

         if(pin != null) {
            if(entity.getCurrPlace() == null) {
               logger.warn("Unable to determine current place -- can't set pin");
            }
            else {
               entity.setPinAtPlace(entity.getCurrPlace(), aes.decrypt(entity.getId().toString(), pin));
            }
         }
         else {
            logger.debug("User [{}] does not have a pin", entity.getId());
         }
      }

      entity.setTermsAgreed(row.getTimestamp(PersonEntityColumns.TERMS_AGREED));
      entity.setPrivacyPolicyAgreed(row.getTimestamp(PersonEntityColumns.PRIVACY_POLICY_AGREED));
      entity.setConsentOffersPromotions(row.getTimestamp(PersonEntityColumns.CONSENT_OFFERSPROMOTIONS));
      entity.setConsentStatement(row.getTimestamp(PersonEntityColumns.CONSENT_STATEMENT));

      Map<String,String> securityAnswers = row.getMap(PersonEntityColumns.SECURITY_ANSWERS, String.class, String.class);
      if(securityAnswers != null && !securityAnswers.isEmpty()) {
         securityAnswers = new HashMap<>(securityAnswers);
         securityAnswers.replaceAll((k,v) -> { return aesDecrypt(questionsAesSecret, k, v); });
         entity.setSecurityAnswers(securityAnswers);
      }

      entity.setHasLogin(row.getBool(PersonEntityColumns.HAS_LOGIN));
      entity.setEmail(row.getString(PersonEntityColumns.EMAIL));
      entity.setEmailVerificationToken(row.getString(PersonEntityColumns.EMAIL_VERIFICATION_TOKEN));
   }

   @Override
   protected Person doInsert(UUID id, Person entity) {
      Person copy = entity.copy();
      copy.setId(id);
      copy = super.doInsert(id, copy);
      session.execute(new BoundStatement(initMobileDeviceSequence).bind(id));
      return copy;
   }

   protected Person doInsert(UUID id, Person person, String password) {
      Person copy = person.copy();
      boolean success = false;
      insertLoginIndex(person.getEmail(), id, password);
      try {
         copy.setId(id);
         copy.setHasLogin(true);
         copy = super.doInsert(id, copy);
         success = true;
      }
      finally {
         if(!success) {
            deleteLoginIndex(copy.getEmail());
         }
      }
      session.execute(new BoundStatement(initMobileDeviceSequence).bind(id));
      return copy;
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.cassandra.BaseCassandraCRUDDao#doUpdate(com.iris.messages.model.BaseEntity)
    */
   @Override
   protected Person doUpdate(Person entity) {
      if(!entity.getHasLogin()) {
         return super.doUpdate(entity);
      }
      // else need to manage the email index

      Person copy = entity.copy();
      copy.setModified(new Date());

      BoundStatement bs = bindUpdate(copy, copy.getEmail());
      ResultSet rs = session.execute(bs);
      if(!rs.wasApplied()) {
         throw new EmailMismatchException(entity.getEmail());
      }
      return copy;
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.PersonDAO#create(com.iris.messages.model.Person, java.lang.String)
    */
   @Override
   public Person create(Person person, String password) throws EmailInUseException {
      Preconditions.checkArgument(person.getId() == null, "can't create a person with an existing id");
      Preconditions.checkNotNull(password, "password may not be null for a credentialed user");
      validateAndFormatMobileNumber(person);
      
      UUID id = nextId(person);
      return doInsert(id, person, password);
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.PersonDAO#createPersonWithNoLogin(com.iris.messages.model.Person)
    */
   @Override
   public Person createPersonWithNoLogin(Person person) {
      Preconditions.checkArgument(person.getId() == null, "can't create a person with an existing id");
      Preconditions.checkArgument(person.getCreated() == null, "can't create a person with a created date");
      validateAndFormatMobileNumber(person);
      
      Person copy = person.copy();
      copy.setHasLogin(false);
      return super.save(copy);
   }

   /* (non-Javadoc)
    * @see com.iris.core.dao.PersonDAO#update(com.iris.messages.model.Person)
    */
   @Override
   public Person update(Person person) {
   	validateAndFormatMobileNumber(person);
      Preconditions.checkNotNull(person.getId(), "can't update a person with no id");
      Preconditions.checkNotNull(person.getCreated(), "can't update a non-persisted person");
      return super.save(person);
   }
      

   @Override
   public Person findByEmail(String email) {
      if(StringUtils.isBlank(email)) {
         return null;
      }
      try(Context ctxt = findByEmailTimer.time()) {
    	  UUID personId = findIdByEmail(email);
          return personId == null ? null : findById(personId);
      }
   }

   @Override
   public void delete(Person entity) {
      if(entity.getHasLogin()) {
         deleteLoginIndex(entity.getEmail());
      }
      session.execute(new BoundStatement(deleteMobileDevices).bind(entity.getId()));
      super.delete(entity);
   }

   @Override
   public boolean updatePassword(String email, String oldPassword, String newPassword) {
      Preconditions.checkArgument(!StringUtils.isBlank(email), "email must not be blank");
      Preconditions.checkArgument(!StringUtils.isBlank(newPassword), "newPassword must not be blank");

      CredentialsHashingStrategy hashingStrategy = ServiceLocator.getInstance(CredentialsHashingStrategy.class);
      if(hashingStrategy == null) {
         throw new IllegalStateException("No credentials hashing strategy has been found, please be sure that a concrete implementation of CredentialsHashingStrategy has been injected.");
      }

      ParsedEmail parsed = ParsedEmail.parse(email);

      oldPassword = StringUtils.trimToNull(oldPassword);
      Login curLogin = findLogin(email);

      if(curLogin != null && hashingStrategy.isSalted()) {
         oldPassword = hashingStrategy.hashCredentials(oldPassword, hashingStrategy.saltAsBytes(curLogin.getPasswordSalt()));
      }

      if(!StringUtils.equals(oldPassword, curLogin.getPassword() == null ? null : curLogin.getPassword())) {
         return false;
      }

      try(Context ctxt = updatePasswordTimer.time()) {
         return updatePassword(parsed, newPassword);
      }
   }

   @Override
   public Login findLogin(String username) {
      Row row;
      try(Context ctxt = findLoginTimer.time()) {
         row = findLoginRowByUsername(username);
      }

      if(row == null) {
         return null;
      }

      Login login = new Login();
      login.setPassword(row.getString(LoginColumns.PASSWORD));
      login.setPasswordSalt(row.getString(LoginColumns.PASSWORD_SALT));
      login.setUserId(row.getUUID(LoginColumns.USERID));
      login.setUsername(username);
      login.setLastPasswordChange(row.getTimestamp(LoginColumns.LAST_PASS_CHANGE));
      return login;
   }

   private static final int TOKEN_LENGTH = 6;

   @Override
   public String generatePasswordResetToken(String email) {
      Preconditions.checkNotNull(email, "Email must be provided");

      try(Context ctxt = generatePasswordResetTokenTimer.time()) {
         String token = TokenUtil.randomTokenString(TOKEN_LENGTH);

         ParsedEmail parsed = ParsedEmail.parse(email);
         Statement stmt = QueryBuilder.update("login")
               .using(QueryBuilder.ttl(tokenTTLMinutes * 60))
               .with(QueryBuilder.set(LoginColumns.RESET_TOKEN, token))
               .where(QueryBuilder.eq(LoginColumns.DOMAIN, parsed.getDomain()))
               .and(QueryBuilder.eq(LoginColumns.USER_0_3, parsed.getUser_0_3()))
               .and(QueryBuilder.eq(LoginColumns.USER, parsed.getUser()));
         session.execute(stmt);
         return token.toString();
      }
   }

   @Override
   public ResetPasswordResult resetPassword(String email, String token, String password) {
      if(StringUtils.isBlank(email)) {
         return FAILURE;
      }

      if(StringUtils.isBlank(token)) {
         return FAILURE;
      }

      if(StringUtils.isBlank(password)) {
         return FAILURE;
      }

      ParsedEmail parsed = ParsedEmail.parse(email);

      try(Context ctxt = resetPasswordTimer.time()) {
         BoundStatement boundStatement = new BoundStatement(findLoginByEmail);
         Row row = session.execute(boundStatement.bind(parsed.getDomain(), parsed.getUser_0_3(), parsed.getUser())).one();

         if(row == null) {
            return FAILURE;
         }

         if(!Objects.equal(token, row.getString(LoginColumns.RESET_TOKEN))) {
            return TOKEN_FAILURE;
         }

         boolean succeeded = updatePassword(parsed, password);

         if(succeeded) {
            Statement stmt = QueryBuilder.update("login")
                  .with(QueryBuilder.set(LoginColumns.RESET_TOKEN, null))
                  .where(QueryBuilder.eq(LoginColumns.DOMAIN, parsed.getDomain()))
                  .and(QueryBuilder.eq(LoginColumns.USER_0_3, parsed.getUser_0_3()))
                  .and(QueryBuilder.eq(LoginColumns.USER, parsed.getUser()));
            session.execute(stmt);
         }

         return succeeded ? SUCCESS : FAILURE;
      }
   }

   @Override
   public Person updatePinAtPlace(Person person, UUID placeId, String newPin) throws PinNotUniqueAtPlaceException
   {
      Preconditions.checkArgument(person != null, "person cannot be null");
      Preconditions.checkArgument(placeId != null, "placeId cannot be null");
      Preconditions.checkArgument(StringUtils.isNotBlank(newPin), "newPin cannot be blank");

      try (Context timerContext = updatePinAtPlaceTimer.time())
      {
         List<Person> personsAtPlace = listByPlaceId(placeId);

         verifyNewPinUniqueness(personsAtPlace, placeId, newPin);

         Date modified = new Date();

         String encryptedNewPin = aes.encrypt(person.getId().toString(), newPin);

         boolean isCurrentPlace = Objects.equal(person.getCurrPlace(), placeId);

         Statement updateStatement = isCurrentPlace ?
            new BoundStatement(updatePinAtPlaceAndPin2)
               .bind(modified, placeId.toString(), encryptedNewPin, encryptedNewPin, person.getId()) :
            new BoundStatement(updatePinAtPlace)
               .bind(modified, placeId.toString(), encryptedNewPin, person.getId());

         session.execute(updateStatement);

         Person copy = person.copy();
         copy.setModified(modified);
         copy.setPinAtPlace(placeId, newPin);

         return copy;
      }
   }

   private List<Person> listByPlaceId(UUID placeId)
   {
      try (Context timerContext = listByPlaceIdTimer.time())
      {
         Set<UUID> personIds = personPlaceAssocDAO.findPersonIdsByPlace(placeId);

         Function<ResultSet, Person> entityTransform =
            resultSet -> buildEntity(resultSet.one());

         return listByIdsAsync(personIds, entityTransform, asyncTimeoutMs);
      }
   }

   private void verifyNewPinUniqueness(List<Person> personsAtPlace, UUID placeId, String newPin)
      throws PinNotUniqueAtPlaceException
   {
      for (Person personAtPlace : personsAtPlace)
      {
         if (personAtPlace.hasPinAtPlace(placeId))
         {
            String pinAtPlace = personAtPlace.getPinAtPlace(placeId);

            if (StringUtils.equals(pinAtPlace, newPin))
            {
               throw new PinNotUniqueAtPlaceException();
            }
         }
      }
   }

   @Override
   public Person deletePinAtPlace(Person person, UUID placeId)
   {
      Preconditions.checkArgument(person != null, "person cannot be null");
      Preconditions.checkArgument(placeId != null, "placeId cannot be null");

      try (Context timerContext = updatePinAtPlaceTimer.time())
      {
         Date modified = new Date();

         boolean isCurrentPlace = Objects.equal(person.getCurrPlace(), placeId);

         Statement deleteStatement = isCurrentPlace ?
            new BoundStatement(updatePinAtPlaceAndPin2).bind(modified, placeId.toString(), null, null, person.getId()) :
            new BoundStatement(updatePinAtPlace).bind(modified, placeId.toString(), null, person.getId());

         session.execute(deleteStatement);

         Person copy = person.copy();
         copy.setModified(modified);
         copy.clearPin(placeId);

         return copy;
      }
   }

   @Override
   public Person updatePersonAndEmail(Person person, String currentLoginEmail) {
      if(!person.getHasLogin()) {
         return update(person);
      }

      String newLoginEmail = person.getEmail();
      ParsedEmail oldParsedEmail = ParsedEmail.parse(person.getEmail());
      if (!oldParsedEmail.isValid()) {
         throw new IllegalArgumentException("Old email address is not valid: " + currentLoginEmail);
      }
      ParsedEmail newParsedEmail = ParsedEmail.parse(newLoginEmail);
      if (!newParsedEmail.isValid()) {
         throw new IllegalArgumentException("New email address is not valid: " + newLoginEmail);
      }

      Row row = findLoginRowByUsername(currentLoginEmail);
      if (row == null) {
         throw new NotFoundException(Address.fromString(person.getAddress()));
      }

      String password = row.getString(LoginColumns.PASSWORD);
      String password_salt = row.getString(LoginColumns.PASSWORD_SALT);
      UUID userId = row.getUUID(LoginColumns.USERID);
      Date lastPassChange = row.getTimestamp(LoginColumns.LAST_PASS_CHANGE);
      BoundStatement insert = new BoundStatement(insertLogin)
         .setString(LoginColumns.DOMAIN, newParsedEmail.getDomain())
         .setString(LoginColumns.USER_0_3, newParsedEmail.getUser_0_3())
         .setString(LoginColumns.USER, newParsedEmail.getUser())
         .setString(LoginColumns.PASSWORD, password)
         .setString(LoginColumns.PASSWORD_SALT, password_salt)
         .setUUID(LoginColumns.USERID, userId)
         // changing the email invalidates the reset token
         .setString(LoginColumns.RESET_TOKEN, null)
         .setTimestamp(LoginColumns.LAST_PASS_CHANGE, lastPassChange);

      Person copy = person.copy();
      copy.setModified(new Date());
      copy.setEmailVerificationToken(null);
      copy.setEmailVerified(null);  //clear emailverified date and token
      BoundStatement update = bindUpdate(copy, currentLoginEmail);

      try(Context ctxt = updatePersonAndEmailTimer.time()) {
         ResultSet rs = session.execute(insert);
         if(!rs.wasApplied()) {
            throw new EmailInUseException(newLoginEmail);
         }
         boolean success = false;
         try {
            ResultSet updateRs = session.execute(update);
            if(!updateRs.wasApplied()) {
               throw new EmailMismatchException(currentLoginEmail);
            }
            success = true;
         }
         finally {
            if(!success) {
               deleteLoginIndex(newLoginEmail);
            }
         }
         deleteLoginIndex(currentLoginEmail);
      }

      return copy;
   }

   @Override
   public void setUpdateFlag(UUID personId, boolean updateFlag) {
      Preconditions.checkArgument(personId != null, "The person id cannot be null");
      BoundStatement statement = new BoundStatement(setUpdateFlag);
      try(Context ctxt = setUpdateFlagTimer.time()) {
         session.execute(statement.bind(updateFlag, personId));
      }
   }

   @Override
   public boolean getUpdateFlag(UUID personId) {
      Preconditions.checkArgument(personId != null, "The person id cannot be null");
      BoundStatement statement = new BoundStatement(getUpdateFlag);
      ResultSet resultSet;
      try(Context ctxt = getUpdateFlagTimer.time()) {
         resultSet = session.execute(statement.bind(personId));
      }
      Row row = resultSet.one();
      return row.getBool(UPDATEFLAG);
   }

   private BoundStatement bindUpdate(Person person, String currentEmail) {
      List<Object> values = new ArrayList<Object>(COLUMN_ORDER.length + 5);
      values.add(person.getModified());
      values.add(person.getTags());
      values.add(person.getImages());
      values.addAll(getValues(person));
      values.add(person.getId());
      values.add(currentEmail);

      // use an optimistic update to prevent inadvertently changing the
      // email address and corrupting the index
      BoundStatement bs = new BoundStatement(updatePersonOptimistic);
      return bs.bind(values.toArray());
   }

   private BoundStatement bindDeleteLogin(ParsedEmail parsed) {
      return
         new BoundStatement(deleteIndex)
            .setString(LoginColumns.DOMAIN, parsed.getDomain())
            .setString(LoginColumns.USER_0_3, parsed.getUser_0_3())
            .setString(LoginColumns.USER, parsed.getUser());   }

   private boolean updatePassword(ParsedEmail parsed, String password) {
      CredentialsHashingStrategy hashingStrategy = ServiceLocator.getInstance(CredentialsHashingStrategy.class);
      if(hashingStrategy == null) {
         throw new IllegalStateException("No credentials hashing strategy has been found, please be sure that a concrete implementation of CredentialsHashingStrategy has been injected.");
      }

      List<String> hashAndSalt = generateHashAndSalt(password);
      BoundStatement update = new BoundStatement(updatePassword);
      ResultSet rs = session.execute(update.bind(hashAndSalt.get(0), hashAndSalt.get(1), new Date(), parsed.getDomain(), parsed.getUser_0_3(), parsed.getUser()));
      return rs.wasApplied();
   }

   private List<String> generateHashAndSalt(String password) {
      CredentialsHashingStrategy hashingStrategy = ServiceLocator.getInstance(CredentialsHashingStrategy.class);
      if(hashingStrategy == null) {
         throw new IllegalStateException("No credentials hashing strategy has been found, please be sure that a concrete implementation of CredentialsHashingStrategy has been injected.");
      }

      String hashedPassword = password;
      String salt = null;

      if(hashingStrategy.isSalted()) {
         ByteSource saltBytes = hashingStrategy.generateSalt();
         salt = saltBytes.toBase64();
         hashedPassword = hashingStrategy.hashCredentials(password,  saltBytes);
      }
      return ImmutableList.of(hashedPassword, salt);
   }

   @Override
   protected UUID getIdFromRow(Row row) {
      return row.getUUID(BaseEntityColumns.ID);
   }

   @Override
   protected UUID nextId(Person person) {
      return UUID.randomUUID();
   }

   private Row findLoginRowByUsername(String username) {
      ParsedEmail parsed = ParsedEmail.parse(username);
      BoundStatement boundStatement = new BoundStatement(findLoginByEmail);
      return session.execute(boundStatement.bind(parsed.getDomain(), parsed.getUser_0_3(), parsed.getUser())).one();
   }

   private void insertLoginIndex(String email, UUID id, String password) {
      ParsedEmail parsed = ParsedEmail.parse(email);
      List<String> hashAndSalt = generateHashAndSalt(password);
      BoundStatement boundStatement = new BoundStatement(insertLogin);
      ResultSet rs = session.execute(boundStatement.bind(parsed.getDomain(), parsed.getUser_0_3(), parsed.getUser(), hashAndSalt.get(0), hashAndSalt.get(1), id, null, new Date()));
      if(!rs.wasApplied()) {
         throw new EmailInUseException(email);
      }
   }

   private void deleteLoginIndex(String email) {
      ParsedEmail parsed = ParsedEmail.parse(email);
      BoundStatement boundStatement = bindDeleteLogin(parsed);
      session.execute(boundStatement);
   }

   private UUID findIdByEmail(String email) {
      Login login = findLogin(email);
      return login == null ? null : login.getUserId();
   }

   @Override
   public PagedResults<Person> listPersons(PersonQuery query) {
	   BoundStatement bs = null;
	   if (query.getToken() != null) {
		   bs = listPaged.bind(UUID.fromString(query.getToken()), query.getLimit() + 1);
	   } else {
		   bs = findAllPeopleLimit.bind(query.getLimit() + 1);
	   }
	   try(Context ctxt = listPersonsTimer.time()) {
         return doList(bs, query.getLimit());
      }
   }

   @Override
   public Stream<Person> streamAll() {
      try(Context ctxt = streamAllTimer.time()) {
         Iterator<Row> rows = session.execute(new BoundStatement(findAllPeople)).iterator();
         Iterator<Person> result = Iterators.transform(rows, (row) -> buildEntity(row));
         Spliterator<Person> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
         return StreamSupport.stream(stream, false);
      }
   }

   private PreparedStatement prepareOptimisticUpdate() {
   	PreparedStatement stmt = session.prepare(OPTIMISTIC_UPDATE);
   	stmt.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
   	return stmt;
   }

   private PreparedStatement prepareInsertLogin() {
      return CassandraQueryBuilder.insert(Tables.LOGIN)
         .addColumn(LoginColumns.DOMAIN)
         .addColumn(LoginColumns.USER_0_3)
         .addColumn(LoginColumns.USER)
         .addColumn(LoginColumns.PASSWORD)
         .addColumn(LoginColumns.PASSWORD_SALT)
         .addColumn(LoginColumns.USERID)
         .addColumn(LoginColumns.RESET_TOKEN)
         .addColumn(LoginColumns.LAST_PASS_CHANGE)
         .ifNotExists()
         .prepare(session);
   }

   private PreparedStatement prepareFindLoginByEmail() {
   	return CassandraQueryBuilder.select(Tables.LOGIN)
   	            .addColumns(LoginColumns.DOMAIN, LoginColumns.USER_0_3, LoginColumns.USER, LoginColumns.PASSWORD, LoginColumns.PASSWORD_SALT, LoginColumns.USERID, LoginColumns.RESET_TOKEN, LoginColumns.LAST_PASS_CHANGE)
   					.addWhereColumnEquals(LoginColumns.DOMAIN)
   					.addWhereColumnEquals(LoginColumns.USER_0_3)
   					.addWhereColumnEquals(LoginColumns.USER)
   					.prepare(session);
   }

   private PreparedStatement prepareDeleteIndex() {
   	return CassandraQueryBuilder.delete(Tables.LOGIN)
		   			.addWhereColumnEquals(LoginColumns.DOMAIN)
						.addWhereColumnEquals(LoginColumns.USER_0_3)
						.addWhereColumnEquals(LoginColumns.USER)
						.prepare(session);
   }

   private PreparedStatement prepareUpdatePassword() {
      return CassandraQueryBuilder.update(Tables.LOGIN)
         .addColumn(LoginColumns.PASSWORD)
         .addColumn(LoginColumns.PASSWORD_SALT)
         .addColumn(LoginColumns.LAST_PASS_CHANGE)
         .addWhereColumnEquals(LoginColumns.DOMAIN)
         .addWhereColumnEquals(LoginColumns.USER_0_3)
         .addWhereColumnEquals(LoginColumns.USER)
         .prepare(session);
   }

   private PreparedStatement prepareUpdatePinAtPlace() {
      return CassandraQueryBuilder.update(TABLE)
         .addColumn(BaseEntityColumns.MODIFIED)
         .addColumn(PersonEntityColumns.PINPERPLACE + "[?]")
         .addWhereColumnEquals(BaseEntityColumns.ID)
         .prepare(session);
   }

   private PreparedStatement prepareUpdatePinAtPlaceAndPin2() {
      return CassandraQueryBuilder.update(TABLE)
         .addColumn(BaseEntityColumns.MODIFIED)
         .addColumn(PersonEntityColumns.PINPERPLACE + "[?]")
         .addColumn(PersonEntityColumns.PIN2)
         .addWhereColumnEquals(BaseEntityColumns.ID)
         .prepare(session);
   }

   private PreparedStatement prepareDeleteMobileDevices() {
   	return CassandraQueryBuilder.delete(Tables.MOBILE_DEVICES)
   					.addWhereColumnEquals(Tables.MobileDevicesCols.PERSON_ID)
   					.prepare(session);
   }

   private PreparedStatement prepareInitMobileDeviceSequence() {
   	return CassandraQueryBuilder.update(TABLE)
   					.set("mobileDeviceSequence=0")
   					.addWhereColumnEquals(BaseEntityColumns.ID)
   					.prepare(session);
   }

   private PreparedStatement prepareSetUpdateFlag() {
   	return CassandraQueryBuilder.update(TABLE)
   					.addColumn(UPDATEFLAG)
   					.addWhereColumnEquals(BaseEntityColumns.ID)
   					.prepare(session);
   }

   private PreparedStatement prepareGetUpdateFlag() {
   	return CassandraQueryBuilder.select(TABLE)
   					.addColumn(UPDATEFLAG)
   					.addWhereColumnEquals(BaseEntityColumns.ID)
   					.prepare(session);
   }

   private PreparedStatement prepareFindAllPeople() {
   	CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(TABLE);
   	return addAllColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareFindAllPeopleLimit() {
   	CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(TABLE)
   					.boundLimit();
   	return addAllColumns(queryBuilder).prepare(session);
   }

   private PreparedStatement prepareListPaged() {
      CassandraQueryBuilder queryBuilder = CassandraQueryBuilder.select(TABLE)
            .where("token(" + BaseEntityColumns.ID + ") >= token(?) LIMIT ?");
      return addAllColumns(queryBuilder).prepare(session);
   }

   private CassandraQueryBuilder addAllColumns(CassandraQueryBuilder queryBuilder) {
      queryBuilder.addColumns(BASE_COLUMN_ORDER).addColumns(COLUMN_ORDER).addColumns(READ_ONLY_COLUMN_ORDER);
      return selectNonEntityColumns(queryBuilder);
   }
   
   private void validateAndFormatMobileNumber(Person person) {
   	//Validate and format phone number
   	PhoneNumber phone1 = PhoneNumbers.fromString(person.getMobileNumber());
   	if(phone1 != null) {
   		person.setMobileNumber(PhoneNumbers.format(phone1, PhoneNumberFormat.PARENS));
   	}
   }

   /**
    * Wrapper to preserve existing encrypted secrets while allowing for migration.
    *
    * @param secretStr
    * @param key
    * @param encrypted
    * @return
    */
   private String aesDecrypt(String secretStr, String key, String encrypted) {
      try {
         // Don't use secretStr - set by injection.
         return aes.decrypt(key, encrypted);
      } catch (RuntimeException e) {
         return Utils.aesDecrypt(secretStr, encrypted);
      }
   }
}

