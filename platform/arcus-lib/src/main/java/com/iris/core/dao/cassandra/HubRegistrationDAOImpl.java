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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.HubDAOConfig;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.model.HubRegistration;

@Singleton
public class HubRegistrationDAOImpl extends BaseCassandraCRUDDao<String, HubRegistration> implements HubRegistrationDAO {
	private static final Logger log = LoggerFactory.getLogger(HubRegistrationDAOImpl.class);
	private static final Timer streamAllTimer = DaoMetrics.readTimer(HubRegistrationDAO.class, "streamAll");
	
	protected static final String TABLE = "hub_registration";
	
	static class EntityColumns {
	  final static String ID = "id";
	  final static String CREATED = "created";
	  final static String MODIFIED = "modified";
	  final static String LAST_CONNECTED = "lastConnected";
	  final static String STATE = "state";
	  final static String UPGRADE_REQUEST_TIME = "upgradeRequestTime";
	  final static String FIRMWARE_VERSION = "firmwareVersion";
	  final static String TARGET_VERSION = "targetVersion";
	  final static String UPGRADE_ERROR_CODE = "upgradeErrorCode";
	  final static String UPGRADE_ERROR_MSG = "upgradeErrorMessage";
	  final static String DOWNLOAD_PROGRESS = "downloadProgress";
	  final static String UPGRADE_ERROR_TIME = "upgradeErrorTime";
      
	};

	private static final String[] COLUMN_ORDER = {
	  EntityColumns.LAST_CONNECTED,
	  EntityColumns.STATE,
	  EntityColumns.UPGRADE_REQUEST_TIME,
	  EntityColumns.FIRMWARE_VERSION,
	  EntityColumns.TARGET_VERSION,
	  EntityColumns.UPGRADE_ERROR_CODE,
	  EntityColumns.UPGRADE_ERROR_MSG,
	  EntityColumns.DOWNLOAD_PROGRESS,
	  EntityColumns.UPGRADE_ERROR_TIME
	};
	   
	private final CapabilityRegistry registry;
	private final Session session;
	private PreparedStatement streamAll;

	@Inject
	public HubRegistrationDAOImpl(Session session, CapabilityRegistry registry, HubDAOConfig config) {
      super(session, TABLE, COLUMN_ORDER, new String[0], config.getHubRegistrationTtl());
      this.session = session;
      this.registry = registry;
      
      streamAll = CassandraQueryBuilder.select(TABLE)
              .addColumns(BASE_COLUMN_ORDER)
              .addColumns(COLUMN_ORDER)
              .prepare(session);
	}

	@Override
	public Stream<HubRegistration> streamAll() {
		try(Context ctxt = streamAllTimer.time()) {
			Iterator<Row> rows = session.execute(new BoundStatement(streamAll)).iterator();
			return CassandraQueryExecutor.stream(rows, (row) -> buildEntity(row));			
		}
	}


	@Override
	protected List<Object> getValues(HubRegistration entity) {	
		List<Object> values = new LinkedList<Object>();		
		  //Note this needs to be same order as defined in COLUMN_ORDER
	      values.add(entity.getLastConnected());
	      values.add(entity.getState()!=null?entity.getState().name():null);
	      values.add(entity.getUpgradeRequestTime());
	      values.add(entity.getFirmwareVersion());
	      values.add(entity.getTargetVersion());
	      values.add(entity.getUpgradeErrorCode());
	      values.add(entity.getUpgradeErrorMessage());
	      values.add(entity.getDownloadProgress());
	      values.add(entity.getUpgradeErrorTime());
	      log.trace("HubRegistration:Values = [{}]", values );
	      return values;
	}

	@Override
	protected String getIdFromRow(Row row) {
		return row.getString(BaseEntityColumns.ID);
	}

	@Override
	protected String nextId(HubRegistration entity) {
		return entity.getId();  //Should be provided rather than auto generated.
	}

	@Override
	protected HubRegistration createEntity() {
		return new HubRegistration();
	}

	@Override
	protected void populateEntity(Row row, HubRegistration entity) {
		entity.setLastConnected(row.getTimestamp(EntityColumns.LAST_CONNECTED));
		if(!row.isNull(EntityColumns.STATE)) {
			entity.setState(HubRegistration.RegistrationState.valueOf(row.getString(EntityColumns.STATE)));
		}	      
	    entity.setUpgradeRequestTime(row.getTimestamp(EntityColumns.UPGRADE_REQUEST_TIME));
	    entity.setFirmwareVersion(row.getString(EntityColumns.FIRMWARE_VERSION));
	    entity.setTargetVersion(row.getString(EntityColumns.TARGET_VERSION));
	    entity.setUpgradeErrorCode(row.getString(EntityColumns.UPGRADE_ERROR_CODE));
	    entity.setUpgradeErrorMessage(row.getString(EntityColumns.UPGRADE_ERROR_MSG));
	    entity.setDownloadProgress(row.getInt(EntityColumns.DOWNLOAD_PROGRESS));
	    entity.setUpgradeErrorTime(row.getTimestamp(EntityColumns.UPGRADE_ERROR_TIME));
	}


	
	
}

