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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.notification.dispatch.DispatchException;
import com.iris.notification.message.NotificationBuilder;
import com.iris.notification.message.NotificationMessageRenderer;
import com.iris.notification.upstream.UpstreamNotificationResponder;
import com.iris.platform.notification.Notification;
import com.iris.platform.notification.NotificationMethod;
import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Pins the HTTP request that {@link MailgunEmailProvider} actually puts on the
 * wire.
 *
 * {@link MailgunEmailProviderTest} mocks {@code MailgunMessagesApi} outright,
 * so it only proves which builder setters were called; everything downstream of
 * that -- the feign form encoder, the request path, the basic-auth header and
 * the response decoder -- is skipped. Those are exactly the parts a mailgun-java
 * upgrade can change without breaking compilation, so this test drives the real
 * client against a loopback stub and asserts the bytes it sends.
 *
 * No mail is sent: the client is pointed at an ephemeral local port and every
 * address here is a fake under the reserved .test / example.com domains.
 */
public class MailgunEmailProviderWireTest {

   private static final String DOMAIN = "mail.example.test";
   private static final String API_KEY = "fake-api-key";
   private static final String MESSAGE_ID = "<20190101000000.1.ABCDEF@mail.example.test>";

   private static final String RECIPIENT_EMAIL = "recipient@example.com";
   private static final String SENDER_NAME = "Arcus Platform";
   private static final String SENDER_EMAIL = "noreply@example.test";
   private static final String REPLYTO_EMAIL = "support@example.test";
   private static final String SUBJECT = "Your alarm was triggered";
   private static final String PLAINTEXT_BODY = "plaintext alarm body";
   private static final String HTML_BODY = "<p>html alarm body</p>";

   private HttpServer server;

   private final AtomicReference<String> requestMethod = new AtomicReference<>();
   private final AtomicReference<String> requestPath = new AtomicReference<>();
   private final AtomicReference<String> requestAuthorization = new AtomicReference<>();
   private final AtomicReference<String> requestContentType = new AtomicReference<>();
   private final AtomicReference<String> requestBody = new AtomicReference<>();
   private final AtomicReference<String> responseId = new AtomicReference<>(MESSAGE_ID);

   private UpstreamNotificationResponder responder;
   private Notification notification;
   private MailgunEmailProvider provider;

   @Before
   public void setUp() throws Exception {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", this::handle);
      server.start();

      // A real feign client, with the only change being that it talks to the
      // stub above instead of api.mailgun.net.
      MailgunMessagesApi api = MailgunClient
            .config("http://127.0.0.1:" + server.getAddress().getPort() + "/", API_KEY)
            .createApi(MailgunMessagesApi.class);

      UUID placeId = UUID.randomUUID();
      UUID personId = UUID.randomUUID();

      Person person = Mockito.mock(Person.class);
      Mockito.when(person.getEmail()).thenReturn(RECIPIENT_EMAIL);

      Place place = Mockito.mock(Place.class);
      PersonDAO personDao = Mockito.mock(PersonDAO.class);
      PlaceDAO placeDao = Mockito.mock(PlaceDAO.class);
      AccountDAO accountDao = Mockito.mock(AccountDAO.class);
      Mockito.when(personDao.findById(Mockito.any())).thenReturn(person);
      Mockito.when(placeDao.findById(placeId)).thenReturn(place);

      Map<String, String> renderedParts = new HashMap<>();
      renderedParts.put("sender-name", SENDER_NAME);
      renderedParts.put("sender-email", SENDER_EMAIL);
      renderedParts.put("replyto-email", REPLYTO_EMAIL);
      renderedParts.put("subject", SUBJECT);
      renderedParts.put("plaintext-body", PLAINTEXT_BODY);
      renderedParts.put("html-body", HTML_BODY);

      NotificationMessageRenderer messageRenderer = Mockito.mock(NotificationMessageRenderer.class);
      Mockito.when(messageRenderer.renderMultipartMessage(
            Mockito.any(Notification.class),
            Mockito.eq(NotificationMethod.EMAIL),
            Mockito.any(Person.class),
            Mockito.any())).thenReturn(renderedParts);

      responder = Mockito.mock(UpstreamNotificationResponder.class);
      notification = new NotificationBuilder().withPersonId(personId).withPlaceId(placeId).build();

      provider = new MailgunEmailProvider(api, DOMAIN, personDao, placeDao, accountDao, messageRenderer, responder);
   }

