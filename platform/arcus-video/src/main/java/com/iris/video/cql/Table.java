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
package com.iris.video.cql;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import com.datastax.driver.core.Session;
import com.iris.video.VideoDaoConfig;

public class Table {
	private static final ConcurrentHashMap<Key, Object> Cache = new ConcurrentHashMap<>();
	
	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, String ts, Class<T> tableType) {
		return (T) Cache.computeIfAbsent(new Key(session, ts, tableType), (k) -> newInstance(session, ts, tableType, null));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, String ts, Class<T> tableType, VideoDaoConfig config) {
		return (T) Cache.computeIfAbsent(new Key(session, ts, tableType), (k) -> newInstance(session, ts, tableType, config));
	}
	
	private static <T> T newInstance(Session session, String ts, Class<T> tableType, VideoDaoConfig config) {
		try{
			if(config != null) {
				return tableType.getConstructor(String.class, Session.class, VideoDaoConfig.class).newInstance(ts, session, config);
			}else{
				return tableType.getConstructor(String.class, Session.class).newInstance(ts, session);
			}
		}catch(NoSuchMethodException e) {
			throw new IllegalArgumentException("Must have a public constructor that takes a Session object as the only argument. " + tableType + " does not.", e);
		}catch (Exception e) {
			throw new IllegalArgumentException("Unable to create instance of " + tableType, e);
		}
	}
	
	private static class Key {
		final WeakReference<Session> sessionRef;
		final Class<?> tableType;
		final String tableSpace;
		
		Key(Session session, String ts, Class<?> tableType) {
			this.sessionRef = new WeakReference<Session>(session);
			this.tableType = tableType;
			this.tableSpace = ts;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sessionRef.get() == null) ? 0 : sessionRef.get().hashCode());
			result = prime * result + ((tableType == null) ? 0 : tableType.hashCode());
			result = prime * result + ((tableSpace == null) ? 0 : tableSpace.hashCode());
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
			Key other = (Key) obj;
			Session session = sessionRef.get();
			if (session == null) {
				Session otherSession = sessionRef.get();
				if (otherSession != null)
					return false;
			} else if (!session.equals(other.sessionRef.get()))
				return false;
			if (tableType == null) {
				if (other.tableType != null)
					return false;
			} else if (!tableType.equals(other.tableType))
				return false;
			if (tableSpace == null) {
				if (other.tableSpace != null)
					return false;
			} else if (!tableSpace.equals(other.tableSpace))
				return false;
			return true;
		}
		
		
	}
}

