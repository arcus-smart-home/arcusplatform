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

package com.iris.driver.pin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.messages.errors.ErrorEventException;

public abstract class AbstractPinManager implements PinManager {

   // TODO: should these be configurable
   public static final long TIMEOUT_MILLIS = 60 * 1000;
   private static final int PIN_LENGTH = 4;

   private final List<Byte> accumulator = new ArrayList<Byte>(PIN_LENGTH);
   private final byte CHAR_0 = (byte)'0';
   private final byte CHAR_9 = (byte)'9';
   private final long timeout;
   private long lastEntry = -1;
   private UUID actor;

   protected AbstractPinManager() {
      this(TIMEOUT_MILLIS);
   }

   protected AbstractPinManager(long timeout) {
      this.timeout = timeout;
   }

   @Override
   public byte[] getPin(UUID placeId, UUID personId) {
      String pin = doGetPin(placeId, personId);
      return pinToByteArray(pin);
   }

   protected abstract String doGetPin(UUID placeId, UUID personId);

   @Override
   public UUID validatePin(UUID placeId, String pin) {
      UUID person = doValidatePin(placeId, pin);
      if(person == null) {
         throw new ErrorEventException("InvalidPin", "Invalid Pin");
      }
      return person;
   }

   @Override
   public UUID validatePin(UUID placeId, byte[] pin) {
      return validatePin(placeId, pinToString(pin));
   }

   protected abstract UUID doValidatePin(UUID placeId, String pin);

   @Override
   public void setActor(UUID actor) {
      this.actor = actor;
   }

   @Override
   public UUID getActor() {
      return actor;
   }

   @Override
   public UUID accumulatePin(UUID placeId, int code) {
      if(code < CHAR_0 || code > CHAR_9) {
         throw new ErrorEventException("InvalidPin", "Invalid Pin");
      }

      if(lastEntry < 0 || System.currentTimeMillis() - lastEntry >= timeout) {
         accumulator.clear();
      }

      lastEntry = System.currentTimeMillis();
      accumulator.add((byte) code);
      if(accumulator.size() == PIN_LENGTH) {
         return validateAndClearAccumulator(placeId);
      }

      return null;
   }

   private UUID validateAndClearAccumulator(UUID placeId) {
      byte[] pin = accumulatorAsBytes();
      accumulator.clear();
      return validatePin(placeId, pin);
   }

   private byte[] accumulatorAsBytes() {
      byte[] buf = new byte[accumulator.size()];
      for(int i = 0; i < accumulator.size(); i++) {
         buf[i] = accumulator.get(i).byteValue();
      }
      return buf;
   }

   private byte[] pinToByteArray(String pin) {
      if(StringUtils.isBlank(pin)) {
         return new byte[0];
      }
      byte[] buffer = new byte[pin.length()];
      for(int i = 0; i < pin.length(); i++) {
         buffer[i] = (byte) pin.charAt(i);
      }
      return buffer;
   }

   private String pinToString(byte[] pin) {
      return new String(pin);
   }
}

