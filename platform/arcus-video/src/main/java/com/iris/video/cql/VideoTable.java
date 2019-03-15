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

import com.datastax.driver.core.Session;

public abstract class VideoTable {
	private final String tableSpace;
	protected final Session session;
	
	public VideoTable(String ts, Session session) {
		this.tableSpace = ts;
		this.session = session;
	}
	
	public String getTableSpace() {
		return this.tableSpace;
	}
	
	/**
	 * Return the full table name with the table space prefix
	 * @return
	 */
	public String getTableName() {
		return tableSpace + "." + getTable();
	}
	
	public abstract String getTable();	//table name without the table space prefix
	
	
}

