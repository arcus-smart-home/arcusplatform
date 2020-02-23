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

import static com.iris.video.VideoMetrics.VIDEO_ATTR_READONLY;
import static com.iris.video.VideoUtil.toblob;
import static com.iris.video.cql.v2.VideoV2Util.executeAndUpdateTimer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BatchStatement.Type;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.platform.PagedResults;
import com.iris.util.IrisUUID;
import com.iris.video.PlacePurgeRecord;
import com.iris.video.PlacePurgeRecord.PurgeMode;
import com.iris.video.StorageUsed;
import com.iris.video.VideoDao;
import com.iris.video.VideoDaoConfig;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoQuery;
import com.iris.video.VideoRecording;
import com.iris.video.VideoRecordingSize;
import com.iris.video.VideoType;
import com.iris.video.VideoUtil;
import com.iris.video.cql.MetadataAttribute;
import com.iris.video.cql.RecordingTableField;
import com.iris.video.cql.Table;
import com.iris.video.cql.VideoConstants;
import com.iris.video.cql.v2.AbstractPlaceRecordingIndexV2Table.Field;


@Singleton
public class CassandraVideoV2Dao implements VideoDao {
	private static final Logger logger = LoggerFactory.getLogger(CassandraVideoV2Dao.class);
	
	private static final Timer InsertVideoTimer = DaoMetrics.insertTimer(VideoDao.class, "video");
	private static final Timer InsertFavoriteVideoTimer = DaoMetrics.insertTimer(VideoDao.class, "favoriteVideo");
	private static final Timer InsertFrameTimer = DaoMetrics.insertTimer(VideoDao.class, "frame");
	private static final Timer UpdateVideoTimer = DaoMetrics.updateTimer(VideoDao.class, "update");
	private static final Timer AddTagsTimer     = DaoMetrics.updateTimer(VideoDao.class, "addTags");
	private static final Timer RemoveTagsTimer  = DaoMetrics.updateTimer(VideoDao.class, "removeTags");
	private static final Timer CompleteTimer    = DaoMetrics.insertTimer(VideoDao.class, "complete");
	private static final Timer DeleteTimer      = DaoMetrics.updateTimer(VideoDao.class, "delete");
	private static final Timer PurgeTimer       = DaoMetrics.deleteTimer(VideoDao.class, "purge");
	
	private static final StorageUsed ZeroStorageUsed = new StorageUsed(0, System.currentTimeMillis());
	private final Session session;
	private final VideoDaoConfig config;
	private final VideoMetadataV2Table recordingMetadataTable;
	private final PlaceRecordingIndexV2Table placeRecordingIndex;
	private final RecordingV2Table recordingTable;
	private final PurgeRecordingV2Table purgeTable;
	private final VideoMetadataV2FavoriteTable recordingMetadataFavoriteTable;
	private final PlaceRecordingIndexV2FavoriteTable placeRecordingIndexFavorite;
	private final RecordingV2FavoriteTable recordingFavoriteTable;
	private final PlacePurgeRecordingTable purgePinnedRecordingTable;
	
	@Inject
	public CassandraVideoV2Dao(Session session, VideoDaoConfig config) {
		this.session = session;
		this.config = config;
		this.recordingMetadataTable = Table.get(session, config.getTableSpace(), VideoMetadataV2Table.class);
		this.placeRecordingIndex = Table.get(session, config.getTableSpace(), PlaceRecordingIndexV2Table.class);
		this.recordingTable = Table.get(session, config.getTableSpace(), RecordingV2Table.class);
		this.purgeTable = Table.get(session, config.getTableSpace(), PurgeRecordingV2Table.class);
		this.recordingMetadataFavoriteTable = Table.get(session, config.getTableSpace(), VideoMetadataV2FavoriteTable.class);
		this.placeRecordingIndexFavorite = Table.get(session, config.getTableSpace(), PlaceRecordingIndexV2FavoriteTable.class);
		this.recordingFavoriteTable = Table.get(session, config.getTableSpace(), RecordingV2FavoriteTable.class);
		this.purgePinnedRecordingTable = Table.get(session, config.getTableSpace(), PlacePurgeRecordingTable.class, config);
	}
	
