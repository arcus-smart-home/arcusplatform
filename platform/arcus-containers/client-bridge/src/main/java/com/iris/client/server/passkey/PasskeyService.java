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
package com.iris.client.server.passkey;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.BreakIterator;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PasskeyDAO;
import com.iris.core.dao.PasskeyDAO.PasskeyRequest;
import com.iris.core.dao.PersonDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.security.passkey.Passkey;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

/**
 * Registers passkeys and verifies passkey logins (WebAuthn). Each ceremony has two
 * steps: start returns options for the browser along with a request id, and finish
 * takes the browser's response to that request. Requests are single-use and expire.
 *
 * Passkeys are discoverable and require user verification, so login doesn't need a
 * username and the passkey stands in for both the password and a second factor.
 *
 * Adding or removing a passkey emails the person, since a passkey someone else adds
 * keeps working after a password change.
 */
@Singleton
public class PasskeyService {
   private static final Logger logger = LoggerFactory.getLogger(PasskeyService.class);

   /** In code points; longer names are cut where a whole character ends. */
   static final int MAX_NAME_LENGTH = 64;
   private static final String DEFAULT_NAME = "Passkey";
   static final int MAX_PASSKEYS = 20;

   /** How much sooner than the request expires the browser is told to give up. */
   private static final long MAX_TIMEOUT_MARGIN_MILLIS = TimeUnit.SECONDS.toMillis(30);

   private final PasskeyConfig config;
   private final PasskeyDAO passkeyDao;
   private final PlatformMessageBus platformBus;
   private final @Nullable RelyingParty relyingParty;
   private final SecureRandom random = new SecureRandom();

