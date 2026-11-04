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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A minimal passkey authenticator for tests: one P-256 (ES256) credential, "none"
 * attestation, user presence and verification always asserted. Produces the JSON a
 * browser's PublicKeyCredential.toJSON() would.
 */
class SoftwareAuthenticator {
   private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();
   private static final Base64.Decoder B64URL_DECODE = Base64.getUrlDecoder();

   private static final int FLAG_UP = 0x01;
   private static final int FLAG_UV = 0x04;
   private static final int FLAG_AT = 0x40;

   private final String origin;
   private final KeyPair keyPair;
   private final byte[] credentialId = new byte[16];
   private byte[] userHandle;
   private int signCount = 0;

   SoftwareAuthenticator(String origin) throws Exception {
      this.origin = origin;
      KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
      generator.initialize(new ECGenParameterSpec("secp256r1"));
      this.keyPair = generator.generateKeyPair();
      new SecureRandom().nextBytes(credentialId);
   }

   String getCredentialId() {
      return B64URL.encodeToString(credentialId);
   }

   /** Responds to registration options ({"publicKey": {...}}) like navigator.credentials.create(). */
   String create(String optionsJson) throws Exception {
      JsonObject publicKey = JsonParser.parseString(optionsJson).getAsJsonObject().getAsJsonObject("publicKey");
      String rpId = publicKey.getAsJsonObject("rp").get("id").getAsString();
      userHandle = B64URL_DECODE.decode(publicKey.getAsJsonObject("user").get("id").getAsString());
      byte[] clientData = clientData("webauthn.create", publicKey.get("challenge").getAsString());

      ByteArrayOutputStream authData = new ByteArrayOutputStream();
      authData.write(sha256(rpId.getBytes(StandardCharsets.UTF_8)));
      authData.write(FLAG_UP | FLAG_UV | FLAG_AT);
      authData.write(ByteBuffer.allocate(4).putInt(signCount).array());
      authData.write(new byte[16]); // aaguid
      authData.write(ByteBuffer.allocate(2).putShort((short) credentialId.length).array());
      authData.write(credentialId);
      authData.write(coseKey());

      Cbor attestation = new Cbor().map(3)
            .text("fmt").text("none")
            .text("attStmt").map(0)
            .text("authData").bytes(authData.toByteArray());

      JsonObject response = new JsonObject();
      response.addProperty("clientDataJSON", B64URL.encodeToString(clientData));
      response.addProperty("attestationObject", B64URL.encodeToString(attestation.toByteArray()));
      return credential(response).toString();
   }

   /** Responds to login options like navigator.credentials.get(). */
   String get(String optionsJson) throws Exception {
      return get(optionsJson, origin, false);
   }

   String get(String optionsJson, String origin, boolean tamper) throws Exception {
      JsonObject publicKey = JsonParser.parseString(optionsJson).getAsJsonObject().getAsJsonObject("publicKey");
      String rpId = publicKey.get("rpId").getAsString();
      byte[] clientData = clientData("webauthn.get", publicKey.get("challenge").getAsString(), origin);

      ByteArrayOutputStream authData = new ByteArrayOutputStream();
      authData.write(sha256(rpId.getBytes(StandardCharsets.UTF_8)));
      authData.write(FLAG_UP | FLAG_UV);
      authData.write(ByteBuffer.allocate(4).putInt(++signCount).array());

      Signature signer = Signature.getInstance("SHA256withECDSA");
      signer.initSign(keyPair.getPrivate());
      signer.update(authData.toByteArray());
      signer.update(sha256(clientData));
      byte[] signature = signer.sign();
      if (tamper) {
         signature[signature.length - 1] ^= 0x01;
      }

      JsonObject response = new JsonObject();
      response.addProperty("clientDataJSON", B64URL.encodeToString(clientData));
      response.addProperty("authenticatorData", B64URL.encodeToString(authData.toByteArray()));
      response.addProperty("signature", B64URL.encodeToString(signature));
      response.addProperty("userHandle", B64URL.encodeToString(userHandle));
      return credential(response).toString();
   }

   private JsonObject credential(JsonObject response) {
      JsonObject credential = new JsonObject();
      credential.addProperty("type", "public-key");
      credential.addProperty("id", getCredentialId());
      credential.addProperty("rawId", getCredentialId());
      credential.add("response", response);
      credential.add("clientExtensionResults", new JsonObject());
      return credential;
   }

   private byte[] clientData(String type, String challenge) {
      return clientData(type, challenge, origin);
   }

   private static byte[] clientData(String type, String challenge, String origin) {
      JsonObject clientData = new JsonObject();
      clientData.addProperty("type", type);
      clientData.addProperty("challenge", challenge);
      clientData.addProperty("origin", origin);
      clientData.addProperty("crossOrigin", false);
      return clientData.toString().getBytes(StandardCharsets.UTF_8);
   }

   private byte[] coseKey() {
      ECPublicKey key = (ECPublicKey) keyPair.getPublic();
      return new Cbor().map(5)
            .integer(1).integer(2)    // kty: EC2
            .integer(3).integer(-7)   // alg: ES256
            .integer(-1).integer(1)   // crv: P-256
            .integer(-2).bytes(coordinate(key.getW().getAffineX()))
            .integer(-3).bytes(coordinate(key.getW().getAffineY()))
            .toByteArray();
   }

   private static byte[] coordinate(BigInteger value) {
      byte[] bytes = value.toByteArray();
      byte[] fixed = new byte[32];
      int length = Math.min(bytes.length, 32);
      System.arraycopy(bytes, bytes.length - length, fixed, 32 - length, length);
      return fixed;
   }

   private static byte[] sha256(byte[] data) throws Exception {
      return MessageDigest.getInstance("SHA-256").digest(data);
   }

   /** Just enough CBOR for attestation objects and COSE keys. */
   private static class Cbor {
      private final ByteArrayOutputStream out = new ByteArrayOutputStream();

      Cbor map(int size) {
         header(5, size);
         return this;
      }

      Cbor text(String value) {
         byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
         header(3, bytes.length);
         out.write(bytes, 0, bytes.length);
         return this;
      }

      Cbor bytes(byte[] value) {
         header(2, value.length);
         out.write(value, 0, value.length);
         return this;
      }

      Cbor integer(int value) {
         if (value >= 0) {
            header(0, value);
         }
         else {
            header(1, -1 - value);
         }
         return this;
      }

      private void header(int majorType, int value) {
         if (value < 24) {
            out.write((majorType << 5) | value);
         }
         else if (value < 256) {
            out.write((majorType << 5) | 24);
            out.write(value);
         }
         else {
            out.write((majorType << 5) | 25);
            out.write(value >> 8);
            out.write(value & 0xFF);
         }
      }

      byte[] toByteArray() {
         return out.toByteArray();
      }
   }
}
