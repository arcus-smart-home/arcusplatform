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
package com.iris.protocol.gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.io.Serializer;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.protocol.ProtocolMessage;

// {
//    "source":"PROT:ZIGB-LWF-0208:jRggu3Zh/UIAAAAAAAAAAAAAAAA\u003d",
//    "destination":"",
//    "correlationId":null,
//    "isPlatform":false,
//    "isRequest":false,
//    "isError":false,
//    "clientTime":1483367089787,
//    "timestamp":1483367089785,
//    "timeToLive":-1,
//    "placeId":"df38f8bf-61bf-45b1-aef6-efac172efcd1",
//    "actor":null,
//    "typeName":"ZIGB",
//    "buffer":"AgsAAAAAAyAAAAABAAAAAA\u003d\u003d"
// }

// {
//    "source":"DRIV:dev:f7ab0cf9-b410-4cda-bb06-641a6772ee14",
//    "destination":"PROT:ZIGB-LWQ-9248:VmstHWoptCMAAAAAAAAAAAAAAAA\u003d",
//    "correlationId":null,
//    "isPlatform":false,
//    "isRequest":false,
//    "isError":false,
//    "clientTime":null,
//    "timestamp":1483287059298,
//    "timeToLive":-1,
//    "placeId":"8247f7ce-e88c-4155-aab6-7ece36fb8dd1",
//    "actor":null,
//    "typeName":"ZIGB",
//    "buffer":"AgsAAAD9ARbC8AACAAAAAA\u003d\u003d"
// }

public class ProtocolMessageTypeAdapter extends TypeAdapter<ProtocolMessage> {
   private static final String ATTR_SOURCE = "source";
   private static final String ATTR_DESTINATION = "destination";
   private static final String ATTR_PLACEID = "placeId";
   private static final String ATTR_POPULATION = "population";
   private static final String ATTR_TYPENAME = "typeName";
   private static final String ATTR_BUFFER = "buffer";
   private static final String ATTR_CLIENTTIME = "clientTime";
   private static final String ATTR_TIMESTAMP = "timestamp";
   private static final String ATTR_TTL = "timeToLive";
   private static final String ATTR_REFLEX_VERSION = "rflx";
   private static final String ATTR_ACTOR = "actor";

   private static final byte[] EMPTY_BUFFER = new byte[0];

   private final int optimizeExpiredMessagesDefaultTtl;

   public ProtocolMessageTypeAdapter(int optimizeExpiredMessagesDefaultTtl) {
      this.optimizeExpiredMessagesDefaultTtl = optimizeExpiredMessagesDefaultTtl;
   }

   @Override
   public void write(JsonWriter out, ProtocolMessage value) throws IOException {
      Address src = value.getSource();
      Address dst = value.getDestination();
      String plc = value.getPlaceId();
      String pop = value.getPopulation();
      Date ts = value.getTimestamp();
      Date ct = value.getClientTime();
      String tn = value.getMessageType();
      Integer rv = value.getReflexVersion();
      Address act = value.getActor();

      out.beginObject();
      out.name(ATTR_TTL).value(value.getTimeToLive());
      if (ts != null) {
         out.name(ATTR_TIMESTAMP).value(ts.getTime());
      } else {
         out.name(ATTR_TIMESTAMP).nullValue();
      }

      if (src != null) {
         out.name(ATTR_SOURCE).value(src.getRepresentation());
      } else {
         out.name(ATTR_SOURCE).nullValue();
      }

      if (dst != null) {
         out.name(ATTR_DESTINATION).value(dst.getRepresentation());
      } else {
         out.name(ATTR_DESTINATION).nullValue();
      }

      if (plc != null) {
         out.name(ATTR_PLACEID).value(plc);
      } else {
         out.name(ATTR_PLACEID).nullValue();
      }
      
      if (pop != null) {
         out.name(ATTR_POPULATION).value(pop);
      } else {
         out.name(ATTR_POPULATION).nullValue();
      }

      if (ct != null) {
         out.name(ATTR_CLIENTTIME).value(ct.getTime());
      } else {
         out.name(ATTR_CLIENTTIME).nullValue();
      }

      if (tn != null) {
         out.name(ATTR_TYPENAME).value(tn);
      } else {
         out.name(ATTR_TYPENAME).nullValue();
      }

      if (rv != null) {
         out.name(ATTR_REFLEX_VERSION).value(rv);
      } else {
         out.name(ATTR_REFLEX_VERSION).nullValue();
      }

      if (act != null) {
         out.name(ATTR_ACTOR).value(act.getRepresentation());
      }

      String encoded = value.getEncodedPayloadIfExists();
      if (encoded != null) {
         out.name(ATTR_BUFFER).value(Base64.encodeBase64String(value.getBuffer()));
      } else {
         byte[] bf = value.getBuffer();
         if (bf != null && bf.length > 0) {
            out.name(ATTR_BUFFER).value(Base64.encodeBase64String(value.getBuffer()));
         } else {
            out.name(ATTR_BUFFER).nullValue();
         }
      }

      out.endObject();
   }

