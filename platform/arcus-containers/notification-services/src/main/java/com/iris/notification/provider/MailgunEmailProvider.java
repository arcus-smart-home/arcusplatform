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
package com.iris.notification.provider;

import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.notification.Notifications;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.type.EmailRecipient;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.iris.platform.notification.provider.NotificationProviderUtil;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.Message.MessageBuilder;
import com.mailgun.model.message.MessageResponse;
import com.mailgun.util.EmailUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MailgunEmailProvider implements EmailProvider {
   private static Logger logger = LoggerFactory.getLogger(MailgunEmailProvider.class);
   
   private final PersonDAO personDao;
   private final PlaceDAO placeDao;
   private final AccountDAO accountDao;
   private final MailgunMessagesApi mailgunMessagesApiUS;
   private final NotificationMessageRenderer messageRenderer;
   private final UpstreamNotificationResponder responder;

   private final String domain;
   private final static String SENDER_NAME_SECTION = "sender-name";
   private final static String SENDER_EMAIL_SECTION = "sender-email";
   private final static String REPLYTO_EMAIL_SECTION = "replyto-email";
   private final static String SUBJECT_SECTION = "subject";
   private final static String PLAINTEXT_BODY_SECTION = "plaintext-body";
   private final static String HTML_BODY_SECTION = "html-body";
   private final static EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
   
   @Inject
   public MailgunEmailProvider(@Named("mailgunemail.provider.apikey") String mailgunApiKey, @Named("email.provider.domain") String domain, PersonDAO personDao, PlaceDAO placeDao, AccountDAO accountDao, NotificationMessageRenderer messageRenderer, UpstreamNotificationResponder responder) {
      this.domain = domain;
      this.mailgunMessagesApiUS = MailgunClient.config(mailgunApiKey).createApi(MailgunMessagesApi.class);
      this.personDao = personDao;
      this.placeDao = placeDao;
      this.accountDao = accountDao;
      this.messageRenderer = messageRenderer;
      this.responder = responder;
   }

   @Override
   public void notifyCustomer(Notification notification) throws DispatchException, DispatchUnsupportedByUserException {
      notifyCustomerOldEmail(notification);

      Message message = buildMessage(notification).build();
      if (message == null) return;

      String toEmail = message.getReplyTo();
      if (!isEmailValid(toEmail)) {
         logger.warn("Notification [{}] for placeId [{}] for person [{}] had invalid toEmail [{}].", notification.getMessageKey(), notification.getPlaceId(), notification.getPersonId(), toEmail == null ? "toEmail is null" : toEmail);
         return;
      }

      sendEmail(message);
      responder.handleHandOff(notification);
   }
    
   public void sendEmail(Message message) throws DispatchException {
      MessageResponse messageResponse = mailgunMessagesApiUS.sendMessage(this.domain, message);
      if (messageResponse.getId() == null || messageResponse.getId().isEmpty()) {
         logger.error("Failed to send email using Mailgun, no message ID returned. Response: {}", messageResponse);
         throw new DispatchException("Failed to send notification email. Reason: no message ID returned");
      }
   }

   public Boolean isEmailValid(String email) {
      if (StringUtils.isEmpty(email)) return false;

      return EMAIL_VALIDATOR.isValid(email);
   }

   private void notifyCustomerOldEmail(Notification notification) throws DispatchUnsupportedByUserException, DispatchException {

      //if the old email param is present this indicates the email has changed.
      Map<String, String> messageParams = notification.getMessageParams();
      if (messageParams == null || !messageParams.containsKey(Notifications.EmailChanged.PARAM_OLD_EMAIL)) return;

      String oldEmail = messageParams.get(Notifications.EmailChanged.PARAM_OLD_EMAIL);
      if (!isEmailValid(oldEmail)) {
         logger.warn("Notification [{}] for placeId [{}] for person [{}] has invalid oldEmail [{}].", notification.getMessageKey(), notification.getPlaceId(), notification.getPersonId(), oldEmail);
         return;
      }
      
      Map<String, BaseEntity<?, ?>> additionalEntityParams = NotificationProviderUtil.addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, notification);
      Person person = NotificationProviderUtil.getPersonFromParams(additionalEntityParams);
      EmailRecipient recipient = getRecipient(person, notification);

      MessageBuilder messageBuilder = buildMessage(notification);
      messageBuilder.to(EmailUtil.nameWithEmail(getPersonDisplayName(recipient), oldEmail));
      Message message = messageBuilder.build();
      if (message == null)
         return;
      sendEmail(message);
   }

   private MessageBuilder buildMessage(Notification notification) throws DispatchUnsupportedByUserException {
      // Collect recipient information
      Map<String, BaseEntity<?, ?>> additionalEntityParams = NotificationProviderUtil.addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, notification);
      Person person = NotificationProviderUtil.getPersonFromParams(additionalEntityParams);
      EmailRecipient recipient = getRecipient(person, notification);

      if (recipient == null) {
         throw new DispatchUnsupportedByUserException("No person or direct email address in notification");
      }

      // Recipient should have email address on file
      String recipientEmail = recipient.getEmail();
      if (!isEmailValid(recipientEmail)) {
         logger.warn("Notification [{}] for placeId [{}] for person [{}] contained invalid recipientEmail [{}].", notification.getMessageKey(), notification.getPlaceId(), notification.getPersonId(), recipientEmail);
         return null;
      }

      // Squirrel away the email address for audit logs
      notification.setDeliveryEndpoint("email:" + recipientEmail);

      // Render the notification message
      Map<String, String> messageParts = messageRenderer.renderMultipartMessage(notification, NotificationMethod.EMAIL, person, additionalEntityParams);

      MessageBuilder messageBuilder = Message.builder();
      if (messageParts.containsKey(SENDER_NAME_SECTION) && messageParts.containsKey(SENDER_EMAIL_SECTION)) {
         messageBuilder.from(EmailUtil.nameWithEmail(messageParts.get(SENDER_NAME_SECTION), messageParts.get(SENDER_EMAIL_SECTION)));
      } else {
         messageBuilder.from(messageParts.get(SENDER_EMAIL_SECTION));
      }
      if (messageParts.containsKey(REPLYTO_EMAIL_SECTION)) {
         messageBuilder.to(EmailUtil.nameWithEmail(getPersonDisplayName(recipient), messageParts.get(REPLYTO_EMAIL_SECTION)));
      }
      if (messageParts.containsKey(SUBJECT_SECTION)) {
         messageBuilder.subject(messageParts.get(SUBJECT_SECTION));
      }
      messageBuilder.text(messageParts.containsKey(PLAINTEXT_BODY_SECTION) ? messageParts.get(PLAINTEXT_BODY_SECTION) : messageParts.get(""));
      messageBuilder.html(messageParts.containsKey(HTML_BODY_SECTION) ? messageParts.get(HTML_BODY_SECTION) : messageParts.get(PLAINTEXT_BODY_SECTION));

      return messageBuilder;
   }

   private EmailRecipient getRecipient(Person p, Notification n) {
      if(p != null) {
         EmailRecipient recipient = new EmailRecipient();
         recipient.setEmail(p.getEmail());
         recipient.setFirstName(p.getFirstName());
         recipient.setLastName(p.getLastName());
         return recipient;
      }
      return n.getEmailRecipient();
   }

   private String getPersonDisplayName(EmailRecipient p) {
      if (p.getFirstName() == null || p.getFirstName().isEmpty() || p.getLastName() == null || p.getLastName().isEmpty() ) {
         return p.getEmail();
      } else {
         return p.getFirstName() + " " + p.getLastName();
      }
   }
}