	@Override
	public void insert(VideoMetadata metadata) {
		BatchStatement stmt = new BatchStatement(Type.UNLOGGED); // recording will be atomic, and place_recording will be atomic, but they will be independently atomic to save performance
		
		UUID placeId = metadata.getPlaceId();
		UUID recordingId = metadata.getRecordingId();
		UUID personId = metadata.getPersonId();
		
		// Recording Metadata Table Mutations
		long expiration = metadata.getExpiration();
		Date purgeAt = new Date(expiration);
		//Make sure expiration value matches with delete time since we add a delay + round to the next hour when calculating the delete time
		//Therefore, the expiration time will be a little longer than what is defined for the service level.
		long actualTtlInSec = VideoV2Util.createActualTTL(recordingId, expiration);
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.NAME, metadata.getName()));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.PLACEID, String.valueOf(metadata.getPlaceId())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.ACCOUNTID, String.valueOf(metadata.getAccountId())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.CAMERAID, String.valueOf(metadata.getCameraId())));

		if (personId != null) {
			stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.PERSONID, String.valueOf(personId)));
		}

		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.WIDTH, String.valueOf(metadata.getWidth())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.HEIGHT, String.valueOf(metadata.getHeight())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.BANDWIDTH, String.valueOf(metadata.getBandwidth())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.FRAMERATE, String.valueOf(metadata.getFramerate())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.PRECAPTURE, String.valueOf(metadata.getPrecapture())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.LOCATION, String.valueOf(metadata.getLoc())));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.TYPE, metadata.isStream() ? VideoMetadataV2Table.ATTR_TYPE_STREAM : VideoMetadataV2Table.ATTR_TYPE_RECORDING));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.EXPIRATION, String.valueOf(expiration)));		   
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.DELETED_TIME, String.valueOf(purgeAt.getTime())));
		
		int partitionId = VideoDao.calculatePartitionId(recordingId, config.getPurgePartitions());
		metadata.setDeletionPartition(partitionId);
      stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.DELETED_PARTITION, String.valueOf(partitionId)));
      
		if(metadata.getVideoCodec() != null) {
			stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.VIDEO_CODEC, metadata.getVideoCodec().name()));
		}

		if(metadata.getAudioCodec() != null) {
			stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSec, MetadataAttribute.AUDIO_CODEC, metadata.getAudioCodec().name()));
		}

		// Recording Table Mutations
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.STORAGE, toblob(metadata.getLoc())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.ACCOUNT, toblob(metadata.getAccountId())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.PLACE, toblob(metadata.getPlaceId())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.CAMERA, toblob(metadata.getCameraId())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.EXPIRATION, toblob(expiration)));

		if (metadata.getPersonId() != null) {
			stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.PERSON, toblob(metadata.getPersonId())));
		}

		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.WIDTH, toblob(metadata.getWidth())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.HEIGHT, toblob(metadata.getHeight())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.BANDWIDTH,toblob(metadata.getBandwidth())));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.FRAMERATE,toblob(metadata.getFramerate())));

		if(metadata.getVideoCodec() != null) {
			stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.VIDEO_CODEC, toblob(metadata.getVideoCodec())));
		}

		if(metadata.getAudioCodec() != null) {
			stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSec, RecordingTableField.AUDIO_CODEC, toblob(metadata.getAudioCodec())));
		}

		// Recording Metadata Index Mutations
		stmt.add(placeRecordingIndex.insertCamera(placeId, recordingId, metadata.getCameraId(), expiration, actualTtlInSec));
		stmt.add(placeRecordingIndex.insertVideo(placeId, recordingId, metadata.isStream() ? PlaceRecordingIndexV2Table.Type.STREAM : PlaceRecordingIndexV2Table.Type.RECORDING, expiration, actualTtlInSec));
		
		// Purge table		
		addPurgeStatements(stmt, placeId, recordingId, purgeAt, partitionId, metadata.getLoc(), !metadata.isStream());	
		VideoV2Util.executeAndUpdateTimer(session, stmt, InsertVideoTimer);		
	}

	@Override
	public ListenableFuture<?> insertIFrame(UUID recordingId, double tsInSeconds, long frameByteOffset, long frameByteSize, long ttlInSeconds) {
		Statement stmt = recordingTable.insertIFrame(recordingId, ttlInSeconds, tsInSeconds, frameByteOffset, toblob(frameByteSize));
		long startTime = System.nanoTime();
		ListenableFuture<?> result = session.executeAsync(stmt);
		result.addListener(() -> InsertFrameTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), MoreExecutors.directExecutor());
		return result;
	}

	@Override
	public void update(UUID placeId, UUID recordingId, long ttlInSeconds, Map<String, Object> attributes) {
		if(attributes == null || attributes.isEmpty()) {
			return;
		}
		long expiration = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		long actualTtlInSeconds = VideoV2Util.createActualTTL(recordingId, expiration);
		BatchStatement stmt = new BatchStatement();
		for (Map.Entry<String,Object> entry : attributes.entrySet()) {
			switch (entry.getKey()) {
			case RecordingCapability.ATTR_NAME:
				stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSeconds, MetadataAttribute.NAME, (String) entry.getValue()));
				break;

			default:
				VIDEO_ATTR_READONLY.inc();
				throw new ErrorEventException(Errors.invalidRequest("attribute is not writable: " + entry.getKey()));
			}
		}

		long startTime = System.nanoTime();
		if(!session.execute(stmt).wasApplied()) {
			throw new NotFoundException(Address.platformService(recordingId, "recording"));
		}
		UpdateVideoTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
	}

	@Override
	public void addTags(UUID placeId, UUID recordingId, Set<String> tags, long ttlInSeconds) {
		if(tags != null && tags.size() > 0) {
			if(tags.contains(VideoConstants.TAG_FAVORITE)) {
				addFavoriteTags(placeId, recordingId, tags, ttlInSeconds);
			}else{
				addNonFavoriteTags(placeId, recordingId, tags, ttlInSeconds);
			}
		}
		
	}	

	@Override
	public ListenableFuture<Set<String>> removeTags(UUID placeId, UUID recordingId, Set<String> tags) {				
		if(tags.contains(VideoConstants.TAG_FAVORITE)) {
			return removeFavoriteTags(placeId, recordingId, tags);
		}else{
			//TODO - this logic is not correct if we support other tags and a video is marked as FAVORITE
			//Currently this method only removes the tags in the normal tag.
			removeNonFavoriteTags(placeId, recordingId, tags);
			VideoMetadata metadata = findByPlaceAndId(placeId, recordingId);
			return Futures.immediateFuture(metadata.getTags());
		}		
	}

	private ListenableFuture<Set<String>> removeFavoriteTags(UUID placeId, UUID recordingId, Set<String> tags) {
		VideoMetadata metadata = findByPlaceAndId(placeId, recordingId);
		if(metadata != null && metadata.getTags().contains(VideoConstants.TAG_FAVORITE)) {
			BatchStatement stmt = new BatchStatement(BatchStatement.Type.LOGGED);
			addStatementsForRemoveFromFavoriteTables(stmt, metadata);	
			return Futures.transformAsync(
					VideoV2Util.executeAsyncAndUpdateTimer(session, stmt, RemoveTagsTimer),
	            (AsyncFunction<ResultSet, Set<String>>) input -> {
	            	Set<String> expectedTags = new HashSet<>(metadata.getTags());
	   				expectedTags.removeAll(tags);
	               return Futures.immediateFuture(expectedTags);
	            },
	            MoreExecutors.directExecutor()
	         );
		}else{
			logger.warn("Can not removeFavoriteTags.  Either recording id [{}] is invalid or video does not contain Favorite tag [{}]", recordingId, metadata.getTags());
			return Futures.immediateFuture(ImmutableSet.<String>of());
			
		}
	}
	
	private void addStatementsForRemoveFromFavoriteTables(BatchStatement stmt, VideoMetadata metadata) {
		UUID placeId = metadata.getPlaceId();
		UUID recordingId = metadata.getRecordingId();
		//Delete placeRecordingIndexFavorite
		for(String tag: metadata.getTags()) {
			stmt.add(placeRecordingIndexFavorite.deleteTag(placeId, recordingId, tag));
		}
		stmt.add(placeRecordingIndexFavorite.deleteVideo(placeId, recordingId, PlaceRecordingIndexV2Table.Type.RECORDING));
		stmt.add(placeRecordingIndexFavorite.deleteCamera(placeId, recordingId, metadata.getCameraId()));
		if(metadata.isDeleted()) {
			stmt.add(placeRecordingIndexFavorite.deleteDeleted(placeId, recordingId));
		}
		//Delete metadata
		stmt.add(recordingMetadataFavoriteTable.deleteRecording(recordingId));
		//Delete recording
		stmt.add(recordingFavoriteTable.deleteRecording(recordingId));
		//Add to purge table
		Date purgeAt = null;
		if(metadata.getDeletionTime() != null && metadata.getDeletionTime().before(new Date())) {
			//already expired
			purgeAt = VideoUtil.getPurgeTimestamp(config.getPurgeDelay(), TimeUnit.MILLISECONDS); 
		}else{
			purgeAt = metadata.getDeletionTime();
		}	 
		addPurgeStatements(stmt, placeId, recordingId, purgeAt, metadata.getDeletionPartition(), metadata.getLoc(), true);
	}
	
	
	private void removeNonFavoriteTags(UUID placeId, UUID recordingId, Set<String> tags) {
		BatchStatement stmt = new BatchStatement(BatchStatement.Type.LOGGED);
		for(String tag: tags) {
			stmt.add(recordingMetadataTable.deleteTag(recordingId, tag));
			stmt.add(placeRecordingIndex.deleteTag(placeId, recordingId, tag));
		}
		executeAndUpdateTimer(session, stmt, RemoveTagsTimer);
	}

	@Override
	public void complete(UUID placeId, UUID recordingId, double duration, long size, long ttlInSeconds) {
		long expiration = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		complete(placeId, recordingId, expiration, VideoV2Util.createActualTTL(recordingId, expiration), duration, size);
		
	}
	
	private void complete(UUID placeId, UUID recordingId, long expiration, long actualTtlInSeconds, double duration, long size) {
		BatchStatement stmt = new BatchStatement(Type.UNLOGGED); // recording will be atomic, and place_recording will be atomic, but they will be independently atomic to save performance
		stmt.setRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE);
		
		addDurationAndSizeStatements(stmt, placeId, recordingId, duration, size, expiration, actualTtlInSeconds);
		// Recording Metadata Index Mutations
		stmt.add(placeRecordingIndex.insertRecording(placeId, recordingId, size, expiration, actualTtlInSeconds));

		executeAndUpdateTimer(session, stmt, CompleteTimer);		
	}

	@Override
	public void completeAndDelete(UUID placeId, UUID recordingId, double duration, long size, Date purgeTime, int purgePartitionId, long ttlInSeconds) {
		BatchStatement stmt = new BatchStatement(Type.UNLOGGED); // recording will be atomic, and place_recording will be atomic, but they will be independently atomic to save performance
		stmt.setRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE);
		
		addDurationAndSizeStatements(stmt, placeId, recordingId, duration, size, ttlInSeconds);
		addDeleteStatements(stmt, placeId, recordingId, false, purgeTime, purgePartitionId);
		executeAndUpdateTimer(session, stmt, CompleteTimer);	
	}


	@Override
	public ListenableFuture<?> delete(UUID placeId, UUID recordingId, boolean isFavorite, Date purgeTime, int purgePartitionId) {
		
		BatchStatement stmt = new BatchStatement(Type.UNLOGGED);

		addDeleteStatements(stmt, placeId, recordingId, isFavorite, purgeTime, purgePartitionId);
		// Add to Purge table if it's favorite
		if(isFavorite) {
			VideoMetadata metadata = findByPlaceAndId(placeId, recordingId);
			metadata.setDeletionTime(purgeTime);
			metadata.setDeletionPartition(purgePartitionId);
			addStatementsForRemoveFromFavoriteTables(stmt, metadata);
		}

		long startTime = System.nanoTime();
		ResultSetFuture result = session.executeAsync(stmt);
		result.addListener(() -> DeleteTimer.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS), MoreExecutors.directExecutor());
		return result;
	}
	
	@Override
	public ListenableFuture<?> delete(VideoMetadata recording, Date purgeTime) {
		int purgePartitionId = recording.getDeletionPartition();
		if( purgePartitionId == VideoConstants.DELETION_PARTITION_UNKNOWN) {
			purgePartitionId = VideoDao.calculatePartitionId(recording.getRecordingId(), config.getPurgePartitions());
		}
		return delete(recording.getPlaceId(), recording.getRecordingId(), recording.isFavorite(), purgeTime, purgePartitionId) ;
	}


	@Override
	public VideoMetadata findByPlaceAndId(UUID placeId, UUID recordingId) {
		//look it in favorite table
		VideoMetadata datum = retrieveMetadataFrom(recordingMetadataFavoriteTable, recordingId, false);
		if(datum != null) {
			return datum;
		}else{
			//look in normal table
			datum = retrieveMetadataFrom(recordingMetadataTable, recordingId, true);
		}	
		return datum;
	}
	
	private VideoMetadata retrieveMetadataFrom(AbstractVideoMetadataV2Table table, UUID recordingId, boolean repair) {
		BoundStatement bs = table.selectRecording(recordingId);
		ResultSet rs = session.execute(bs);
		VideoMetadata datum = null;
		if(!rs.isExhausted()) {
			//found it in table
			datum = Iterables.getFirst( table.materialize(rs), null );
			if(repair) {
				repairIfNeeded(datum);
			}
		}
		return datum;
	}

	@Override
	public PagedResults<Map<String, Object>> query(VideoQuery query) {
		Preconditions.checkNotNull(query.getPlaceId(), "Must specify placeid");
		
		UUID start = start(query);
		UUID end = end(query);
		
		logger.debug("Querying recordings by: types: {} deleted: {} tags: {} cameras: {} in range [{} - {}] limit [{}]", query.getRecordingType(), query.isListDeleted(), query.getTags(), query.getCameras(), start, end, query.getLimit());
		Predicate<VideoMetadata> predicate = queryPredicate(query);
		Iterator<VideoRecordingSize> recordingIds = queryPlan(query, start, end);
		return query(query.getPlaceId(), start, end, query.getLimit(), recordingIds, predicate);
	}
	
	@Override
	public Stream<VideoRecordingSize> streamRecordingSizeAsc(UUID placeId, boolean includeFavorites, boolean includeInProgress) {
		Iterator<VideoRecordingSize> it = null;
		if(includeFavorites){		
			List<PeekingIterator<VideoRecordingSize>> idIterators = new ArrayList<>(2);
			idIterators.add( peekingUuidIterator( placeRecordingIndexFavorite.selectRecordingSizeAsc(placeId, PlaceRecordingIndexV2Table.Type.RECORDING), placeRecordingIndexFavorite::getRecordingIdAndSizeAndFavorite ) );
			idIterators.add( peekingUuidIterator( placeRecordingIndex.selectRecordingSizeAsc(placeId, PlaceRecordingIndexV2Table.Type.RECORDING), placeRecordingIndex::getRecordingIdAndSizeAndFavorite ) );
			it = new UnionIterator(idIterators);
		}else{
			Iterator<VideoRecordingSize> favorites = recordingIdsIterator(placeRecordingIndexFavorite.selectRecordingSizeAsc(placeId, PlaceRecordingIndexV2Table.Type.RECORDING), placeRecordingIndexFavorite::getRecordingIdAndSizeAndFavorite);
			Iterator<VideoRecordingSize> allRecordings = recordingIdsIterator(placeRecordingIndex.selectRecordingSizeAsc(placeId, PlaceRecordingIndexV2Table.Type.RECORDING), placeRecordingIndex::getRecordingIdAndSizeAndFavorite );
			it = new DifferenceIterator(allRecordings, favorites);
			
		}
		Spliterator<VideoRecordingSize> matches = Spliterators.spliteratorUnknownSize(it, Spliterator.DISTINCT | Spliterator.SORTED);
		
		Stream<VideoRecordingSize> stream = StreamSupport.stream(matches, false);
		if(!includeInProgress) {
			stream = stream.filter(VideoRecordingSize::isCompletedRecording);
		}
		return stream;
	}

	@Override
	public void purge(VideoMetadata metadata) {
		//This is a no op since relying on cassandra TTL
	}

	@Override
	public void purge(UUID placeId, UUID recordingId) {
		//This is a no op since relying on cassandra TTL
	}

	@Override
	@Deprecated
	public StorageUsed getUsedBytes(UUID placeId) {
		return ZeroStorageUsed;
	}
		
	@Override
	@Deprecated
	public StorageUsed incrementUsedBytes(UUID placeId, long bytes) {
		return ZeroStorageUsed;
	}

	@Override
	@Deprecated
	public StorageUsed incrementUsedBytes(UUID placeId, long bytes, StorageUsed storage) {
		return ZeroStorageUsed;
	}

	@Override
	@Deprecated
	public StorageUsed syncQuota(UUID placeId) {
		return ZeroStorageUsed;
	}

	@Override
	public VideoRecording getVideoRecordingById(UUID recordingId) {
		//look in favorite table
		VideoRecording rec = retrieveVideoRecordingFromTable(recordingFavoriteTable, recordingId);
		if(rec != null) {
			return rec;
		}else{
			//look in normal table
			rec = retrieveVideoRecordingFromTable(recordingTable, recordingId);
		}
		if(rec == null) {
			logger.warn("Failed to find video recording with id [{}]", recordingId);
			return null;
		}
		return rec;
	}
	
	private VideoRecording retrieveVideoRecordingFromTable(AbstractRecordingV2Table table, UUID recordingId) {
		BoundStatement select = table.select(recordingId);
		ResultSet rs = session.execute(select);
		if(rs.isExhausted()) {
			return null;
		}else{
			return VideoUtil.recMaterializeRecording(rs, recordingId);
		}
	}

	//Repair tables when metadata table indicating still in progress, but recording table indicating finished
	//TODO - Currently only the regular tables get repaired.  Is it possible favorite tables will have the same issue?
	private void repairIfNeeded(VideoMetadata metadata) {
		if(metadata != null && metadata.isInProgress()) {
			try {
				ResultSet rs = session.execute(recordingTable.select(metadata.getRecordingId()));
				if(rs.isExhausted()) {
					logger.warn("Can't load recording data for recording [{}]", metadata);
					return;
				}
				
				VideoRecording rec = VideoUtil.recMaterializeRecording( rs, metadata.getRecordingId() );
				if(rec.isRecordingFinished()) {
					logger.debug("Repairing completed recording [{}]", metadata.getRecordingId());
					metadata.setDuration(rec.duration);
					metadata.setSize(rec.size);
					if(!metadata.isStream()) {						
						complete(metadata.getPlaceId(), metadata.getRecordingId(), metadata.getExpiration(), VideoV2Util.createActualTTL(metadata.getRecordingId(), metadata.getExpiration()), rec.duration, rec.size);
					}
				}
			}
			catch(Exception e) {
				logger.warn("Unable to load recording data for [{}] in order to attempt repair", metadata.getRecordingId(), e);
			}
		}
		
	}

	

	private void addDurationAndSizeStatements(BatchStatement stmt, UUID placeId, UUID recordingId, double duration, long size, long ttlInSeconds) {
		long expiration = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		long actualTtlInSeconds = VideoV2Util.createActualTTL(recordingId, expiration);
		addDurationAndSizeStatements(stmt, placeId, recordingId, duration, size, expiration, actualTtlInSeconds);
	}
	
	private void addDurationAndSizeStatements(BatchStatement stmt, UUID placeId, UUID recordingId, double duration, long size, long expiration, long actualTtlInSeconds) {
		// Recording Metadata Table Mutations
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSeconds, MetadataAttribute.DURATION, String.valueOf(duration)));
		stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSeconds, MetadataAttribute.SIZE, String.valueOf(size)));

		// Recording Table Mutations
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSeconds, RecordingTableField.DURATION, toblob(duration)));
		stmt.add(recordingTable.insertField(recordingId, expiration, actualTtlInSeconds, RecordingTableField.SIZE, toblob(size)));

	}

	private void addDeleteStatements(BatchStatement stmt, UUID placeId, UUID recordingId, boolean isFavorite, Date purgeTime, int purgePartitionId) {
		boolean expired = false;
		long expiration = 0;
		if(isFavorite) {
			//check to see if data in the normal table has expired
			BoundStatement bs = recordingMetadataTable.selectRecording(recordingId);
			ResultSet rs = session.execute(bs);
			if(!rs.isExhausted()) {
				expiration = rs.one().getLong(VideoMetadataV2Table.COL_EXPIRATION);
			}else{
				expired = true;
			}
		}
		if(!expired) {					
			long actualTtlInSeconds = VideoV2Util.createActualTTL(recordingId, expiration);		
			// Recording metadata Table Mutations
			stmt.add(recordingMetadataTable.insertField(recordingId, expiration, actualTtlInSeconds, MetadataAttribute.DELETED, Boolean.TRUE.toString()));
			// Recording Place Index Mutations
			stmt.add(placeRecordingIndex.insertDeleted(placeId, recordingId, expiration, actualTtlInSeconds));
			stmt.add(placeRecordingIndex.deleteVideo(placeId, recordingId, PlaceRecordingIndexV2Table.Type.RECORDING));
			stmt.add(placeRecordingIndex.deleteVideo(placeId, recordingId, PlaceRecordingIndexV2Table.Type.STREAM));					
		}

	}
	
	

	@SuppressWarnings("unchecked")
	static Iterator<UUID> intersection(Iterator<UUID>... iterators) {
		List<PeekingIterator<UUID>> filters = new ArrayList<>(iterators.length);
		for(Iterator<UUID> iterator: iterators) {
			if(iterator != null) {
				filters.add(Iterators.peekingIterator(iterator));
			}
		}
		if(filters.size() > 1) {
			return new IntersectionIterator(filters);
		}
		else {
			return filters.get(0);
		}
	}	
	
	
	
	private PagedResults<Map<String, Object>> query(UUID placeId, UUID start, UUID end, int limit, Iterator<VideoRecordingSize> recordingIds, Predicate<VideoMetadata> predicate) {
		List<Map<String, Object>> recordings = new ArrayList<>(limit + 1);
		VideoMetadata recording = null;
		while(recordings.size() <= limit && recordingIds.hasNext()) {
			VideoRecordingSize recordingId = recordingIds.next();
			recording = fetchVideoMetadata(recordingId);
			if(recording != null && predicate.apply(recording)) {
				repairIfNeeded(recording);
				recordings.add(recording.toMap());
			}
		}
		if(recordings.size() > limit) {
			Map<String, Object> last = recordings.remove(limit);
			return PagedResults.newPage(recordings, String.valueOf(last.get(Capability.ATTR_ID)));
		}
		else {
			return PagedResults.newPage(recordings);
		}
	}
	
	private VideoMetadata fetchVideoMetadata(VideoRecordingSize r) {
		VideoMetadata recording = null;
		ResultSet rs = null;
		if(r.isFavorite()) {
			rs = session.execute( recordingMetadataFavoriteTable.selectRecording(r.getRecordingId()) );
			recording = Iterables.getFirst( recordingMetadataFavoriteTable.materialize(rs), null );
		}else{
			rs = session.execute( recordingMetadataTable.selectRecording(r.getRecordingId()) );
			recording = Iterables.getFirst( recordingMetadataTable.materialize(rs), null );
		}
		if(recording == null) {
			logger.debug("Invalid index for recording [{}]", r);
		}
		return recording;
	}
	
	private VideoMetadata fetchVideoMetadata(VideoRecordingSize r, Predicate<VideoMetadata> predicate) {
		VideoMetadata recording = fetchVideoMetadata(r);
		if(recording != null && predicate.apply(recording)) {
			return recording;
		}else{
			return null;
		}
	}
	
	
	private <K> Iterator<VideoRecordingSize> querySet(Set<K> matchers, Function<K, List<Pair<BoundStatement, Function<Row, VideoRecordingSize>>>> queries) {
		if(matchers == null || matchers.isEmpty()) {
			return null;
		}
		
		if(matchers.size() == 1) {			
			List<Pair<BoundStatement, Function<Row, VideoRecordingSize>>> bsList = queries.apply(matchers.iterator().next());
			if(bsList.size() == 1) {
				Pair<BoundStatement, Function<Row, VideoRecordingSize>> cur = bsList.get(0);
				return recordingIdsIterator(cur.getLeft(), cur.getRight());
			}			
		}		
		List<PeekingIterator<VideoRecordingSize>> iterators = new ArrayList<>();			
		for(K matcher: matchers) {
			List<Pair<BoundStatement, Function<Row, VideoRecordingSize>>> bsList = queries.apply(matcher);
			for(Pair<BoundStatement, Function<Row, VideoRecordingSize>>bs: bsList) {
				Iterator<VideoRecordingSize> iterator = recordingIdsIterator(bs.getLeft(), bs.getRight());
				iterators.add(Iterators.peekingIterator(iterator));
			}			
		}
		return new UnionIterator(iterators);
	}
	

	private Iterator<VideoRecordingSize> queryPlan(VideoQuery query, UUID start, UUID end) {
		// choose the most specific index and then apply additional filters in memory
		
		// there should be at most one per camera, so this should be the most specific index
		if(query.getRecordingType() == VideoType.STREAM) {
			return recordingIdsIterator( placeRecordingIndex.selectIdsByType(query.getPlaceId(), PlaceRecordingIndexV2Table.Type.STREAM, start, end, query.getLimit() + 1), placeRecordingIndex::getRecordingIdAndFavorite );
		}
		
		if(query.getTags() != null && !query.getTags().isEmpty()) {
			return querySet(query.getTags(), (tag) -> addStatementToList(
							placeRecordingIndexFavorite.selectIdsByTag(query.getPlaceId(), tag, start, end, query.getLimit() + 1), 
							placeRecordingIndexFavorite::getRecordingIdAndFavorite,
							placeRecordingIndex.selectIdsByTag(query.getPlaceId(), tag, start, end, query.getLimit() + 1),
							placeRecordingIndex::getRecordingIdAndFavorite));
		}
		
		if(query.getCameras() != null && !query.getCameras().isEmpty()) {
			return querySet(query.getCameras(), (cameraId) -> addStatementToList(
						placeRecordingIndexFavorite.selectIdsByCamera(query.getPlaceId(), cameraId, start, end, query.getLimit() + 1), 
						placeRecordingIndexFavorite::getRecordingIdAndFavorite,
						placeRecordingIndex.selectIdsByCamera(query.getPlaceId(), cameraId, start, end, query.getLimit() + 1),
						placeRecordingIndex::getRecordingIdAndFavorite));
		}
		
		List<PeekingIterator<VideoRecordingSize>> typeIterators = new ArrayList<>(5);
		// type is either ANY or RECORDING, already handled stream above
		typeIterators.add( peekingUuidIterator( placeRecordingIndexFavorite.selectIdsByType(query.getPlaceId(), PlaceRecordingIndexV2Table.Type.RECORDING, start, end, query.getLimit() + 1), placeRecordingIndexFavorite::getRecordingIdAndFavorite ) );
		typeIterators.add( peekingUuidIterator( placeRecordingIndex.selectIdsByType(query.getPlaceId(), PlaceRecordingIndexV2Table.Type.RECORDING, start, end, query.getLimit() + 1), placeRecordingIndex::getRecordingIdAndFavorite ) );
		if(query.getRecordingType() == VideoType.ANY) {
			typeIterators.add( peekingUuidIterator( placeRecordingIndex.selectIdsByType(query.getPlaceId(), PlaceRecordingIndexV2Table.Type.STREAM, start, end, query.getLimit() + 1), placeRecordingIndex::getRecordingIdAndFavorite ) );
		}
		if(query.isListDeleted()) {
			typeIterators.add( peekingUuidIterator( placeRecordingIndexFavorite.selectIdsByDeleted(query.getPlaceId(), start, end, query.getLimit() + 1), placeRecordingIndexFavorite::getRecordingIdAndFavorite ) );
			typeIterators.add( peekingUuidIterator( placeRecordingIndex.selectIdsByDeleted(query.getPlaceId(), start, end, query.getLimit() + 1), placeRecordingIndex::getRecordingIdAndFavorite ) );
		}
		return new UnionIterator( typeIterators );
	}

	List<Pair<BoundStatement, Function<Row, VideoRecordingSize>>> addStatementToList(BoundStatement stmt, Function<Row, VideoRecordingSize> function) {
		return ImmutableList.<Pair<BoundStatement, Function<Row, VideoRecordingSize>>>of(new ImmutablePair<BoundStatement, Function<Row, VideoRecordingSize>>(stmt, function));
	}
	
	List<Pair<BoundStatement, Function<Row, VideoRecordingSize>>> addStatementToList(BoundStatement stmt1, Function<Row, VideoRecordingSize> function1, BoundStatement stmt2, Function<Row, VideoRecordingSize> function2) {
		return ImmutableList.<Pair<BoundStatement, Function<Row, VideoRecordingSize>>>of(new ImmutablePair<BoundStatement, Function<Row, VideoRecordingSize>>(stmt1, function1), new ImmutablePair<BoundStatement, Function<Row, VideoRecordingSize>>(stmt2, function2));
	}
	
	
	private Predicate<VideoMetadata> queryPredicate(VideoQuery query) {
		List<Predicate<VideoMetadata>> predicates = new ArrayList<>();
		predicates.add((m) -> m != null);
		
		Set<UUID> cameraIds = toUuidSet(query.getCameras());
		if(!cameraIds.isEmpty()) {
			predicates.add((m) -> cameraIds.contains(m.getCameraId()));
		}
		if(!query.getTags().isEmpty()) {
			predicates.add((m) -> CollectionUtils.containsAny(query.getTags(), m.getTags()));
		}
		if(!query.isListDeleted()) {
			predicates.add((m) -> !m.isDeleted());
		}
		if(!query.isListInProgress()) {
			predicates.add((m) -> !m.isInProgress());
		}
		if(query.getRecordingType() == VideoType.STREAM) {
			predicates.add((m) -> m.isStream());
		}
		else if(query.getRecordingType() == VideoType.RECORDING) {
			predicates.add((m) -> !m.isStream());
		}
		return Predicates.and(predicates);
	}

	private Set<UUID> toUuidSet(Set<String> cameras) {
		if(CollectionUtils.isEmpty(cameras)) {
			return ImmutableSet.of();
		}
		
		Set<UUID> ids = new HashSet<>(cameras.size());
		for(String camera: cameras) {
			ids.add(UUID.fromString(camera));
		}
		return ids;
	}

	private Iterator<VideoRecordingSize> recordingIdsIterator(BoundStatement bs, Function<Row, VideoRecordingSize> function) {
		ResultSet rs = session.execute(bs);
		return Iterators.transform(rs.iterator(), row -> function.apply(row));
	}
	

	private PeekingIterator<VideoRecordingSize> peekingUuidIterator(BoundStatement bs, Function<Row, VideoRecordingSize> function) {
		return Iterators.peekingIterator( recordingIdsIterator(bs, function) );
	}


	private UUID start(VideoQuery query) {
		Date startTime = query.getLatest();
		UUID token = StringUtils.isEmpty(query.getToken()) ? null : UUID.fromString(query.getToken());
		if(startTime == null && token == null) {
			return IrisUUID.maxTimeUUID();
		}
		else if(startTime == null) {
			return token;
		}
		else if(token == null || startTime.getTime() < IrisUUID.timeof(token)) {
			return IrisUUID.timeUUID(startTime.getTime(), Long.MAX_VALUE);
		}
		else {
			return token;
		}
	}

	private UUID end(VideoQuery query) {
		return query.getEarliest() == null ? IrisUUID.minTimeUUID() : IrisUUID.timeUUID(query.getEarliest(), 0);
	}
	
	private static int compare(UUID id1, UUID id2) {
		return IrisUUID.descTimeUUIDComparator().compare(id1, id2);		
	}
	
	private void addFavoriteTags(UUID placeId, UUID recordingId, Set<String> tags, long ttlInSeconds) {
		//Get metadata and copy from recording_metadata_v2 to recording_metadata_v2_favorite
		BatchStatement insertStmt = new BatchStatement(Type.UNLOGGED);
		ArrayList<Statement> placeRecordingIndexFavoriteInserts = new ArrayList<Statement>();
		boolean deleted = false;
		BoundStatement metadataSelect = recordingMetadataTable.selectRecording(recordingId);
		Iterator<Row> metadataRs = session.execute(metadataSelect).iterator();
		Row curRow = null;
		Date purgeTime = null;
		int partitionId = 0;
		while(metadataRs.hasNext()) {
			curRow = metadataRs.next();
			MetadataAttribute curField = MetadataAttribute.valueOf(curRow.getString(VideoMetadataV2Table.COL_FIELD).toUpperCase());
			String curValue = curRow.getString(VideoMetadataV2Table.COL_VALUE);
			//copy the current row to the favorite table
			insertStmt.add(recordingMetadataFavoriteTable.insertField(recordingId, curField, curValue));
			//reconstruct placeRecordingIndexV2 table from metadata because query by recordingId alone for place_recording_index_v2 is not possible
			if(MetadataAttribute.CAMERAID.equals(curField)) {
				placeRecordingIndexFavoriteInserts.add(placeRecordingIndexFavorite.insertCamera(placeId, recordingId, curValue));
			}else if(MetadataAttribute.TYPE.equals(curField)) {
				PlaceRecordingIndexV2Table.Type curType = PlaceRecordingIndexV2Table.Type.RECORDING;
				if(VideoMetadataV2Table.ATTR_TYPE_STREAM.equals(curValue)) {
					curType = PlaceRecordingIndexV2Table.Type.STREAM;
				}
				placeRecordingIndexFavoriteInserts.add(placeRecordingIndexFavorite.insertVideo(placeId, recordingId, curType));
			}else if(MetadataAttribute.SIZE.equals(curField)) {
				placeRecordingIndexFavoriteInserts.add(placeRecordingIndexFavorite.insertRecording(placeId, recordingId, Long.valueOf(curValue)));
			}else if(MetadataAttribute.DELETED_TIME.equals(curField)) {
				purgeTime = new Date(Long.valueOf(curValue));
				if(purgeTime.before(new Date())) {
					deleted = true;
					logger.warn("can not add favorite tag to a deleted video [{}] for place [{}]", recordingId, placeId);					
				}
			}else if(MetadataAttribute.DELETED_PARTITION.equals(curField)) {
				partitionId = Integer.valueOf(curValue);
			}
		}
		
		if(!deleted) {
			insertStmt.addAll(placeRecordingIndexFavoriteInserts);
			//add the statements for the new tags
			for(String tag: tags) {
				insertStmt.add(recordingMetadataFavoriteTable.insertTag(recordingId, tag));
				insertStmt.add(placeRecordingIndexFavorite.insertTag(placeId, recordingId, tag));
			}
			//add the statements for recording
			BoundStatement recordingSelect = recordingTable.select(recordingId);
			session.execute(recordingSelect).forEach(curRecordingRow -> {
				double ts = curRecordingRow.getDouble(RecordingV2Table.COL_TS);
	         long bo = curRecordingRow.getLong(RecordingV2Table.COL_BO);
	         ByteBuffer bl = curRecordingRow.getBytes(RecordingV2Table.COL_BL);
	         insertStmt.add(recordingFavoriteTable.insertIFrame(recordingId, ts, bo, bl));
			});
			//add the statements for removing from purge table
			insertStmt.add(purgeTable.deletePurgeEntry(purgeTime, partitionId, recordingId));
			VideoV2Util.executeBatchWithLimit(session, insertStmt, config.getCreateFavoriteVideoBatchSize(), InsertFavoriteVideoTimer);			
		}
		
	}
	
	

	private void addNonFavoriteTags(UUID placeId, UUID recordingId, Set<String> tags, long ttlInSeconds) {
		BatchStatement stmt = new BatchStatement(BatchStatement.Type.LOGGED);	
		long expiration = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		long actualTtlInSeconds = VideoV2Util.createActualTTL(recordingId, expiration);
		for(String tag: tags) {
			stmt.add(recordingMetadataTable.insertTag(recordingId, expiration, actualTtlInSeconds, tag));
			stmt.add(placeRecordingIndex.insertTag(placeId, recordingId, expiration, actualTtlInSeconds, tag));
		}
		executeAndUpdateTimer(session, stmt, AddTagsTimer);
	}
	
	private void addPurgeStatements(BatchStatement stmt, UUID placeId, UUID recordingId, Date purgeTime, int purgePartitionId, String fileLocation, boolean purgePreview) {
		stmt.add(purgeTable.insertPurgeEntry(purgeTime, purgePartitionId, recordingId, placeId, fileLocation, purgePreview));
		stmt.add(purgeTable.insertPurgeAt(purgeTime, purgePartitionId));
	}

	/**
	 * Combines a collection of sorted iterators so that each value
	 * is returned once and only once, maintaining ordering.
	 * @author tweidlin
	 *
	 */
	public static class UnionIterator implements Iterator<VideoRecordingSize> {
		private VideoRecordingSize currentId;
		private Collection<PeekingIterator<VideoRecordingSize>> iterators;
		
		public UnionIterator(Collection<PeekingIterator<VideoRecordingSize>> iterators) {
			this.iterators = iterators;
			this.advance();
		}
		
		private void advance() {
			VideoRecordingSize latestTs = new VideoRecordingSize(IrisUUID.minTimeUUID(), false);
			Iterator<VideoRecordingSize> nextIt = null; 
			// find the largest value
			for(PeekingIterator<VideoRecordingSize> it: iterators) {
				if(!it.hasNext()) {
					continue;
				}
				
				if(currentId != null && (currentId.equals(it.peek()) || (currentId.getRecordingId().equals(it.peek().getRecordingId()) && !it.peek().isFavorite()))) {
					// throw away duplicate entry when the next record's recordingId is the same as currentId and not favorite
					it.next();
					if(!it.hasNext()) {
						continue;
					}
				}
				int comp = compare(latestTs.getRecordingId(), it.peek().getRecordingId());
				if(comp > 0){
					latestTs = it.peek(); //Set it because recordingId is larger 
					nextIt = it;
				}else if(comp == 0) {
					if(latestTs.isFavorite() || !it.peek().isFavorite()) {
						it.next(); //skip
					}else {
						latestTs = it.peek(); //Set it because the next one is favorite and the current on is not 
						nextIt = it;
					}
				}
			}
			if(nextIt == null) {
				currentId = null;
			}
			else {
				currentId = nextIt.next();
			}
		}
		
		@Override
		public boolean hasNext() {
			return currentId != null;
		}
		
		@Override
		public VideoRecordingSize next() {
			if(currentId == null) {
				throw new NoSuchElementException();
			}
			else {
				VideoRecordingSize nextId = currentId;
				advance();
				return nextId;
			}
		}
	}

	/**
	 * Combines a collection of sorted iterators so that each value
	 * that exists in _every_ delegate iterator is returned once and only once, 
	 * maintaining ordering.
	 * @author tweidlin
	 *
	 */
	private static class IntersectionIterator implements Iterator<UUID> {
		private UUID currentId;
		private Collection<PeekingIterator<UUID>> iterators;
		
		public IntersectionIterator(Collection<PeekingIterator<UUID>> iterators) {
			this.iterators = iterators;
			this.advance();
		}
		
		private void advance() {
			currentId = null;
			
			UUID nextId = null;
			int matches = 0;
			while(matches < iterators.size()) {
				matches = 0;
				for(PeekingIterator<UUID> it: iterators) {
					while(nextId != null && it.hasNext() && compare(nextId, it.peek()) > 0) {
						// fast forward to where the current id is
						it.next();
					}
					if(!it.hasNext()) {
						// since its an intersection if any iterator is done, the whole thing is done
						return;
					}
					else if(nextId == null || it.peek().equals(nextId)) {
						// advance the iterator if it matches the current id
						nextId = it.next();
						matches++;
					}
					else if(nextId != null && compare(nextId, it.peek()) < 0) {
						// if this iterator is farther along then the others, reset nextId and start the loop over
						nextId = it.peek();
						break;
					}
				}
			}
			currentId = nextId;
		}
		
		@Override
		public boolean hasNext() {
			return currentId != null;
		}
		
		@Override
		public UUID next() {
			if(currentId == null) {
				throw new NoSuchElementException();
			}
			else {
				UUID nextId = currentId;
				advance();
				return nextId;
			}
		}
	}
	
	private static class DifferenceIterator implements Iterator<VideoRecordingSize> {
		private VideoRecordingSize next;
		private UUID nextToSkip;
		private Iterator<VideoRecordingSize> delegate;
		private Iterator<VideoRecordingSize> subtract;
		
		public DifferenceIterator(Iterator<VideoRecordingSize> delegate, Iterator<VideoRecordingSize> subtract) {
			this.delegate = delegate;
			this.subtract = subtract;
			this.nextToSkip = nextToSkip();
			this.advance();
		}
		
		private UUID nextToSkip() {
			return subtract.hasNext() ? subtract.next().getRecordingId() : null;
		}
		
		private void advance() {
			next = null;
			
			while(delegate.hasNext()) {
				VideoRecordingSize row = delegate.next();
				UUID id = row.getRecordingId();
				int comp = nextToSkip == null ? 1 : compare(id, nextToSkip);
				while(comp < 0 && subtract.hasNext()) {
					nextToSkip = nextToSkip();
					comp = compare(id, nextToSkip);
				}
				if(comp == 0) {
					// if the current row matches the next row to skip,
					// drop the current row and advance the subtraction iterator
					nextToSkip = nextToSkip();
				}
				else {
					// if the current row isn't the next one to skip,
					// then let it through and drop out of the loop
					next = row;
					break;
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return next != null;
		}
		
		@Override
		public VideoRecordingSize next() {
			if(next == null) {
				throw new NoSuchElementException();
			}
			else {
				VideoRecordingSize current = next;
				advance();
				return current;
			}
		}
	}

	@Override
	public long countByTag(UUID placeId, String tag) {
		if(VideoConstants.TAG_FAVORITE.equals(tag)) {
			BoundStatement stmt = placeRecordingIndexFavorite.countByField(placeId, Field.TAG, tag);
			return session.execute(stmt).one().getLong(0);
		}else{
			//TODO - need to merge favorite with the normal.  Figuring out what are the duplicates is pretty tricky.
			BoundStatement stmt = placeRecordingIndexFavorite.countByField(placeId, Field.TAG, tag);
			return session.execute(stmt).one().getLong(0);
		}
		
	}

	@Override
	public void addToPurgePinnedRecording(UUID placeId, Date deleteTime) {
		//round deleteTime to the beginning of the next day
		BoundStatement stmt = purgePinnedRecordingTable.insert(VideoV2Util.getStartOfNextDay(deleteTime), placeId, PurgeMode.PINNED);
		session.execute(stmt);		
	}
	
	@Override
	public void addToPurgeAllRecording(UUID placeId, Date deleteTime) {
		//round deleteTime to the beginning of the next day
		BoundStatement stmt = purgePinnedRecordingTable.insert(VideoV2Util.getStartOfNextDay(deleteTime), placeId, PurgeMode.ALL);
		session.execute(stmt);		
	}
	
	@Override
	public List<PlacePurgeRecord> getPlacePurgeRecordingNoLaterThan(Date deleteTime) {
		DateTime d = new DateTime(deleteTime.getTime());
		d = d.withTimeAtStartOfDay();
		List<PlacePurgeRecord> records = new ArrayList<>();
		BoundStatement stmt;
		Iterator<Row> rs;
		for(int i=0; i<config.getPlacePurgeRecordingNumberOfDays(); i++) {
			stmt = purgePinnedRecordingTable.selectBy(d.toDate());
			rs = session.execute(stmt).iterator();
			while(rs.hasNext()) {
				records.add(purgePinnedRecordingTable.buildEntity(rs.next()));				
			}
			d = d.minusDays(1);
		}
		return records;
	}
	
	@Override
	public void deletePurgePinnedRecordingNoLaterThan(Date deleteTime) {
		DateTime d = new DateTime(deleteTime.getTime());
		d = d.withTimeAtStartOfDay();
		BoundStatement stmt;
		for(int i=0; i<config.getPlacePurgeRecordingNumberOfDays(); i++) {
			stmt = purgePinnedRecordingTable.deleteBy(d.toDate());
			session.execute(stmt);			
			d = d.minusDays(1);
		}
		
	}

	@Override
	public Stream<VideoMetadata> streamVideoMetadata(VideoQuery query) {
		Preconditions.checkNotNull(query.getPlaceId(), "Must specify placeid");
		
		UUID start = start(query);
		UUID end = end(query);
		
		logger.debug("Querying recordings by: types: {} deleted: {} tags: {} cameras: {} in range [{} - {}] limit [{}]", query.getRecordingType(), query.isListDeleted(), query.getTags(), query.getCameras(), start, end, query.getLimit());
		Predicate<VideoMetadata> predicate = queryPredicate(query);
		Iterator<VideoRecordingSize> recordingIds = queryPlan(query, start, end);
		Iterator<VideoMetadata> result = Iterators.transform(recordingIds, (r) -> fetchVideoMetadata(r, predicate));
      Spliterator<VideoMetadata> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      return StreamSupport.stream(stream, false);
		
	}

	
	
	
}