   @Override
   public ProtocolMessage read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      }

      ProtocolMessage.Builder bld = ProtocolMessage.builder();

      String type = null;
      String encoded = null;
      byte[] buffer = null;

      boolean isExpired = false;
      long now = System.currentTimeMillis();
      Integer ttl = null;
      Long ts = null;

      in.beginObject();
      while (in.hasNext()) {
         switch (in.nextName()) {
         ///////////////////////////////////////////////////////////////////////
         // Message Components Required Even For Expired Messages
         ///////////////////////////////////////////////////////////////////////
         case ATTR_SOURCE:
            if (in.peek() != JsonToken.NULL) {
               bld.from(Address.fromString(in.nextString()));
            } else {
               in.nextNull();
            }
            break;
         case ATTR_DESTINATION:
            if (in.peek() != JsonToken.NULL) {
               bld.to(Address.fromString(in.nextString()));
            } else {
               in.nextNull();
            }
            break;
         case ATTR_TYPENAME:
            if (in.peek() != JsonToken.NULL) {
               type = in.nextString();
            } else {
               in.nextNull();
            }
            break;
         case ATTR_TIMESTAMP:
            if (in.peek() != JsonToken.NULL) {
               ts = in.nextLong();
            } else {
               in.nextNull();
               ts = now;
            }

            bld.withTimestamp(ts);
            isExpired = optimizeExpiredMessagesDefaultTtl != 0 && ttl != null && ts != null && Message.isExpired(optimizeExpiredMessagesDefaultTtl, now, ts, ttl);
            break;
         case ATTR_TTL:
            if (in.peek() != JsonToken.NULL) {
               ttl = in.nextInt();
            } else {
               in.nextNull();
               ttl = -1;
            }

            bld.withTimeToLive(ttl);
            isExpired = optimizeExpiredMessagesDefaultTtl != 0 && ttl != null && ts != null && Message.isExpired(optimizeExpiredMessagesDefaultTtl, now, ts, ttl);
            break;

         ///////////////////////////////////////////////////////////////////////
         // Message Components That Can be Ignored for Expired Messages
         ///////////////////////////////////////////////////////////////////////
         case ATTR_PLACEID:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withPlaceId(in.nextString());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_POPULATION:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withPopulation(in.nextString());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_BUFFER:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               encoded = in.nextString();
            } else {
               buffer = EMPTY_BUFFER;
               in.nextNull();
            }
            break;
         case ATTR_CLIENTTIME:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withClientTime(in.nextLong());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_REFLEX_VERSION:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withReflexVersion(in.nextInt());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_ACTOR:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withActor(Address.fromString(in.nextString()));
            } else {
               in.nextNull();
            }
            break;
         default:
            // ignore extra fields
            in.skipValue();
            break;
         }
      }
      in.endObject();

      if (isExpired) {
         return bld.createExpired(type);
      } else if (encoded != null) {
         bld.withBase64EncodedPayload(type, encoded);
         return bld.create();
      } else {
         bld.withPayload(type, ByteArraySerializer.INSTANCE, buffer);
         return bld.create();
      }
   }

   private static enum ByteArraySerializer implements Serializer<byte[]> {
      INSTANCE;

      @Override
      public byte[] serialize(byte[] value) throws IllegalArgumentException {
         return value;
      }

      @Override
      public void serialize(byte[] value, OutputStream out) throws IOException, IllegalArgumentException {
         throw new UnsupportedOperationException();
      }
   }
}