   @After
   public void tearDown() {
      if (server != null) {
         server.stop(0);
      }
   }

   @Test
   public void shouldPostMultipartMessageToDomainEndpoint() throws Exception {
      provider.notifyCustomer(notification);

      assertEquals("POST", requestMethod.get());
      // The /v3 prefix comes from the ApiVersion on MailgunMessagesApi, not
      // from the base url, so it is worth pinning separately.
      assertEquals("/v3/" + DOMAIN + "/messages", requestPath.get());

      String expectedAuth = "Basic " + Base64.getEncoder()
            .encodeToString(("api:" + API_KEY).getBytes(StandardCharsets.UTF_8));
      assertEquals(expectedAuth, requestAuthorization.get());

      assertNotNull(requestContentType.get());
      assertTrue(
            "expected a multipart/form-data body, got: " + requestContentType.get(),
            requestContentType.get().startsWith("multipart/form-data"));

      String body = requestBody.get();
      assertFormField(body, "from", SENDER_NAME + " <" + SENDER_EMAIL + ">");
      assertFormField(body, "to", RECIPIENT_EMAIL);
      assertFormField(body, "h:Reply-To", REPLYTO_EMAIL);
      assertFormField(body, "subject", SUBJECT);
      assertFormField(body, "text", PLAINTEXT_BODY);
      assertFormField(body, "html", HTML_BODY);

      // The provider only hands off once it has seen an id come back.
      Mockito.verify(responder).handleHandOff(notification);
   }

   /**
    * Mailgun answers 200 with a JSON body, so an id is the only thing that
    * distinguishes a queued message from a silently dropped one. Decoding that
    * body is the client's job, which makes it worth pinning across an upgrade.
    */
   @Test(expected = DispatchException.class)
   public void shouldFailDispatchWhenMailgunReturnsNoId() throws Exception {
      responseId.set("");
      provider.notifyCustomer(notification);
   }

   private void handle(HttpExchange exchange) throws IOException {
      requestMethod.set(exchange.getRequestMethod());
      requestPath.set(exchange.getRequestURI().getPath());
      requestAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));

      try (InputStream in = exchange.getRequestBody()) {
         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         byte[] chunk = new byte[4096];
         int read;
         while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
         }
         requestBody.set(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
      }

      byte[] response = ("{\"id\":\"" + responseId.get() + "\",\"message\":\"Queued. Thank you.\"}")
            .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, response.length);
      try (OutputStream out = exchange.getResponseBody()) {
         out.write(response);
      }
   }

   /**
    * Multipart bodies carry each value after its own
    * {@code Content-Disposition} header, so assert on that pairing rather than
    * on a bare substring that any other field could satisfy.
    */
   private static void assertFormField(String body, String name, String expectedValue) {
      String marker = "name=\"" + name + "\"";
      int at = body.indexOf(marker);
      assertTrue("multipart body has no field named " + name + ": " + body, at >= 0);

      int valueStart = body.indexOf("\r\n\r\n", at);
      assertTrue("field " + name + " has no value section: " + body, valueStart >= 0);
      int valueEnd = body.indexOf("\r\n--", valueStart + 4);
      assertTrue("field " + name + " is not terminated: " + body, valueEnd >= 0);

      assertEquals(expectedValue, body.substring(valueStart + 4, valueEnd));
   }
}
