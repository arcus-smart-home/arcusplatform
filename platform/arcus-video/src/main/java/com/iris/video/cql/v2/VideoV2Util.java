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
package com.iris.video.cql.v2;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.codahale.metrics.Timer;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.VideoService;
import com.iris.messages.service.VideoService.QuotaReportEvent;
import com.iris.util.IrisUUID;
import com.iris.video.VideoUtil;
import com.iris.video.cql.PlaceQuota.Unit;
import com.iris.video.recording.ConstantVideoTtlResolver;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

public class VideoV2Util {

	public static long formatDate(Date dt) {
		if(dt != null) {
			return dt.getTime();
		}else{
			return 0;
		}
	}

	public static UUID createExpirationIdFromTTL(UUID recordingId, long ttlInSeconds) {
		//recordingId is a timed UUID
		return IrisUUID.timeUUID(ttlInSeconds*1000+IrisUUID.timeof(recordingId));
	}

	public static long createActualTTL(UUID recordingId, UUID expirationId) {
		if(expirationId != null) {
			//return (long) ((expirationId.timestamp() - recordingId.timestamp()) / 10000000);
			return TimeUnit.MILLISECONDS.toSeconds(IrisUUID.timeof(expirationId) - IrisUUID.timeof(recordingId));
		}else{
			return ConstantVideoTtlResolver.getDefaultTtlInSeconds();
		}
	}

	public static long createExpirationFromTTL(UUID recordingId, long ttlInSeconds) {
		Date purgeAt = VideoUtil.getPurgeTimestamp(IrisUUID.timeof(recordingId), ttlInSeconds*1000, TimeUnit.MILLISECONDS);
		return purgeAt.getTime();
	}

	public static long createActualTTL(UUID recordingId, long expirationInMs) {
		if(expirationInMs > 0) {
			return (expirationInMs - IrisUUID.timeof(recordingId)) / 1000l;
		}else{
			return ConstantVideoTtlResolver.getDefaultTtlInSeconds();
		}
	}

	static void executeAndUpdateTimer(CqlSession session, Statement<?> stmt, Timer timer) {
		long startTime = System.nanoTime();
		try{
			session.execute(stmt);
		}finally{
			if(timer != null) {
				timer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			}
		}
	}

	static CompletionStage<AsyncResultSet> executeAsyncAndUpdateTimer(CqlSession session, Statement<?> stmt, Timer timer) {
		long startTime = System.nanoTime();
		CompletionStage<AsyncResultSet> rs = session.executeAsync(stmt);
		if(timer != null) {
			rs.whenComplete((result, err) -> timer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
		}
		return rs;
	}

	public static void executeBatchWithLimit(CqlSession session, BatchStatement batchStmt, int batchSize, Timer timer) {
		long startTime = System.nanoTime();
		try{
			if(batchStmt.size() > batchSize) {
	   		//Need to break into multiple batches
	   		int numOfBatches = batchStmt.size()/batchSize + 1;
	   		List<BatchStatement> batchList = new ArrayList<>(numOfBatches);
	   		Iterator<BatchableStatement<?>> allStatements = batchStmt.iterator();
	   		BatchStatementBuilder curBatch = BatchStatement.builder(DefaultBatchType.LOGGED);
	   		int count = 0;
	   		while(allStatements.hasNext()) {
	   			curBatch.addStatement(allStatements.next());
	   			if(++count > batchSize) {
	   				batchList.add(curBatch.build());
	   				curBatch = BatchStatement.builder(DefaultBatchType.LOGGED);
	   				count = 0;
	   			}
	   		}
	   		batchList.add(curBatch.build());
	   		batchList.forEach(cur -> {
	   			session.execute( cur );
	   		});
	   	}else{
	   		session.execute( batchStmt );
	   	}
		}finally{
			if(timer != null) {
				timer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			}
		}
	}

	public static List<SimpleStatement> getDataCleanupStatements(String tableSpace) {
		List<SimpleStatement> stmts = new ArrayList<>();
		stmts.add(truncate(tableSpace, PlaceRecordingIndexV2Table.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, PlaceRecordingIndexV2FavoriteTable.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, PurgeRecordingV2Table.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, RecordingV2Table.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, RecordingV2FavoriteTable.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, VideoMetadataV2Table.TABLE_NAME).build());
		stmts.add(truncate(tableSpace, VideoMetadataV2FavoriteTable.TABLE_NAME).build());
		return stmts;
	}

	public static PlatformMessage createQuotaReportEvent(UUID placeId, String population, long used, long usedTimestamp, Unit unit, boolean favorite) {
		MessageBody report =
				VideoService.QuotaReportEvent
					.builder()
					.withUsed(used)
					.withFavorite(favorite)
					.withUnit(Unit.Number.equals(unit)?QuotaReportEvent.UNIT_NUMBER:QuotaReportEvent.UNIT_BYTES)
					.build();
		PlatformMessage message =
				PlatformMessage
					.buildEvent(report, VideoUtil.SERVICE_ADDRESS)
					.withPlaceId(placeId)
					.withPopulation(population)
					.withTimestamp(usedTimestamp)
					.create();
		return message;
	}

	//Round dt to the beginning of the next day
	public static Date getStartOfNextDay(Date dt) {
		DateTime d = new DateTime(dt.getTime());
		d = d.plusDays(1);
		return d.withTimeAtStartOfDay().toDate();
	}
}
