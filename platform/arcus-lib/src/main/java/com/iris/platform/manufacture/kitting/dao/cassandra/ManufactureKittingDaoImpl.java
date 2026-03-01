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
package com.iris.platform.manufacture.kitting.dao.cassandra;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.io.json.JSON;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.manufacture.kitting.kit.Kit;
import com.iris.platform.manufacture.kitting.kit.KitDevice;

@Singleton
public class ManufactureKittingDaoImpl implements ManufactureKittingDao {
	public final static String TABLE = "manufacture_kit";

	enum KitColumns { hubid, type, devices };

	private final CqlSession session;

	private final PreparedStatement createKit;
	private final PreparedStatement deleteKit;
	private final PreparedStatement getKit;


	@Inject
	public ManufactureKittingDaoImpl(CqlSession session) {
		this.session = session;

		createKit = CassandraQueryBuilder
						.insert(TABLE)
						.addColumns(KitColumns.values())
						.prepare(session);
		deleteKit = CassandraQueryBuilder
				.delete(TABLE)
				.addWhereColumnEquals(KitColumns.hubid)
				.prepare(session);
		getKit = CassandraQueryBuilder
					.select(TABLE)
					.addColumns(KitColumns.values())
					.addWhereColumnEquals(KitColumns.hubid)
					.prepare(session);
	}

	@Override
	public void createKit(Kit kit) {
		BoundStatement bound = createKit.bind();
		bound = bound.setString(KitColumns.hubid.name(), kit.getHubId());
		bound = bound.setString(KitColumns.type.name(), kit.getType());
		bound = bound.setList(KitColumns.devices.name(), convertToString(kit.getDevices()), String.class);
		session.execute(bound);
	}

	@Override
	public void deleteKit(String hubId) {
		BoundStatement bound = deleteKit.bind(hubId);
		session.execute(bound);
	}

	@Override
	@Nullable
	public Kit getKit(String hubId) {
		BoundStatement bound = getKit.bind(hubId);
		ResultSet results = session.execute(bound);
		Row row = results.one();
		if (row == null) return null;

		List<String> deviceStrings = row.getList(KitColumns.devices.name(), String.class);
		List<KitDevice> devices = convertFromString(deviceStrings);

		Kit kit = Kit.builder()
						.withHubId(row.getString(KitColumns.hubid.name()))
						.withType(row.getString(KitColumns.type.name()))
						.withDevices(devices)
						.build();

		return kit;
	}

	List<KitDevice> convertFromString(List<String> deviceStrings) {
		return deviceStrings
				.stream()
				.map(dev -> JSON.fromJson(dev, KitDevice.class))
				.collect(Collectors.toList());
	}

	List<String> convertToString(List<KitDevice> devices) {
		return devices
				.stream()
				.map(dev -> JSON.toJson(dev))
				.collect(Collectors.toList());
	}
}
