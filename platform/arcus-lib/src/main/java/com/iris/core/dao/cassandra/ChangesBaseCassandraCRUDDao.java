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
package com.iris.core.dao.cassandra;

import com.datastax.driver.core.Session;
import com.iris.messages.model.BaseEntity;

public abstract class ChangesBaseCassandraCRUDDao<I, T extends BaseEntity<I, T>> extends BaseCassandraCRUDDao<I, T> {	
	private final ChangeTracker<T>[] trackers;
	
	protected ChangesBaseCassandraCRUDDao(Session session, String table, String[] columns, ChangeTracker<T>[] trackers) {
	   super(session, table, columns);
	   this.trackers = trackers;
   }
	
	@Override
   protected T doUpdate(T entity) {
	   // Have to check to see if the value has changed.
		if (trackers != null && trackers.length > 0) {
			I id = entity.getId();
			T currentEntity = findById(id);
			for (ChangeTracker<T> tracker : trackers) {
				entity = tracker.checkForChange(currentEntity, entity);
			}
		}
	   return super.doUpdate(entity);
   }
}

