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

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.inject.Inject;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.AudioCodec;
import com.iris.video.VideoCodec;
import com.iris.video.VideoMetadata;
import com.iris.video.cql.MetadataAttribute;
import com.iris.video.cql.VideoConstants;
import com.iris.video.cql.VideoTable;

public abstract class AbstractVideoMetadataV2Table extends VideoTable{
	private static final Logger logger = LoggerFactory.getLogger(AbstractVideoMetadataV2Table.class);


	public static final String COL_RECORDINGID = "recordingid";
	public static final String COL_FIELD = "field";
	public static final String COL_VALUE = "value";	
	
	
	
	public static final String ATTR_TYPE_STREAM = "s";
	public static final String ATTR_TYPE_RECORDING = "r";
	
	protected final PreparedStatement selectByRecordingId;
	protected final PreparedStatement deleteRecording;
	
	@Inject
	public AbstractVideoMetadataV2Table(String ts, Session session) {
		super(ts, session);
		this.selectByRecordingId =
				CassandraQueryBuilder
					.select(getTableName())
					.addColumns(getTableColumns())
					.addWhereColumnEquals(COL_RECORDINGID)
					.prepare(session);
		this.deleteRecording =
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_RECORDINGID)
					.withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))
					.prepare(session);
	}
	
	abstract protected String[] getTableColumns() ;

	

	public BoundStatement selectRecording(UUID recordingId) {
		return selectByRecordingId.bind(recordingId);
	}	


	/**
	 * Deletes all fields associated with the recording
	 * @param recordingId
	 * @return
	 */
	public Statement deleteRecording(UUID recordingId) {
		return deleteRecording.bind(recordingId);
	}
	
	public Iterable<VideoMetadata> materialize(ResultSet rs) {
		return () -> new MetadataIterator(rs);
	}

	private void accumulate(Row row, VideoMetadata metadata) {
		String field = row.getString(COL_FIELD);
		try {
			if(field.startsWith(VideoConstants.ATTR_TAG_PREFIX)) {
				metadata.addTag(field.substring(VideoConstants.ATTR_TAG_PREFIX.length()));
			}
			else {
				MetadataAttribute attribute = MetadataAttribute.valueOf(field.toUpperCase());
				String value = row.getString(COL_VALUE);
				if(StringUtils.isEmpty(value)) {
					return;
				}
				
				switch(attribute) {
				case ACCOUNTID:
					metadata.setAccountId(UUID.fromString(value));
					break;
					
				case BANDWIDTH:
					metadata.setBandwidth(Integer.valueOf(value));
					break;
					
				case CAMERAID:
					metadata.setCameraId(UUID.fromString(value));
					break;
					
				case DELETED_PARTITION:
					metadata.setDeletionPartition(Integer.valueOf(value));
					break;
				
				case DELETED_TIME:
					metadata.setDeletionTime(new Date(Long.valueOf(value)));
					break;
				case DELETED:
					metadata.setDeleted(Boolean.valueOf(value));
					break;	
				case DURATION:
					metadata.setDuration(Double.valueOf(value));
					break;
					
				case FRAMERATE:
					metadata.setFramerate(Double.valueOf(value));
					break;
					
				case HEIGHT:
					metadata.setHeight(Integer.valueOf(value));
					break;
					
				case IMG:
					// FIXME add image
					break;
					
				case LOCATION:
					metadata.setLoc(value);
					break;
					
				case NAME:
					metadata.setName(value);
					break;
					
				case PERSONID:
					metadata.setPersonId(UUID.fromString(value));
					break;
					
				case PLACEID:
					metadata.setPlaceId(UUID.fromString(value));
					break;
				
				case PRECAPTURE:
					metadata.setPrecapture(Double.valueOf(value));
					break;
					
				case SIZE:
					metadata.setSize(Long.valueOf(value));
					break;
					
				case TYPE:
					metadata.setStream(ATTR_TYPE_STREAM.equals(value));
					break;
					
				case WIDTH:
					metadata.setWidth(Integer.valueOf(value));
					break;
				case VIDEO_CODEC:
					metadata.setVideoCodec(VideoCodec.valueOf(value));
					break;
				case AUDIO_CODEC:
					metadata.setAudioCodec(AudioCodec.valueOf(value));
					break;
				case EXPIRATION:
					metadata.setExpiration( Long.valueOf(value)) ;					
					break;
				default:
					logger.warn("Unrecognized attribute [{}]", attribute);
				}
			}
		}
		catch(IllegalArgumentException e) {
			logger.warn("Unrecognized field [{}]", field, e);
		}
	}
	
	private class MetadataIterator implements Iterator<VideoMetadata> {
		private PeekingIterator<Row> delegate;
		
		MetadataIterator(ResultSet rs) {
			this.delegate = Iterators.peekingIterator(rs.iterator());
		}

		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@Override
		public VideoMetadata next() {
			VideoMetadata metadata = new VideoMetadata();
			Row first = delegate.next();
			metadata.setRecordingId(first.getUUID(COL_RECORDINGID));
			accumulate(first, metadata);
			while(delegate.hasNext() && metadata.getRecordingId().equals( delegate.peek().getUUID(COL_RECORDINGID) )) {
				accumulate(delegate.next(), metadata);
			}
			if(metadata.getVideoCodec() == null) {
				metadata.setVideoCodec(VideoCodec.H264_BASELINE_3_1);
			}
			if(metadata.getAudioCodec() == null) {
				metadata.setAudioCodec(AudioCodec.NONE);
			}
			if(metadata.getDeletionTime() == null) {
				metadata.setDeletionTime(VideoConstants.DELETE_TIME_SENTINEL);
			}
			return metadata;
		}

	}

}

