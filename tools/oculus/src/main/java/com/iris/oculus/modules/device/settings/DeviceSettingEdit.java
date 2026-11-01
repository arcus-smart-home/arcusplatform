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
package com.iris.oculus.modules.device.settings;

import java.util.Objects;

/**
 * The editing state of one device setting: the latest attribute value, what the
 * user has changed and how far the last request got.
 *
 * The driver stores a value as soon as it sends it and reports the attribute
 * again when the device answers, so the attribute can match the request before
 * the device has applied it and can change again afterwards. requested is kept
 * after a match so that later report still shows up as a mismatch. An attribute
 * change can also arrive before the SetParam response, or be for another param
 * entirely, because the attribute holds the whole list.
 */
public class DeviceSettingEdit {
   public enum Status { NONE, OK, PENDING, WARNING, ERROR }

   private static final String RESPONSE_OK = "OK";
   private static final String RESPONSE_PENDING = "PENDING";
   private static final String RESPONSE_INVALID_PARAM = "INVALID_PARAM";
   private static final String RESPONSE_INVALID_VALUE = "INVALID_VALUE";

   private DeviceSettingParam param;
   /** A valid value the user entered that differs from what the row shows as saved. */
   private String draft;
   /** Why the text in the editor can't be sent. */
   private String invalid;
   /** The value of the SetParam in flight. */
   private String sending;
   /** The last value the driver accepted. */
   private String requested;
   private boolean awaitingAttribute;
   private boolean reading;
   private boolean awaitingRead;
   private Status status = Status.NONE;
   private String statusText = "";

   public DeviceSettingEdit(DeviceSettingParam param) {
      this.param = param;
   }

   public DeviceSettingParam getParam() {
      return param;
   }

   /** True if the editor holds input of the user's that must not be overwritten. */
   public boolean hasUnsavedEdit() {
      return draft != null || invalid != null;
   }

   /** The value the editor should show when there is no unsaved edit. */
   public String getEditorValue() {
      if(sending != null) {
         return sending;
      }
      return savedValue();
   }

   private String savedValue() {
      return awaitingAttribute ? requested : param.getValue();
   }

   public Status getStatus() {
      return invalid != null ? Status.ERROR : status;
   }

   public String getStatusText() {
      return invalid != null ? invalid : statusText;
   }

   public boolean canEdit() {
      return sending == null;
   }

   public boolean canSave() {
      return draft != null && sending == null;
   }

   public boolean canReset() {
      return sending == null && param.validate(param.getDefault()) == null;
   }

   public boolean canRead() {
      return sending == null && !reading;
   }

   public void onEdit(String value) {
      invalid = null;
      draft = value.equals(savedValue()) ? null : value;
   }

   public void onInvalidEdit(String message) {
      draft = null;
      invalid = message;
   }

   /** @return the value to send with SetParam */
   public String beginSave() {
      return beginSet(draft);
   }

   /** @return the value to send with SetParam */
   public String beginReset() {
      draft = null;
      invalid = null;
      return beginSet(param.getDefault());
   }

   private String beginSet(String value) {
      sending = value;
      requested = null;
      awaitingAttribute = false;
      awaitingRead = false;
      setStatus(Status.NONE, "Sending...");
      return value;
   }

   public void onSetResponse(String response) {
      String value = sending;
      sending = null;
      if(RESPONSE_OK.equals(response) || RESPONSE_PENDING.equals(response)) {
         draft = null;
         requested = value;
         awaitingAttribute = !value.equals(param.getValue());
         if(RESPONSE_OK.equals(response)) {
            setStatus(Status.OK, RESPONSE_OK);
         }
         else if(awaitingAttribute) {
            setStatus(Status.PENDING, RESPONSE_PENDING + ", waiting for the attribute to change");
         }
         else {
            setApplied();
         }
      }
      else if(RESPONSE_INVALID_VALUE.equals(response)) {
         setStatus(Status.ERROR, response + ": the driver rejected " + param.describe(value));
      }
      else if(RESPONSE_INVALID_PARAM.equals(response)) {
         setStatus(Status.ERROR, response + ": the driver does not have this setting");
      }
      else {
         setStatus(Status.ERROR, "Unexpected status: " + response);
      }
   }

   public void onSetFailed(String message) {
      sending = null;
      setStatus(Status.ERROR, "Failed: " + message);
   }

   public void beginRead() {
      reading = true;
      awaitingRead = false;
      setStatus(Status.NONE, "Reading...");
   }

   /**
    * A device that reports the value it already had changes nothing, so a
    * PENDING read is not guaranteed an attribute change to end it.
    */
   public void onReadResponse(String response, String value) {
      reading = false;
      String known = DeviceSettingParam.toValue(value);
      if(RESPONSE_OK.equals(response)) {
         setStatus(Status.OK, RESPONSE_OK + ": " + param.describe(known));
      }
      else if(RESPONSE_PENDING.equals(response)) {
         awaitingRead = true;
         setStatus(Status.PENDING, known == null ? response : response + ", last known " + param.describe(known));
      }
      else if(RESPONSE_INVALID_PARAM.equals(response)) {
         setStatus(Status.ERROR, response + ": the driver does not have this setting");
      }
      else {
         setStatus(Status.ERROR, "Unexpected status: " + response);
      }
   }

   public void onReadFailed(String message) {
      reading = false;
      setStatus(Status.ERROR, "Failed: " + message);
   }

   /** Applies a new attribute value, whichever param caused the attribute to change. */
   public void onParamChanged(DeviceSettingParam updated) {
      String value = updated.getValue();
      boolean changed = !Objects.equals(param.getValue(), value);
      param = updated;

      if(value != null && value.equals(draft)) {
         draft = null;
      }

      if(requested != null && requested.equals(value)) {
         awaitingAttribute = false;
         if(status == Status.PENDING) {
            setApplied();
         }
      }
      else if(requested != null && changed) {
         setStatus(Status.WARNING, "Attribute changed to " + param.describe(value) + " after requesting " + param.describe(requested));
         requested = null;
         awaitingAttribute = false;
      }
      else if(awaitingRead && changed) {
         setStatus(Status.OK, "Attribute changed to " + param.describe(value));
      }

      if(changed) {
         awaitingRead = false;
      }
   }

   private void setApplied() {
      setStatus(Status.OK, "Attribute shows " + param.describe(requested));
   }

   private void setStatus(Status status, String text) {
      this.status = status;
      this.statusText = text;
   }
}
