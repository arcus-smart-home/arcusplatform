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
package com.iris.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.Message;
import com.iris.notification.provider.EmailProvider;
import com.iris.notification.provider.MailgunEmailProvider;

public class NotificationModuleTest {

   @Test
   public void unsetSendsEmailThroughMailgun() {
      List<Element> elements = configure(null);
      assertEquals(List.of(), errors(elements));
      assertEquals(Key.get(MailgunEmailProvider.class), emailProviderTarget(elements));
   }

   @Test
   public void mailgunSendsEmailThroughMailgun() {
      List<Element> elements = configure("mailgun");
      assertEquals(List.of(), errors(elements));
      assertEquals(Key.get(MailgunEmailProvider.class), emailProviderTarget(elements));
   }

   // "default" meant SendGrid, and "noop" is what arcus-k8s's checked-in overlays set
   @Test
   public void anyOtherProviderStopsStartup() {
      for (String sender : List.of("default", "sendgrid", "noop", "")) {
         List<String> errors = errors(configure(sender));
         assertEquals(sender, 1, errors.size());
         assertTrue(errors.get(0), errors.get(0).contains("'" + sender + "'"));
      }
   }

   // Injects the setting into the module the way Bootstrap does, then records what it configures.
   private static List<Element> configure(String emailSender) {
      NotificationModule module = new NotificationModule();
      Guice.createInjector(binder -> {
         if (emailSender != null) {
            binder.bindConstant().annotatedWith(Names.named("notificationservice.sender.email")).to(emailSender);
         }
      }).injectMembers(module);
      return Elements.getElements(module);
   }

   private static List<String> errors(List<Element> elements) {
      return elements.stream()
            .filter(Message.class::isInstance)
            .map(e -> ((Message) e).getMessage())
            .collect(Collectors.toList());
   }

   private static Key<?> emailProviderTarget(List<Element> elements) {
      return elements.stream()
            .filter(LinkedKeyBinding.class::isInstance)
            .map(e -> (LinkedKeyBinding<?>) e)
            .filter(b -> b.getKey().equals(Key.get(EmailProvider.class)))
            .map(LinkedKeyBinding::getLinkedKey)
            .findFirst()
            .orElse(null);
   }
}
