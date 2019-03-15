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
package com.iris.common.subsystem.alarm;

import static com.iris.messages.capability.AlarmIncidentCapability.ALERT_CO;
import static com.iris.messages.capability.AlarmIncidentCapability.ALERT_PANIC;
import static com.iris.messages.capability.AlarmIncidentCapability.ALERT_SECURITY;
import static com.iris.messages.capability.AlarmIncidentCapability.ALERT_SMOKE;
import static com.iris.messages.capability.AlarmIncidentCapability.ALERT_WATER;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_CANCELLED;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_DISPATCHED;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_DISPATCHING;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_FAILED;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_NONE;
import static com.iris.messages.capability.AlarmIncidentCapability.MONITORINGSTATE_REFUSED;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.google.common.collect.ImmutableMap;

public class CancelAlarmMessageTable
{
   static final String[] messageTitles =
   {
      // These are in order of first appearance in the requirements table on the Wiki
      "Device May Be Sounding", // 0
      "Cancel Alarm",           // 1
      "Hub Lost Connection",    // 2
   };

   static final String[] messageTexts =
   {
      // These are in order of first appearance in the requirements table on the Wiki
      // 0
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Smoke is no longer detected.",
      // 1
      "For your safety, the Monitoring Station will call to assist you and will attempt to dispatch the Fire Department if unanswered. To cancel this, call the Monitoring Station at %s.",
      // 2
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Smoke is no longer detected.\nFor your safety, the Monitoring Station will call to assist you and will attempt to dispatch the Fire Department if unanswered. To cancel this, call the Monitoring Station at %s.",
      // 3
      "The Fire Department has been notified and may be on their way. To cancel this, call the Monitoring Station at %s.",
      // 4
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Smoke is no longer detected.\nThe Fire Department has been notified and may be on their way. To cancel this, call the Monitoring Station at  %s.",
      // 5
      "The Fire Department was unable to be dispatched. Call your local Fire Department if you still need assistance.",
      // 6
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Smoke is no longer detected.\nThe Fire Department was unable to be dispatched. Call your local Fire Department if you still need assistance.",
      // 7
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Carbon Monoxide is no longer detected.",
      // 8
      "The Monitoring Station will attempt to dispatch the Fire Department.",
      // 9
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Carbon Monoxide is no longer detected.\nThe Monitoring Station will attempt to dispatch the Fire Department.",
      // 10
      "The Fire Department has been notified and may be on their way.",
      // 11
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Carbon Monoxide is no longer detected.\nThe Fire Department has been notified and may be on their way.",
      // 12
      "The Iris Alarm has been silenced; however the triggered device(s) will continue to sound until Carbon Monoxide is no longer detected.\nThe Fire Department was unable to be dispatched. Call your local Fire Department if you still need assistance.",
      // 13
      "The Monitoring Station will attempt to dispatch the Police Department. To cancel this, call the Monitoring Station at %s.",
      // 14
      "The Police Department has been notified and may be on their way.",
      // 15
      "The Police Department was unable to be dispatched. Call your local Police Department if you still need assistance.",
      // 16
      "For your safety, the Monitoring Station will call to assist you and will attempt to dispatch the Police Department if unanswered. To cancel this, call the Monitoring Station at %s.",
      // 17
      "The Police Department has been notified and may be on their way. To cancel this, call the Monitoring Station at %s.",
      // 18
      "Iris cannot stop sounds in the home until you reconnect the hub.",
      // 19
      "Iris cannot stop sounds in the home until you reconnect the hub.\nFor your safety, the Monitoring Station will call to assist you and will attempt to dispatch the Fire Department if unanswered. To cancel this, call the Monitoring Station at %s.",
      // 20
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Fire Department has been notified and may be on their way. To cancel this, call the Monitoring Station at %s.",
      // 21
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Fire Department was unable to be dispatched. Call your local Fire Department if you still need assistance.",
      // 22
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Monitoring Station will attempt to dispatch the Fire Department.",
      // 23
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Fire Department has been notified and may be on their way.",
      // 24
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Monitoring Station will attempt to dispatch the Police Department. To cancel this, call the Monitoring Station at %s.",
      // 25
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Police Department has been notified and may be on their way.",
      // 26
      "Iris cannot stop sounds in the home until you reconnect the hub.\nThe Police Department was unable to be dispatched. Call your local Police Department if you still need assistance.",
   };

