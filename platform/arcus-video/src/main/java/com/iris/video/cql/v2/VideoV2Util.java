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
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.VideoService;
import com.iris.messages.service.VideoService.QuotaReportEvent;
import com.iris.util.IrisUUID;
import com.iris.video.VideoUtil;
import com.iris.video.cql.PlaceQuota.Unit;
import com.iris.video.recording.ConstantVideoTtlResolver;

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
	
	static void executeAndUpdateTimer(Session session, Statement stmt, Timer timer) {
		long startTime = System.nanoTime();
		try{
			session.execute(stmt);
		}finally{
			if(timer != null) {
				timer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			}
		}
	}
	
	static ResultSetFuture executeAsyncAndUpdateTimer(Session session, Statement stmt, Timer timer) {
		long startTime = System.nanoTime();
		try{
			ResultSetFuture rs = session.executeAsync(stmt);
			if(timer != null) {
				rs.addListener(() -> timer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), MoreExecutors.directExecutor());
			}
			return rs;
		}finally{
			
		}
	}
	
	public static void executeBatchWithLimit(Session session, BatchStatement batchStmt, int batchSize, Timer timer) {
		long startTime = System.nanoTime();
		try{
			if(batchStmt.size() > batchSize) {
	   		//Need to break into multiple batches
	   		int numOfBatches = batchStmt.size()/batchSize + 1;    
	   		List<BatchStatement> batchList = new ArrayList<>(numOfBatches);
	   		Iterator<Statement> allStatements = batchStmt.getStatements().iterator();
	   		BatchStatement curBatch = new BatchStatement();
	   		batchList.add(curBatch);
	   		int count = 0;
	   		while(allStatements.hasNext()) {
	   			curBatch.add(allStatements.next());
	   			if(++count > batchSize) {	   				
	   				curBatch = new BatchStatement();
	   				batchList.add(curBatch);
	   				count = 0;
	   			}
	   		}
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
	
	public static List<Statement> getDataCleanupStatements(String tableSpace) {
		List<Statement> stmts = new ArrayList<>();
		stmts.add(QueryBuilder.truncate(tableSpace, PlaceRecordingIndexV2Table.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, PlaceRecordingIndexV2FavoriteTable.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, PurgeRecordingV2Table.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, RecordingV2Table.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, RecordingV2FavoriteTable.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, VideoMetadataV2Table.TABLE_NAME));
		stmts.add(QueryBuilder.truncate(tableSpace, VideoMetadataV2FavoriteTable.TABLE_NAME));		
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