   @Inject
   public PasskeyService(PasskeyConfig config, PasskeyDAO passkeyDao, PersonDAO personDao, PlatformMessageBus platformBus) {
      this.config = config;
      this.passkeyDao = passkeyDao;
      this.platformBus = platformBus;
      this.relyingParty = !config.isEnabled() ? null : RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder().id(config.getRpId()).name(config.getRpName()).build())
            .credentialRepository(new PasskeyCredentialRepository(passkeyDao, personDao))
            .origins(config.getOrigins())
            .build();
      if (config.isEnabled()) {
         logger.info("Passkeys enabled for [{}] from {}", config.getRpId(), config.getOrigins());
      }
   }

   public boolean isEnabled() {
      return relyingParty != null;
   }

   public PendingRequest startRegistration(Person person) throws PasskeyException {
      RelyingParty rp = relyingParty();
      // the same check as PasskeyRealm makes at login
      if (!person.getHasLogin() || StringUtils.isBlank(person.getEmail())) {
         throw new PasskeyException("Only a person with a login can register a passkey");
      }
      checkPasskeyLimit(person.getId());
      String displayName = StringUtils.trimToNull(StringUtils.defaultString(person.getFirstName()) + " " + StringUtils.defaultString(person.getLastName()));
      PublicKeyCredentialCreationOptions options = rp.startRegistration(
            StartRegistrationOptions.builder()
               .user(UserIdentity.builder()
                     .name(person.getEmail())
                     .displayName(displayName != null ? displayName : person.getEmail())
                     .id(PasskeyCredentialRepository.userHandle(person.getId()))
                     .build())
               .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                     .residentKey(ResidentKeyRequirement.REQUIRED)
                     .userVerification(UserVerificationRequirement.REQUIRED)
                     .build())
               .timeout(timeoutMillis())
               .build());
      try {
         return saveRequest(person.getId(), options.toJson(), options.toCredentialsCreateJson());
      }
      catch (IOException e) {
         throw new IllegalStateException("Unable to serialize registration options", e);
      }
   }

   public Passkey finishRegistration(Person person, String requestId, String credential, @Nullable String name) throws PasskeyException {
      RelyingParty rp = relyingParty();
      PasskeyRequest request = takeRequest(requestId);
      if (!Objects.equals(person.getId(), request.getPersonId())) {
         throw new PasskeyException("Unknown or expired request");
      }
      PublicKeyCredentialCreationOptions options;
      RegistrationResult result;
      try {
         options = PublicKeyCredentialCreationOptions.fromJson(request.getRequest());
         result = rp.finishRegistration(
               FinishRegistrationOptions.builder()
                  .request(options)
                  .response(com.yubico.webauthn.data.PublicKeyCredential.parseRegistrationResponseJson(credential))
                  .build());
      }
      catch (RegistrationFailedException | IOException | IllegalArgumentException e) {
         logger.debug("Passkey registration failed for person [{}]", person.getId(), e);
         throw new PasskeyException("Passkey registration failed");
      }
      // checked again here, since several registrations can be started at once
      checkPasskeyLimit(person.getId());

      Passkey passkey = new Passkey();
      passkey.setPersonId(person.getId());
      passkey.setCredentialId(result.getKeyId().getId().getBase64Url());
      passkey.setUserHandle(options.getUser().getId().getBase64Url());
      passkey.setPublicKey(result.getPublicKeyCose().getBase64Url());
      passkey.setSignCount(result.getSignatureCount());
      passkey.setName(normalizeName(name));
      passkey.setCreated(Instant.now());
      if (!passkeyDao.save(passkey)) {
         // a passkey row with this id is already stored for the person, from a save that
         // didn't finish; its key, not this one, is what logins would be checked against
         logger.warn("person=[{}] already has a stored row for passkey [{}]; not registering it again", person.getId(), passkey.getCredentialId());
         throw new PasskeyException("Passkey registration failed");
      }
      logger.info("person=[{}] registered passkey [{}]", person.getId(), passkey.getCredentialId()); // Audit event
      sendNotice(person.getId(), Notifications.PasskeyAdded.KEY, Notifications.PasskeyAdded.PARAM_NAME, passkey.getName());
      return passkey;
   }

   public PendingRequest startLogin() throws PasskeyException {
      RelyingParty rp = relyingParty();
      AssertionRequest request = rp.startAssertion(
            StartAssertionOptions.builder()
               .userVerification(UserVerificationRequirement.REQUIRED)
               .timeout(timeoutMillis())
               .build());
      try {
         return saveRequest(null, request.toJson(), request.toCredentialsGetJson());
      }
      catch (IOException e) {
         throw new IllegalStateException("Unable to serialize login options", e);
      }
   }

   /**
    * @return the id of the person the passkey belongs to
    */
   public UUID finishLogin(String requestId, String credential) throws PasskeyException {
      RelyingParty rp = relyingParty();
      PasskeyRequest request = takeRequest(requestId);
      if (request.getPersonId() != null) {
         // a registration request, not a login one
         throw new PasskeyException("Unknown or expired request");
      }
      try {
         AssertionResult result = rp.finishAssertion(
               FinishAssertionOptions.builder()
                  .request(AssertionRequest.fromJson(request.getRequest()))
                  .response(com.yubico.webauthn.data.PublicKeyCredential.parseAssertionResponseJson(credential))
                  .build());
         if (!result.isSuccess()) {
            throw new PasskeyException("Passkey login failed");
         }
         UUID personId = PasskeyCredentialRepository.personId(result.getCredential().getUserHandle())
               .orElseThrow(() -> new PasskeyException("Passkey login failed"));
         if (!passkeyDao.updateUsage(personId, result.getCredential().getCredentialId().getBase64Url(), result.getSignatureCount(), Instant.now())) {
            // the passkey was removed while this login was being checked
            throw new PasskeyException("Passkey login failed");
         }
         return personId;
      }
      catch (AssertionFailedException | IOException | IllegalArgumentException e) {
         logger.debug("Passkey login failed", e);
         throw new PasskeyException("Passkey login failed");
      }
   }

   public List<Passkey> list(UUID personId) {
      List<Passkey> passkeys = passkeyDao.findByPerson(personId);
      // names stored before the current rules are shown the same way as the emails show them
      passkeys.forEach(passkey -> passkey.setName(normalizeName(passkey.getName())));
      return passkeys;
   }

   /**
    * @return false if the person has no passkey with that id
    */
   public boolean delete(UUID personId, String credentialId) {
      Optional<Passkey> owned = passkeyDao.findByPerson(personId).stream()
            .filter(passkey -> passkey.getCredentialId().equals(credentialId))
            .findFirst();
      if (!owned.isPresent()) {
         return false;
      }
      if (!passkeyDao.delete(personId, credentialId)) {
         // another request deleted it first, and sent the notice
         return false;
      }
      logger.info("person=[{}] removed passkey [{}]", personId, credentialId); // Audit event
      sendNotice(personId, Notifications.PasskeyRemoved.KEY, Notifications.PasskeyRemoved.PARAM_NAME, normalizeName(owned.get().getName()));
      return true;
   }

   /** Emails the person; LOW priority goes by email only, like a password change notice. */
   private void sendNotice(UUID personId, String key, String nameParam, String passkeyName) {
      platformBus.send(Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(key)
            .addMsgParam(nameParam, passkeyName)
            .create());
   }

   private void checkPasskeyLimit(UUID personId) throws PasskeyException {
      if (passkeyDao.findByPerson(personId).size() >= MAX_PASSKEYS) {
         throw new TooManyPasskeysException("You already have " + MAX_PASSKEYS + " passkeys, the most allowed. Remove one to add another.");
      }
   }

   /**
    * The browser's prompt timeout. It's a tenth shorter than the request's lifetime (at
    * most 30 seconds shorter), since the browser starts counting after the request was
    * stored and its answer takes time to arrive.
    */
   private long timeoutMillis() {
      long ttl = TimeUnit.SECONDS.toMillis(config.getRequestTtlSecs());
      return ttl - Math.min(MAX_TIMEOUT_MARGIN_MILLIS, ttl / 10);
   }

   private RelyingParty relyingParty() throws PasskeyException {
      if (relyingParty == null) {
         throw new PasskeyException("Passkeys aren't enabled");
      }
      return relyingParty;
   }

   private PendingRequest saveRequest(@Nullable UUID personId, String request, String options) {
      byte[] id = new byte[32];
      random.nextBytes(id);
      String requestId = new ByteArray(id).getBase64Url();
      passkeyDao.saveRequest(requestId, personId, request, config.getRequestTtlSecs());
      return new PendingRequest(requestId, options);
   }

   private PasskeyRequest takeRequest(String requestId) throws PasskeyException {
      if (StringUtils.isBlank(requestId)) {
         throw new PasskeyException("Unknown or expired request");
      }
      return passkeyDao.takeRequest(requestId).orElseThrow(() -> new PasskeyException("Unknown or expired request"));
   }

   /**
    * Names are shown only to their own person, in the passkey list and in the add and
    * remove emails. Whoever adds a passkey picks its name, though, and that may be someone
    * else who got into the account, so a few things are tightened up:
    * <ul>
    * <li>control characters (line breaks among them) and every kind of space become one
    * space, and the ends are trimmed;</li>
    * <li>formatting characters, which are drawn as nothing, are dropped: direction
    * overrides, zero-width spaces, soft hyphens, tag text. The zero-width joiner and
    * non-joiner stay, since emoji and scripts such as Persian need them;</li>
    * <li>half of a character, from malformed input, is dropped;</li>
    * <li>a name is cut to MAX_NAME_LENGTH code points, backing up so an emoji isn't cut in
    * half, and a blank one gets the default.</li>
    * </ul>
    * Otherwise a name is kept as typed.
    */
   static String normalizeName(@Nullable String name) {
      if (name == null) {
         return DEFAULT_NAME;
      }
      StringBuilder kept = new StringBuilder(name.length());
      name.codePoints().forEach(c -> {
         int type = Character.getType(c);
         if (type == Character.CONTROL || Character.isSpaceChar(c)) {
            kept.append(' ');
         }
         else if (type != Character.SURROGATE && (type != Character.FORMAT || c == 0x200C || c == 0x200D)) {
            kept.appendCodePoint(c);
         }
      });
      String cleaned = StringUtils.trimToNull(StringUtils.normalizeSpace(kept.toString()));
      if (cleaned == null) {
         return DEFAULT_NAME;
      }
      if (cleaned.codePointCount(0, cleaned.length()) <= MAX_NAME_LENGTH) {
         return cleaned;
      }
      int limit = cleaned.offsetByCodePoints(0, MAX_NAME_LENGTH);
      BreakIterator characters = BreakIterator.getCharacterInstance(Locale.ROOT);
      characters.setText(cleaned);
      int end = characters.isBoundary(limit) ? limit : characters.preceding(limit);
      // a single character longer than the limit is cut inside it
      return StringUtils.stripEnd(cleaned.substring(0, end > 0 ? end : limit), null);
   }

   /** Options to pass to the browser, and the id to send back with its response. */
   public static class PendingRequest {
      private final String requestId;
      private final String options;

      PendingRequest(String requestId, String options) {
         this.requestId = requestId;
         this.options = options;
      }

      public String getRequestId() {
         return requestId;
      }

      /**
       * The JSON for navigator.credentials.create() or .get(), {@code {"publicKey": {...}}},
       * with binary values base64url-encoded.
       */
      public String getOptions() {
         return options;
      }
   }

   @SuppressWarnings("serial")
   public static class PasskeyException extends Exception {
      public PasskeyException(String message) {
         super(message);
      }
   }

   /** The person already has {@link #MAX_PASSKEYS} passkeys. */
   @SuppressWarnings("serial")
   public static class TooManyPasskeysException extends PasskeyException {
      public TooManyPasskeysException(String message) {
         super(message);
      }
   }
}