   private static final Object[][] messageData =
   {
      // hubOnlineOrMissing, alert, monitoringState, smokeCOClearing, messageTitle, messageText
      { true,  ALERT_SMOKE,    MONITORINGSTATE_NONE,        true,  messageTitles[0], messageTexts[0]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_CANCELLED,   true,  messageTitles[0], messageTexts[0]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_DISPATCHING, false, messageTitles[1], messageTexts[1]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_DISPATCHING, true,  messageTitles[0], messageTexts[2]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_DISPATCHED,  false, messageTitles[1], messageTexts[3]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_DISPATCHED,  true,  messageTitles[0], messageTexts[4]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_REFUSED,     false, messageTitles[1], messageTexts[5]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_REFUSED,     true,  messageTitles[0], messageTexts[6]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_FAILED,      false, messageTitles[1], messageTexts[5]  },
      { true,  ALERT_SMOKE,    MONITORINGSTATE_FAILED,      true,  messageTitles[0], messageTexts[6]  },

      { true,  ALERT_CO,       MONITORINGSTATE_NONE,        true,  messageTitles[0], messageTexts[7]  },
      { true,  ALERT_CO,       MONITORINGSTATE_CANCELLED,   true,  messageTitles[0], messageTexts[7]  },
      { true,  ALERT_CO,       MONITORINGSTATE_DISPATCHING, false, messageTitles[1], messageTexts[8]  },
      { true,  ALERT_CO,       MONITORINGSTATE_DISPATCHING, true,  messageTitles[0], messageTexts[9]  },
      { true,  ALERT_CO,       MONITORINGSTATE_DISPATCHED,  false, messageTitles[1], messageTexts[10] },
      { true,  ALERT_CO,       MONITORINGSTATE_DISPATCHED,  true,  messageTitles[0], messageTexts[11] },
      { true,  ALERT_CO,       MONITORINGSTATE_REFUSED,     false, messageTitles[1], messageTexts[5]  },
      { true,  ALERT_CO,       MONITORINGSTATE_REFUSED,     true,  messageTitles[0], messageTexts[12] },
      { true,  ALERT_CO,       MONITORINGSTATE_FAILED,      false, messageTitles[1], messageTexts[5]  },
      { true,  ALERT_CO,       MONITORINGSTATE_FAILED,      true,  messageTitles[0], messageTexts[12] },

      { true,  ALERT_PANIC,    MONITORINGSTATE_DISPATCHING, false, messageTitles[1], messageTexts[13] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_DISPATCHING, true,  messageTitles[1], messageTexts[13] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_DISPATCHED,  false, messageTitles[1], messageTexts[14] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_DISPATCHED,  true,  messageTitles[1], messageTexts[14] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_REFUSED,     false, messageTitles[1], messageTexts[15] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_REFUSED,     true,  messageTitles[1], messageTexts[15] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_FAILED,      false, messageTitles[1], messageTexts[15] },
      { true,  ALERT_PANIC,    MONITORINGSTATE_FAILED,      true,  messageTitles[1], messageTexts[15] },

      { true,  ALERT_SECURITY, MONITORINGSTATE_DISPATCHING, false, messageTitles[1], messageTexts[16] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_DISPATCHING, true,  messageTitles[1], messageTexts[16] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_DISPATCHED,  false, messageTitles[1], messageTexts[17] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_DISPATCHED,  true,  messageTitles[1], messageTexts[17] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_REFUSED,     false, messageTitles[1], messageTexts[15] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_REFUSED,     true,  messageTitles[1], messageTexts[15] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_FAILED,      false, messageTitles[1], messageTexts[15] },
      { true,  ALERT_SECURITY, MONITORINGSTATE_FAILED,      true,  messageTitles[1], messageTexts[15] },

      { false, ALERT_SMOKE,    MONITORINGSTATE_NONE,        false, messageTitles[2], messageTexts[18] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_NONE,        true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_CANCELLED,   false, messageTitles[2], messageTexts[18] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_CANCELLED,   true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_DISPATCHING, false, messageTitles[2], messageTexts[19] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_DISPATCHING, true,  messageTitles[2], messageTexts[19] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_DISPATCHED,  false, messageTitles[2], messageTexts[20] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_DISPATCHED,  true,  messageTitles[2], messageTexts[20] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_REFUSED,     false, messageTitles[2], messageTexts[21] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_REFUSED,     true,  messageTitles[2], messageTexts[21] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_FAILED,      false, messageTitles[2], messageTexts[21] },
      { false, ALERT_SMOKE,    MONITORINGSTATE_FAILED,      true,  messageTitles[2], messageTexts[21] },

      { false, ALERT_CO,       MONITORINGSTATE_NONE,        false, messageTitles[2], messageTexts[18] },
      { false, ALERT_CO,       MONITORINGSTATE_NONE,        true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_CO,       MONITORINGSTATE_CANCELLED,   false, messageTitles[2], messageTexts[18] },
      { false, ALERT_CO,       MONITORINGSTATE_CANCELLED,   true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_CO,       MONITORINGSTATE_DISPATCHING, false, messageTitles[2], messageTexts[22] },
      { false, ALERT_CO,       MONITORINGSTATE_DISPATCHING, true,  messageTitles[2], messageTexts[22] },
      { false, ALERT_CO,       MONITORINGSTATE_DISPATCHED,  false, messageTitles[2], messageTexts[23] },
      { false, ALERT_CO,       MONITORINGSTATE_DISPATCHED,  true,  messageTitles[2], messageTexts[23] },
      { false, ALERT_CO,       MONITORINGSTATE_REFUSED,     false, messageTitles[2], messageTexts[21] },
      { false, ALERT_CO,       MONITORINGSTATE_REFUSED,     true,  messageTitles[2], messageTexts[21] },
      { false, ALERT_CO,       MONITORINGSTATE_FAILED,      false, messageTitles[2], messageTexts[21] },
      { false, ALERT_CO,       MONITORINGSTATE_FAILED,      true,  messageTitles[2], messageTexts[21] },

      { false, ALERT_PANIC,    MONITORINGSTATE_NONE,        false, messageTitles[2], messageTexts[18] },
      { false, ALERT_PANIC,    MONITORINGSTATE_NONE,        true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_PANIC,    MONITORINGSTATE_CANCELLED,   false, messageTitles[2], messageTexts[18] },
      { false, ALERT_PANIC,    MONITORINGSTATE_CANCELLED,   true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_PANIC,    MONITORINGSTATE_DISPATCHING, false, messageTitles[2], messageTexts[24] },
      { false, ALERT_PANIC,    MONITORINGSTATE_DISPATCHING, true,  messageTitles[2], messageTexts[24] },
      { false, ALERT_PANIC,    MONITORINGSTATE_DISPATCHED,  false, messageTitles[2], messageTexts[25] },
      { false, ALERT_PANIC,    MONITORINGSTATE_DISPATCHED,  true,  messageTitles[2], messageTexts[25] },
      { false, ALERT_PANIC,    MONITORINGSTATE_REFUSED,     false, messageTitles[2], messageTexts[26] },
      { false, ALERT_PANIC,    MONITORINGSTATE_REFUSED,     true,  messageTitles[2], messageTexts[26] },
      { false, ALERT_PANIC,    MONITORINGSTATE_FAILED,      false, messageTitles[2], messageTexts[26] },
      { false, ALERT_PANIC,    MONITORINGSTATE_FAILED,      true,  messageTitles[2], messageTexts[26] },

      { false, ALERT_SECURITY, MONITORINGSTATE_NONE,        false, messageTitles[2], messageTexts[18] },
      { false, ALERT_SECURITY, MONITORINGSTATE_NONE,        true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_SECURITY, MONITORINGSTATE_CANCELLED,   false, messageTitles[2], messageTexts[18] },
      { false, ALERT_SECURITY, MONITORINGSTATE_CANCELLED,   true,  messageTitles[2], messageTexts[18] },
      { false, ALERT_SECURITY, MONITORINGSTATE_DISPATCHING, false, messageTitles[2], messageTexts[24] },
      { false, ALERT_SECURITY, MONITORINGSTATE_DISPATCHING, true,  messageTitles[2], messageTexts[24] },
      { false, ALERT_SECURITY, MONITORINGSTATE_DISPATCHED,  false, messageTitles[2], messageTexts[25] },
      { false, ALERT_SECURITY, MONITORINGSTATE_DISPATCHED,  true,  messageTitles[2], messageTexts[25] },
      { false, ALERT_SECURITY, MONITORINGSTATE_REFUSED,     false, messageTitles[2], messageTexts[26] },
      { false, ALERT_SECURITY, MONITORINGSTATE_REFUSED,     true,  messageTitles[2], messageTexts[26] },
      { false, ALERT_SECURITY, MONITORINGSTATE_FAILED,      false, messageTitles[2], messageTexts[26] },
      { false, ALERT_SECURITY, MONITORINGSTATE_FAILED,      true,  messageTitles[2], messageTexts[26] },

      { false, ALERT_WATER,    MONITORINGSTATE_NONE,        false, messageTitles[2], messageTexts[18] },
      { false, ALERT_WATER,    MONITORINGSTATE_NONE,        true,  messageTitles[2], messageTexts[18] },
   };

