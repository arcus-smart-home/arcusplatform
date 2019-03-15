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

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.model.ChildId;
import com.iris.platform.PagedQuery;
import com.iris.platform.PagedResults;
import com.iris.util.IrisUUID;

/**
 * 
 */
public interface HistoryLogDAO {
   public static final int MAX_PROCESSED_ROWS = 1000;

   PagedResults<HistoryLogEntry> listCriticalEntriesByPlace(UUID placeId, int limit);

   PagedResults<HistoryLogEntry> listEntriesByPlace(UUID placeId, int limit);

   PagedResults<HistoryLogEntry> listEntriesByDevice(UUID deviceId, int limit);
   
   PagedResults<HistoryLogEntry> listEntriesByHub(String hubId, int limit);

   PagedResults<HistoryLogEntry> listEntriesByActor(UUID actorId, int limit);

   PagedResults<HistoryLogEntry> listEntriesByRule(ChildId ruleId, int limit);
   
   PagedResults<HistoryLogEntry> listEntriesBySubsystem(SubsystemId subsystemId, int limit);
   
   PagedResults<HistoryLogEntry> listEntriesByAlarmIncident(UUID incidentId, int limit);
   
   PagedResults<HistoryLogEntry> listEntriesByQuery(ListEntriesQuery query);
   
   default PagedResults<HistoryLogEntry> listEntriesByAddress(String address, int limit) {
      return listEntriesByAddress(Address.fromString(address), limit);
   }
   
   default PagedResults<HistoryLogEntry> listEntriesByAddress(Address address, int limit) {
      Preconditions.checkNotNull(address.getId(), "Invalid address, must have an object id");
      switch((String) address.getGroup()) {
      case PlaceCapability.NAMESPACE:
         return listEntriesByPlace((UUID) address.getId(), limit);
      case DeviceCapability.NAMESPACE:
         return listEntriesByDevice((UUID) address.getId(), limit);
      case PersonCapability.NAMESPACE:
         return listEntriesByActor((UUID) address.getId(), limit);
      case RuleCapability.NAMESPACE:
         return listEntriesByRule((ChildId) address.getId(), limit);
      case AlarmIncidentCapability.NAMESPACE:
      	return listEntriesByAlarmIncident((UUID) address.getId(), limit);
      default:
    	  if (address.isHubAddress()) {
    		return listEntriesByHub(address.getHubId(), limit);  
    	  } else {
    		  throw new IllegalArgumentException("Invalid object type for listing events: " + address.getGroup());
    	  }
      }
   }
   
   public static class ListEntriesQuery extends PagedQuery {
      private HistoryLogEntryType type;
      private Object id;
      private UUID before;
      private Predicate<HistoryLogEntry> filter;
      
      public ListEntriesQuery() {
      	
      }
      
      public ListEntriesQuery(ListEntriesQuery query) {
      	super(query);
			this.type = query.getType();
			this.id = query.getId();
			this.before = query.getBeforeUuid();
			this.filter = query.getFilter();
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
      
      public Object getId() {
         return id;
      }
      
      public void setId(Object id) {
         this.id = id;
      }
      
      public UUID getBeforeUuid() {
      	return before;
      }
      
      public void setBeforeUuid(UUID before) {
      	this.before = before;
      }
      
      /**
       * @return the after
       */
      public Date getBefore() {
         return before != null ? new Date(IrisUUID.timeof(before)) : null;
      }
      
      /**
       * @param before the after to set
       */
      public void setBefore(Date before) {
      	if(before == null) {
      		this.before = null;
      	}
      	else {
      		this.before = IrisUUID.timeUUID(before, 0);
      	}
      }
      
      public void setToken(String token) {
         if(StringUtils.isEmpty(token)) {
            return;
         }
         if(token.length() == 36) { // UUID
         	this.before = UUID.fromString(token); 
         }
         else {
         	long value = Long.valueOf(token);
         	this.before = IrisUUID.timeUUID(value, 0);
         }
         super.setToken(token);
      }

      /**
       * @return the filter
       */
      @Nullable
      public Predicate<HistoryLogEntry> getFilter() {
         return filter;
      }

      /**
       * Applies an in-memory filter to the query.  NOTE this may
       * be very expensive if the filter does not match most things.
       * @param filter the filter to set
       */
      public void setFilter(Predicate<HistoryLogEntry> filter) {
         this.filter = filter;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "ListEntriesQuery [type=" + type + ", id=" + id + ", before="
               + before + ", token=" + getToken() + ", limit="
               + getLimit() + "]";
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = super.hashCode();
         result = prime * result + ((before == null) ? 0 : before.hashCode());
         result = prime * result + ((id == null) ? 0 : id.hashCode());
         result = prime * result + ((type == null) ? 0 : type.hashCode());
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (!super.equals(obj)) return false;
         if (getClass() != obj.getClass()) return false;
         ListEntriesQuery other = (ListEntriesQuery) obj;
         if (before == null) {
            if (other.before != null) return false;
         }
         else if (!before.equals(other.before)) return false;
         if (id == null) {
            if (other.id != null) return false;
         }
         else if (!id.equals(other.id)) return false;
         if (type != other.type) return false;
         return true;
      }

   }

}

