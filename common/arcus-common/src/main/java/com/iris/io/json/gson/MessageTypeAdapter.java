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
package com.iris.io.json.gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.iris.io.Serializer;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

// {
//    "source":"SERV:LWG-5814:hub",
//    "destination":"",
//    "transactionId":null,
//    "placeId":"90035467-da91-43fa-bc97-32f5384cf41e",
//    "actor":null,
//    "clientTime":1479834564228,
//    "type":"base:ValueChange",
//    "payload":{
//       "messageType":"base:ValueChange",
//       "attributes":{"hubsounds:source":""}
//    },
//    "timestamp":1479834564259,
//    "ttl":-1,
//    "isRequest":false
// }

public class MessageTypeAdapter extends TypeAdapter<PlatformMessage> {
   private static final String ATTR_SOURCE = "source";
   private static final String ATTR_DESTINATION = "destination";
   private static final String ATTR_CORRELATION_ID = "transactionId";
   private static final String ATTR_PLACEID = "placeId";
   private static final String ATTR_POPULATION = "population";
   private static final String ATTR_ACTOR = "actor";
   private static final String ATTR_PAYLOAD_TYPE = "type";
   private static final String ATTR_ISREQUEST = "isRequest";
   private static final String ATTR_PAYLOAD = "payload";
   private static final String ATTR_CLIENTTIME = "clientTime";
   private static final String ATTR_TIMESTAMP = "timestamp";
   private static final String ATTR_TTL = "ttl";

   private static final byte[] EMPTY_BUFFER = new byte[0];
   private final Gson gson;
   private final long optimizeExpiredMessagesDefaultTtl;

   public MessageTypeAdapter(Gson gson, int optimizeExpiredMessagesDefaultTtl) {
      this.gson = gson;
      this.optimizeExpiredMessagesDefaultTtl = optimizeExpiredMessagesDefaultTtl;
   }

   @Override
   public void write(JsonWriter out, PlatformMessage value) throws IOException {
      Address src = value.getSource();
      Address dst = value.getDestination();
      Address act = value.getActor();
      String plc = value.getPlaceId();
      String pop = value.getPopulation();
      String corr = value.getCorrelationId();
      boolean req = value.isRequest();
      Date ts = value.getTimestamp();
      Date ct = value.getClientTime();
      String pt = value.getMessageType();

      out.beginObject();

      //////////////////////////////////////////////////////////////////////////
      // NOTE: TTL and Timestamp are placed first in the serialized message
      //       as an optimization for expired messages when deserializing.
      //////////////////////////////////////////////////////////////////////////
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

      if (pt != null) {
         out.name(ATTR_PAYLOAD_TYPE).value(pt);
      } else {
         out.name(ATTR_PAYLOAD_TYPE).nullValue();
      }

      if (act != null) {
         out.name(ATTR_ACTOR).value(act.getRepresentation());
      } else {
         out.name(ATTR_ACTOR).nullValue();
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

      if (corr != null) {
         out.name(ATTR_CORRELATION_ID).value(corr);
      } else {
         out.name(ATTR_CORRELATION_ID).nullValue();
      }

      if (ct != null) {
         out.name(ATTR_CLIENTTIME).value(ct.getTime());
      } else {
         out.name(ATTR_CLIENTTIME).nullValue();
      }

      out.name(ATTR_ISREQUEST).value(req);

      MessageBody plo = value.getValue();
      if (plo != null) {
         out.name(ATTR_PAYLOAD);
         gson.toJson(plo, MessageBody.class, out);
      } else {
         byte[] pl = value.getBuffer();
         if (pl != null) {
            out.name(ATTR_PAYLOAD).value(Base64.encodeBase64String(pl));
         } else {
            out.name(ATTR_PAYLOAD).nullValue();
         }
      }

      out.endObject();
   }

   @Override
   public PlatformMessage read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
         in.nextNull();
         return null;
      }

      PlatformMessage.Builder bld = PlatformMessage.builder();

      String type = null;
      MessageBody body = null;
      byte[] buffer = EMPTY_BUFFER;

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
         case ATTR_PAYLOAD_TYPE:
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
         case ATTR_ACTOR:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withActor(Address.fromString(in.nextString()));
            } else {
               in.nextNull();
            }
            break;
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
         case ATTR_CORRELATION_ID:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.withCorrelationId(in.nextString());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_ISREQUEST:
            if (isExpired) {
               in.skipValue();
            } else if (in.peek() != JsonToken.NULL) {
               bld.isRequestMessage(in.nextBoolean());
            } else {
               in.nextNull();
            }
            break;
         case ATTR_PAYLOAD:
            if (isExpired) {
        	      in.skipValue();
            } else {
               switch (in.peek()) {
               case NULL:
                  buffer = EMPTY_BUFFER;
                  break;
               case STRING:
                  buffer = Base64.decodeBase64(in.nextString());
                  break;
               default:
                  body = gson.fromJson(in, MessageBody.class);
                  break;
               }
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
         default:
            // ignore extra fields
        	   in.skipValue();
            break;
         }
      }
      in.endObject();

      if (isExpired) {
         return bld.createExpired(type);
      } else if (body != null) {
         bld.withPayload(body);
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

