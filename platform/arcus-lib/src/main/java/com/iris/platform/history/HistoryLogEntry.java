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
/**
 * 
 */
package com.iris.platform.history;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.iris.messages.model.Copyable;
import com.iris.util.IrisUUID;

/**
 * 
 */
public class HistoryLogEntry implements Copyable<HistoryLogEntry> {
   private HistoryLogEntryType type;
   private UUID timestamp;
   private Object id;
   private String messageKey;
   private List<String> values = ImmutableList.of();
   private String subjectAddress;

   /**
    * 
    */
   public HistoryLogEntry() {
      // TODO Auto-generated constructor stub
   }
   
   // constructor for test cases only
   public HistoryLogEntry(HistoryLogEntryType type, long timestamp, UUID id, String messageKey, String subjectAddress, String... values) {
   	this.type = type;
   	this.timestamp = IrisUUID.timeUUID(timestamp, 0);
   	this.id = id;
   	this.messageKey = messageKey;
   	this.subjectAddress = subjectAddress;
   	this.values = ImmutableList.copyOf(values);
   }
   
   public HistoryLogEntry(HistoryLogEntry event) {
      this.type = event.getType();
      this.timestamp = event.getTimestampUuid();
      this.id = event.getId();
      this.messageKey = event.getMessageKey();
      this.values = ImmutableList.copyOf(event.getValues());
      this.subjectAddress = event.getSubjectAddress();
   }

   /**
    * @return the type
    */
   public HistoryLogEntryType getType() {
      return type;
   }

   /**
    * @param type the type to set
    */
   public void setType(HistoryLogEntryType type) {
      this.type = type;
   }

   /**
    * @return the timestamp
    */
   public UUID getTimestampUuid() {
      return timestamp;
   }

   /**
    * @param timestamp the timestamp to set
    */
   public void setTimestamp(UUID timestamp) {
      this.timestamp = timestamp;
   }

   /**
    * @return the timestamp
    */
   public long getTimestamp() {
      return timestamp != null ? IrisUUID.timeof(timestamp) : 0;
   }

   /**
    * @param timestamp the timestamp to set
    */
   public void setTimestamp(long timestamp) {
      this.timestamp = IrisUUID.timeUUID(timestamp, 0);
   }

   /**
    * @return the id
    */
   @Nullable
   public Object getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(Object id) {
      this.id = id;
   }

   /**
    * @return the messageKey
    */
   public String getMessageKey() {
      return messageKey;
   }

   /**
    * @param messageKey the messageKey to set
    */
   public void setMessageKey(String messageKey) {
      this.messageKey = messageKey;
   }

   /**
    * @return the values
    */
   public List<String> getValues() {
      return values;
   }
   
   /**
    * @return only those values that are not null (useful for Cassandra operations that 
    * will not accept null values)
    */
   public List<String> getNonNullValues() {
	   return values.stream().filter(v -> v != null).collect(Collectors.toList());
   }
   
   /**
    * @return the address of the subject of this entry
    */
   public String getSubjectAddress() {
   	return subjectAddress;
   }
   
   /** 
    * @param values
    */
   public void setValues(@Nullable List<String> values) {
      if(values == null) {
         this.values = ImmutableList.of();
      }
      else {
         this.values = values;
      }
   }

   /**
    * @param values the values to set
    */
   public void setValues(String[] values) {
      setValues(Arrays.asList(values));
   }
   
   /**
    * @param subjectAddress address of the subject of this entry
    */
   public void setSubjectAddress(String subjectAddress) {
   	this.subjectAddress = subjectAddress;
   }
   
   @Override
   public HistoryLogEntry copy() {
      return new HistoryLogEntry(this);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "HistoryLogEntry [type=" + type + ", timestamp=" + (timestamp == null ? null : new Date(getTimestamp()))
            + ", id=" + id + ", messageKey=" + messageKey + ", values="
            + values + ", subjectAddress=" + subjectAddress + "]";
   }

   @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
		result = prime * result + ((subjectAddress == null) ? 0 : subjectAddress.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : (int) IrisUUID.timeof(timestamp) ^ 32);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
		return result;
	}

   @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HistoryLogEntry other = (HistoryLogEntry) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (messageKey == null) {
			if (other.messageKey != null)
				return false;
		} else if (!messageKey.equals(other.messageKey))
			return false;
		if (subjectAddress == null) {
			if (other.subjectAddress != null)
				return false;
		} else if (!subjectAddress.equals(other.subjectAddress))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (IrisUUID.timeof(timestamp)  != IrisUUID.timeof(other.timestamp))
			return false;
		if (type != other.type)
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}