   private static final ImmutableMap<Key, CancelMessage> messageMap;
   static
   {
      ImmutableMap.Builder<Key, CancelMessage> builder = ImmutableMap.builder();

      for (int i = 0; i < messageData.length; i++)
      {
         Object[] messageRow = messageData[i];

         builder.put(
            new Key(
               (boolean) messageRow[0],
               (String)  messageRow[1],
               (String)  messageRow[2],
               (boolean) messageRow[3]),
            new CancelMessage(
               (String)  messageRow[4],
               (String)  messageRow[5]));
      }

      messageMap = builder.build();
   }

   public static CancelMessage getCancelMessage(
      boolean hubOnlineOrMissing, String alert, String monitoringState, boolean smokeCOClearing)
   {
      return messageMap.get(new Key(hubOnlineOrMissing, alert, monitoringState, smokeCOClearing));
   }

   private static class Key
   {
      private boolean hubOnlineOrMissing;
      private String alert;
      private String monitoringState;
      private boolean smokeCOClearing;

      public Key(boolean hubOnlineOrMissing, String alert, String monitoringState, boolean smokeCOClearing)
      {
         this.hubOnlineOrMissing = hubOnlineOrMissing;
         this.alert = alert;
         this.monitoringState = monitoringState;
         this.smokeCOClearing = smokeCOClearing;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null) return false;
         if (this == obj) return true;
         if (getClass() != obj.getClass()) return false;

         Key other = (Key) obj;

         return new EqualsBuilder()
            .append(hubOnlineOrMissing, other.hubOnlineOrMissing)
            .append(alert,              other.alert)
            .append(monitoringState,    other.monitoringState)
            .append(smokeCOClearing,    other.smokeCOClearing)
            .isEquals();
      }

      @Override
      public int hashCode()
      {
         return new HashCodeBuilder()
            .append(hubOnlineOrMissing)
            .append(alert)
            .append(monitoringState)
            .append(smokeCOClearing)
            .toHashCode();
      }

      @Override
      public String toString()
      {
         return new ToStringBuilder(this)
            .append("hubOnlineOrMissing", hubOnlineOrMissing)
            .append("alert",              alert)
            .append("monitoringState",    monitoringState)
            .append("smokeCOClearing",    smokeCOClearing)
            .toString();
      }
   }

   public static class CancelMessage
   {
      private String title;
      private String text;

      private CancelMessage(String title, String text)
      {
         this.title = title;
         this.text = text;
      }

      public String getTitle()
      {
         return title;
      }

      public String getText()
      {
         return text;
      }
   }
}

