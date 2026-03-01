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
package com.iris.core.dao.cassandra;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.security.dao.AppHandoffDao;

/**
 * @author tweidlin
 *
 */
@Singleton
public class CassandraAppHandoffDao implements AppHandoffDao {
	private static final Logger logger = LoggerFactory.getLogger(CassandraAppHandoffDao.class);

	private static final String TABLE = "app_handoff_token";

	private static enum Column {
		handoffToken,
		personId,
		ip,
		url,
		username
	}

	private static final Timer newToken = DaoMetrics.insertTimer(AppHandoffDao.class, "newToken");
	private static final Timer validateToken = DaoMetrics.readTimer(AppHandoffDao.class, "validateToken");

	private final int tokenLength;
	private final CqlSession session;
	private final PreparedStatement selectToken;
	private final PreparedStatement insertToken;
	private final PreparedStatement updateToken;
	private final PreparedStatement clearToken;

	@Inject
	public CassandraAppHandoffDao(
			CassandraAppHandoffConfig config,
			CqlSession session
	) {
		this.tokenLength = config.getTokenLength();
		this.session = session;
		this.selectToken =
			CassandraQueryBuilder
				.select(TABLE)
				.addColumns(Column.values())
				.addWhereColumnEquals(Column.handoffToken)
				.prepare(session);
		this.insertToken =
			CassandraQueryBuilder
				.insert(TABLE)
				.addColumns(Column.values())
				.withTtlSec(config.getTtlSec())
				.prepare(session);
		this.updateToken =
			CassandraQueryBuilder
				.update(Tables.PERSON)
				.addColumn(Tables.PersonCols.HANDOFF_TOKEN)
				.addWhereColumnEquals(Tables.PersonCols.ID)
				.withTtlSec(config.getTtlSec())
				.prepare(session);
		this.clearToken =
			CassandraQueryBuilder
				.update(Tables.PERSON)
				.set(String.format("%s = null", Tables.PersonCols.HANDOFF_TOKEN))
				.addWhereColumnEquals(Tables.PersonCols.ID)
				.ifClause(String.format("%s = ?", Tables.PersonCols.HANDOFF_TOKEN))
				.prepare(session);
	}

	@Override
	public String newToken(SessionHandoff handoff) {
		String token = Utils.randomTokenString(tokenLength);
		try(Timer.Context ctx = newToken.time()) {
			BatchStatementBuilder batch = BatchStatement.builder(DefaultBatchType.LOGGED);
			batch.addStatement( insertToken.bind(token, handoff.getPersonId(), handoff.getIp(), handoff.getUrl(), handoff.getUsername()) );
			batch.addStatement( updateToken.bind(token, handoff.getPersonId()) );
			session.execute( batch.build() );
			return token;
		}
	}

	@Override
	public Optional<SessionHandoff> validate(String token) {
		try(Timer.Context ctx = validateToken.time()) {
			BoundStatement bs = selectToken.bind( token );
			ResultSet result = session.execute( bs );
			if(result.getAvailableWithoutFetching() == 0) {
				logger.debug("No token exists for [{}]", token);
				return Optional.empty();
			}
			SessionHandoff handoff = translate(result.one());
			UUID personId = handoff.getPersonId();
			bs = clearToken.bind(personId, token);
			result = session.execute( bs );
			if(!result.wasApplied()) {
				logger.debug("Token [{}] has already been used / invalidated", token);
				return Optional.empty();
			}
			return Optional.of(handoff);
		}
	}

	private SessionHandoff translate(Row one) {
		SessionHandoff handoff = new SessionHandoff();
		handoff.setPersonId(one.getUuid(Column.personId.name()));
		handoff.setUrl(one.getString(Column.url.name()));
		handoff.setIp(one.getString(Column.ip.name()));
		handoff.setUsername(one.getString(Column.username.name()));
		return handoff;
	}

}
