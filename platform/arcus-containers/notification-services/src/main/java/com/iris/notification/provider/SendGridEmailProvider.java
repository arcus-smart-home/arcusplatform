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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class SendGridEmailProvider implements EmailProvider {

    private static Logger logger = LoggerFactory.getLogger(SendGridEmailProvider.class);

    private static final String REQUEST_END_POINT = "mail/send";
    private final static String SENDER_NAME_SECTION = "sender-name";
    private final static String SENDER_EMAIL_SECTION = "sender-email";
    private final static String REPLYTO_EMAIL_SECTION = "replyto-email";
    private final static String SUBJECT_SECTION = "subject";
    private final static String PLAINTEXT_BODY_SECTION = "plaintext-body";
    private final static String HTML_BODY_SECTION = "html-body";
    private final static EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();


    @Inject @Named("email.sendername") 	private String defaultSenderName;
    @Inject @Named("email.senderemail") 	private String defaultSenderEmail;
    @Inject @Named("email.replyto") 	private String defaultReplyToEmail;
    @Inject @Named("email.subject") 	private String defaultSubject;

    @Inject(optional=true) @Named("email.timeout.ms")
    private int timeout = (int)TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);

    @Inject(optional = true) @Named("email.filter.domain") private String defaultEmailFilterDomain;

    private final SendGrid sendGrid;
    private final PersonDAO personDao;
    private final PlaceDAO placeDao;
    private final AccountDAO accountDao;
    private final NotificationMessageRenderer messageRenderer;
    private final UpstreamNotificationResponder responder;

    @Inject
    public SendGridEmailProvider(@Named("email.provider.apikey") String sendGridApiKey, PersonDAO personDao, PlaceDAO placeDao, AccountDAO accountDao, NotificationMessageRenderer messageRenderer, UpstreamNotificationResponder responder) {
        this.sendGrid = new SendGrid(sendGridApiKey);


        this.personDao = personDao;
        this.placeDao = placeDao;
        this.accountDao = accountDao;
        this.messageRenderer = messageRenderer;
        this.responder = responder;
    }

    public class MailParams {
        private String recipientName;
        private String emailFilterDomain = defaultEmailFilterDomain;
        private String senderName = defaultSenderName;
        private String senderEmail = defaultSenderEmail;
        private String replyToEmail = defaultReplyToEmail;
        private String subject = defaultSubject;
        private String plaintextBody;
        private String htmlBody;
        private Email toEmail;
        private Email fromEmail;

        public MailParams() {
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getReplyToEmail() {
            return replyToEmail;
        }

        public void setReplyToEmail(String replyToEmail) {
            this.replyToEmail = replyToEmail;
        }

        public String getSenderEmail() {
            return senderEmail;
        }

        public void setSenderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPlaintextBody() {
            return plaintextBody;
        }

        public void setPlaintextBody(String plaintextBody) {
            this.plaintextBody = plaintextBody;
        }

        public String getHtmlBody() {
            return htmlBody;
        }

        public void setHtmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
        }

        public Email getToEmail() {
            return toEmail;
        }

        public void setToEmail(Email toEmail) {
            this.toEmail = toEmail;
        }

        public Email getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(Email fromEmail) {
            this.fromEmail = fromEmail;
        }

        public String getRecipientName() {
            return recipientName;
        }

        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

        public String getEmailFilterDomain() { return emailFilterDomain; }

        public void setEmailFilterDomain(String emailFilterDomain) { this.emailFilterDomain = emailFilterDomain; }
    }

    private MailParams fromMap(Map<String, String> messageParts) {
        MailParams mailParams = new MailParams();

        if (messageParts.containsKey(SENDER_NAME_SECTION)) {
            mailParams.setSenderName(messageParts.get(SENDER_NAME_SECTION));
        }

        if (messageParts.containsKey(SENDER_EMAIL_SECTION)) {
            mailParams.setSenderEmail(messageParts.get(SENDER_EMAIL_SECTION));
        }

        if (messageParts.containsKey(REPLYTO_EMAIL_SECTION)) {
            mailParams.setReplyToEmail(messageParts.get(REPLYTO_EMAIL_SECTION));
        }

        if (messageParts.containsKey(SUBJECT_SECTION)) {
            mailParams.setSubject(messageParts.get(SUBJECT_SECTION));
        }

        mailParams.setPlaintextBody(messageParts.containsKey(PLAINTEXT_BODY_SECTION) ? messageParts.get(PLAINTEXT_BODY_SECTION) : messageParts.get(""));
        mailParams.setHtmlBody(messageParts.containsKey(HTML_BODY_SECTION) ? messageParts.get(HTML_BODY_SECTION) : mailParams.getPlaintextBody());

        return mailParams;
    }

    @Override
    public void notifyCustomer(Notification notification) throws DispatchUnsupportedByUserException, DispatchException {

        //wds - handle email address changes. see: https://eyeris.atlassian.net/browse/ITWO-11070
        notifyCustomerOldEmail(notification);

        MailParams mailParams = getMailParams(notification);
        if (mailParams == null) return;

        Email toEmail = mailParams.getToEmail();
        if (!isEmailValid(toEmail)) {
            logger.warn("Notification [{}] for placeId [{}] for person [{}] had invalid toEmail [{}].", notification.getMessageKey(), notification.getPlaceId(), notification.getPersonId(), toEmail == null ? "toEmail is null" : toEmail.getEmail());
            return;
        }

        sendEmail(mailParams);
        responder.handleHandOff(notification);
    }

    public Boolean isEmailValid(Email toEmail) {
        if (toEmail == null) return false;
        return isEmailValid(toEmail.getEmail());
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

        MailParams mailParams = getMailParams(notification);
        if (mailParams == null) return;

        mailParams.setToEmail(new Email(oldEmail, mailParams.getRecipientName()));
        sendEmail(mailParams);
    }

    private MailParams getMailParams(Notification notification) throws DispatchUnsupportedByUserException {
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
        MailParams mailParams = fromMap(messageParts);

        mailParams.setRecipientName(getPersonDisplayName(recipient));
        mailParams.setToEmail(new Email(recipientEmail, mailParams.getRecipientName()));
        mailParams.setFromEmail(new Email(mailParams.getSenderEmail(), mailParams.getSenderName()));

        return mailParams;
    }

    public void sendEmail(MailParams mailParams) throws DispatchException {

        // Filter the load testing domain so we don't flood email provider.
        String filterDomain = mailParams.getEmailFilterDomain();
        if (StringUtils.isNotBlank(filterDomain)) {
            String email = mailParams.getToEmail().getEmail();
            if (StringUtils.isNotBlank(email) && email.endsWith(filterDomain)) {
                logger.debug("Dropping email to address {} matches domain {}", mailParams.getToEmail().getEmail(), filterDomain);
                return;
            }
        }

        // Send the email; throw DispatchException if we're unable to
        Content content = new Content("text/plain", mailParams.getPlaintextBody());

        Mail mail = new Mail(mailParams.getFromEmail(), mailParams.getSubject(), mailParams.getToEmail(), content);
        Content htmlContent = new Content("text/html", mailParams.getHtmlBody());
        mail.addContent(htmlContent);
        mail.setReplyTo(new Email(mailParams.getReplyToEmail()));

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint(REQUEST_END_POINT);
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            //TODO - status codes?
            if(response == null) {
                logger.error("Null response attempting to send mail");
                throw new DispatchException("Failed to send notification email. Reason: null response");
            }
            else if (response.getStatusCode() > 202) {
                logger.error("Invalid response status [{}] attempting to send mail with response body [{}]", response.getStatusCode(), response.getBody());
                throw new DispatchException("Failed to send notification email. Reason: " + response.getStatusCode());
            }

        } catch (IOException ex) {
            throw new DispatchException(ex);
        }
    }

    protected String getPersonDisplayName(EmailRecipient p) {
        if (p.getFirstName() == null || p.getFirstName().isEmpty() || p.getLastName() == null || p.getLastName().isEmpty() ) {
            return p.getEmail();
        } else {
            return p.getFirstName() + " " + p.getLastName();
        }
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
}

