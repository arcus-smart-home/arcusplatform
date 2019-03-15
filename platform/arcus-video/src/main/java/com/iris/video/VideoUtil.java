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
package com.iris.video;

import static com.iris.video.VideoMetrics.VIDEO_BAD_FRAME;
import static com.iris.video.VideoMetrics.VIDEO_DOESNT_EXIST;
import static com.iris.video.VideoMetrics.VIDEO_NO_ACCOUNT;
import static com.iris.video.VideoMetrics.VIDEO_NO_CAMERA;
import static com.iris.video.VideoMetrics.VIDEO_NO_PLACE;
import static com.iris.video.VideoMetrics.VIDEO_NO_STORAGE;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.Nullable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.service.VideoService;
import com.iris.util.IrisUUID;
import com.iris.video.cql.RecordingTableField;
import com.iris.video.cql.VideoConstants;
import com.iris.video.cql.v2.AbstractPlaceRecordingIndexV2Table;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

public final class VideoUtil {
	private static final Logger logger = LoggerFactory.getLogger(VideoUtil.class);
   private static final long UUID_MAC_ULBIT = 0x0000020000000000L;

   public static final String TAG_BACKFILLED = "BACKFILL";
   public static final String TAG_V2         = "V2";
   
   public static final Address SERVICE_ADDRESS = Address.platformService(VideoService.NAMESPACE);
   
   private VideoUtil() {
   }

   @Nullable
   public static Address getActorFromPersonId(UUID placeId, @Nullable UUID personId) {
      if(personId == null) {
         return null;
      }
      if(IrisUUID.isTime(personId)) { // time uuid means alarm incident
         return Address.platformService(personId, AlarmIncidentCapability.NAMESPACE);
      }
      else if(personId.getMostSignificantBits() == 0) { // MSB of 0 means rule
         return Address.platformService(placeId, RuleCapability.NAMESPACE, (int) personId.getLeastSignificantBits());
      }
      else if(personId.getMostSignificantBits() == 1) { // MSB of 1 means scene
         return Address.platformService(placeId, SceneCapability.NAMESPACE, (int) personId.getLeastSignificantBits());
      }
      else { // everything else is a person
         return Address.platformService(personId, PersonCapability.NAMESPACE);
      }
   }
   
   public static UUID timeUUIDForRecording() {
      UUID original = IrisUUID.timeUUID();

      long lsb = original.getLeastSignificantBits() & ~UUID_MAC_ULBIT;
      return new UUID(original.getMostSignificantBits(), lsb);
   }

   public static UUID timeUUIDForStream() {
      UUID original = IrisUUID.timeUUID();

      long lsb = original.getLeastSignificantBits() | UUID_MAC_ULBIT;
      return new UUID(original.getMostSignificantBits(), lsb);
   }

   public static boolean isStreamUUID(UUID uuid) {
      return (uuid.getLeastSignificantBits() & UUID_MAC_ULBIT) != 0;
   }

   public static String getHlsUri(String videoStreamUrl, UUID recordingId, String exp, String sig) {
      QueryStringEncoder enc = new QueryStringEncoder(videoStreamUrl + "/hls/" + recordingId + "/playlist.m3u8");
      enc.addParam("exp", exp);
      enc.addParam("sig", sig);
      return enc.toString();
   }

   public static String getJpgUri(String videoStreamUrl, UUID recordingId, String exp, String sig) {
      QueryStringEncoder enc = new QueryStringEncoder(videoStreamUrl + "/jpg/" + recordingId + "/preview.jpg");
      enc.addParam("exp", exp);
      enc.addParam("sig", sig);
      return enc.toString();
   }

   public static String getMp4Uri(String videoDownloadUrl, UUID recordingId, String exp, String sig) {
      QueryStringEncoder enc = new QueryStringEncoder(videoDownloadUrl + "/mp4/" + recordingId + ".mp4");
      enc.addParam("exp", exp);
      enc.addParam("sig", sig);
      return enc.toString();
   }

   public static long getQuotaForPlace(Place plc, long premiumQuota) {
      long total = 0L;
      if(ServiceLevel.isPremiumOrPromon(plc.getServiceLevel())){
         total += premiumQuota;
      }

      for (String addon : plc.getServiceAddons()) {
         if (addon == null || addon.isEmpty()) continue;
         if (!addon.startsWith("VIDEO")) continue;

         String amount = addon.substring("VIDEO".length());

         long mult = 1;
         if (amount.endsWith("TB")) {
            mult = 1024L * 1024L * 1024L * 1024L;
            amount = amount.substring(0, amount.length() - 2);
         } else if (amount.endsWith("GB")) {
            mult = 1024L * 1024L * 1024L;
            amount = amount.substring(0, amount.length() - 2);
         } else if (amount.endsWith("MB")) {
            mult = 1024L * 1024L;
            amount = amount.substring(0, amount.length() - 2);
         } else if (amount.endsWith("KB")) {
            mult = 1024L;
            amount = amount.substring(0, amount.length() - 2);
         }

         try {
            long quotaAddon = Long.parseLong(amount);
            total += quotaAddon * mult;
         } catch (Exception ex) {
            // ignore
         }
      }

      return total;
   }

   public static Date getPurgeTimestamp(long delay, TimeUnit unit) {
   	return getPurgeTimestamp(System.currentTimeMillis(), delay, unit);
   }
   
   public static Date getPurgeTimestamp(long startTime, long delay, TimeUnit unit) {
      long ms = TimeUnit.MILLISECONDS.convert(delay, unit);

      DateTime purgeTime = new DateTime(startTime + ms);
      if (purgeTime.getMinuteOfHour() != 0 || purgeTime.getSecondOfMinute() != 0 || purgeTime.getMillisOfSecond() != 0) {
         purgeTime = purgeTime.plusHours(1);
      }

      return purgeTime.withTime(purgeTime.getHourOfDay(), 0, 0, 0).toDate();
   }


   private static String doBase64Encode(UUID uuid1, UUID uuid2) {
      try {
         ByteBuffer buffer = ByteBuffer.allocate(32);
         buffer.putLong(uuid1.getMostSignificantBits());
         buffer.putLong(uuid1.getLeastSignificantBits());
         buffer.putLong(uuid2.getMostSignificantBits());
         buffer.putLong(uuid2.getLeastSignificantBits());

         return Base64.getUrlEncoder().encodeToString(buffer.array());
      } catch (RuntimeException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private static String doBase64Encode(UUID uuid1, UUID uuid2, ByteBuffer sig) {
      try {
         ByteBuffer buffer = ByteBuffer.allocate(48);
         buffer.putLong(uuid1.getMostSignificantBits());
         buffer.putLong(uuid1.getLeastSignificantBits());
         buffer.putLong(uuid2.getMostSignificantBits());
         buffer.putLong(uuid2.getLeastSignificantBits());
         buffer.put(sig);

         return Base64.getUrlEncoder().encodeToString(buffer.array());
      } catch (RuntimeException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static String generateRecordUsername(SecretKeySpec secret, UUID cameraId, UUID accountId, UUID placeId) {
      return doBase64Encode(cameraId, placeId);
   }

   public static String generateRecordPassword(SecretKeySpec secret, UUID cameraId, UUID accountId, UUID placeId, UUID personId, UUID recordingId) {
      try {
         StringBuilder message = new StringBuilder(180);
         message.append(cameraId);
         message.append(accountId);
         message.append(placeId);
         message.append(personId);
         message.append(recordingId);

         Mac hmac = Mac.getInstance("HmacSHA256");
         hmac.init(secret);

         byte[] sigResult = hmac.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));
         ByteBuffer sig = ByteBuffer.wrap(sigResult, 0, 16);

         return doBase64Encode(personId, recordingId, sig);
      } catch (RuntimeException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static String generateAccessExpiration(long expTime) {
      try {
         byte[] expTimeBytes = ByteBuffer.allocate(8).putLong(expTime).array();
         return Base64.getEncoder().encodeToString(expTimeBytes);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static String generateAccessSignature(SecretKeySpec secret, UUID recordingId, long expTime) {
      try {
         byte[] expTimeBytes = ByteBuffer.allocate(8).putLong(expTime).array();
         byte[] recordingIdBytes = recordingId.toString().getBytes(StandardCharsets.UTF_8);

         Mac hmac = Mac.getInstance("HmacSHA256");
         hmac.init(secret);
         hmac.update(recordingIdBytes);
         byte[] result = hmac.doFinal(expTimeBytes);

         return Base64.getEncoder().encodeToString(result);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static boolean verifyAccessSignature(SecretKeySpec secret, UUID recordingId, String expirationTime, String sig) {
      try {
         byte[] expTimeBytes = Base64.getDecoder().decode(expirationTime);
         byte[] recordingIdBytes = recordingId.toString().getBytes(StandardCharsets.UTF_8);

         Mac hmac = Mac.getInstance("HmacSHA256");
         hmac.init(secret);
         hmac.update(recordingIdBytes);

         byte[] result = hmac.doFinal(expTimeBytes);
         byte[] sigb = Base64.getDecoder().decode(sig);
         if (!Arrays.equals(sigb, result)) {
            return false;
         }

         long expirationTimestamp = ByteBuffer.wrap(expTimeBytes).getLong();
         if (expirationTimestamp <= System.currentTimeMillis()) {
            return false;
         }

         return true;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static Date getExpirationTs(QueryStringDecoder decoder) {
      List<String> exps = decoder.parameters().get("exp");
      if (exps == null || exps.size() != 1) {
         throw new IllegalArgumentException();
      }

      byte[] expTimeBytes = Base64.getDecoder().decode(exps.get(0));
      long expirationTimestamp = ByteBuffer.wrap(expTimeBytes).getLong();
      return new Date(expirationTimestamp);
   }

   public static boolean isFavorited(Map<String,Object> recording) {
      Object tagsObj = recording.get(Capability.ATTR_TAGS);
      if (tagsObj == null) {
         return false;
      }

      @SuppressWarnings("unchecked")
      Set<String> tags = (Set<String>)tagsObj;
      return tags.contains(VideoConstants.TAG_FAVORITE);
   }

   public static boolean isFavoritedTag(String prefix, String tag) {
      return (prefix+VideoConstants.TAG_FAVORITE).equals(tag);
   }
   
   public static VideoRecording recMaterializeRecording(ResultSet results, UUID recId) {
      UUID camId = null;
      UUID plcId = null;
      UUID perId = null;
      UUID accId = null;
      long expiration = 0;
      String storage = null;

      int width = -1;
      int height = -1;
      int bandwidth = -1;
      double framerate = -1.0;
      double duration = -1.0;
      long size = -1L;

      int rows = 0;
      long lastBo = -1;
      List<VideoIFrame> iframes = new ArrayList<VideoIFrame>();
      VideoCodec videoCodec = null;
      AudioCodec audioCodec = null;
      for (Row row : results) {
         rows++;
         double ts = row.getDouble(0);
         long bo = row.getLong(1);
         ByteBuffer bl = row.getBytes(2);

         if (ts == VideoConstants.REC_TS_START) {
            if (bo == RecordingTableField.STORAGE.bo()) {
               storage = tostr(bl);
            } else if (bo == RecordingTableField.CAMERA.bo()) {
               camId = touuid(bl);
            } else if (bo == RecordingTableField.ACCOUNT.bo()) {
               accId = touuid(bl);
            } else if (bo == RecordingTableField.PLACE.bo()) {
               plcId = touuid(bl);
            } else if (bo == RecordingTableField.PERSON.bo()) {
               perId = touuid(bl);
            } else if (bo == RecordingTableField.WIDTH.bo()) {
               width = toint(bl);
            } else if (bo == RecordingTableField.HEIGHT.bo()) {
               height = toint(bl);
            } else if (bo == RecordingTableField.BANDWIDTH.bo()) {
               bandwidth = toint(bl);
            } else if (bo == RecordingTableField.FRAMERATE.bo()) {
               framerate = todouble(bl);
            } else if (bo == RecordingTableField.VIDEO_CODEC.bo()) {
               videoCodec = toenum(bl, VideoCodec.class);
            } else if (bo == RecordingTableField.AUDIO_CODEC.bo()) {
               audioCodec = toenum(bl, AudioCodec.class);
            } else if (bo == RecordingTableField.EXPIRATION.bo()) {
            	expiration = tolong(bl);
            }else {
               logger.warn("unknown start metadata for recording: {}", bo);
            }
         } else if (ts == VideoConstants.REC_TS_END) {
            if (bo == RecordingTableField.DURATION.bo()) {
               duration = todouble(bl);
            } else if (bo == RecordingTableField.SIZE.bo()) {
               size = tolong(bl);
            } else {
            	logger.warn("unknown end metadata for recording: {}", bo);
            }
         } else {
            if(bo > lastBo) {
               lastBo = bo;
               iframes.add(new VideoIFrame(ts,bo,tolong(bl)));
            }
            else {
               VIDEO_BAD_FRAME.inc();
            }
         }
      }

      if (rows == 0) {
         VIDEO_DOESNT_EXIST.inc();
         throw new RuntimeException("recording does not exist");
      }

      if (storage == null) {
         VIDEO_NO_STORAGE.inc();
         throw new RuntimeException("recording has no storage location");
      }

      if (accId == null) {
         VIDEO_NO_ACCOUNT.inc();
         throw new RuntimeException("recording has no account");
      }

      if (camId == null) {
         VIDEO_NO_CAMERA.inc();
         throw new RuntimeException("recording has no camera");
      }

      if (plcId == null) {
         VIDEO_NO_PLACE.inc();
         throw new RuntimeException("recording has no place");
      }
      
      if(expiration > 0 && new Date(expiration).before(new Date())) {
      	VIDEO_DOESNT_EXIST.inc();
      	throw new RuntimeException("recording already expired");
      }

      if(duration < 0.0 && iframes.size() > 0) {
         // check for missing metadat, this recording might actually be done
         VideoIFrame lastIframe = iframes.get(iframes.size() - 1);
         long lastIframeTimestamp = IrisUUID.timeof(recId) + (long)(lastIframe.timestamp  * 1000);
         if(lastIframeTimestamp + VideoConstants.MAX_IFRAME_QUIET_PERIOD_MS < System.currentTimeMillis()) {
            duration = lastIframe.timestamp;
            size = lastIframe.byteOffset + lastIframe.byteLength;
         }
         // FIXME consider writing this back to the db
      }

      if(videoCodec == null) {
         videoCodec = VideoCodec.H264_BASELINE_3_1;
      }
      if(audioCodec == null) {
         audioCodec = AudioCodec.NONE;
      }
      
      return new VideoRecording(recId, camId, accId, plcId, expiration, perId, storage, width, height, bandwidth, framerate, duration, size, videoCodec, audioCodec, iframes);
   }

	/////////////////////////////////////////////////////////////////////////////
	// Conversion functions
	/////////////////////////////////////////////////////////////////////////////

	public static ByteBuffer toblob(String value) {
		return DataType.text().serialize(value, ProtocolVersion.V3);
	}

	public static ByteBuffer toblob(UUID value) {
		return DataType.uuid().serialize(value, ProtocolVersion.V3);
	}

	public static ByteBuffer toblob(int value) {
		return DataType.cint().serialize(value, ProtocolVersion.V3);
	}

	public static ByteBuffer toblob(long value) {
		return DataType.bigint().serialize(value, ProtocolVersion.V3);
	}
	
	public static ByteBuffer toblob(Date value) {
		return DataType.timestamp().serialize(value, ProtocolVersion.V3);
	}

	public static ByteBuffer toblob(double value) {
		return DataType.cdouble().serialize(value, ProtocolVersion.V3);
	}

	public static String tostr(ByteBuffer value) {
		return (String) DataType.text().deserialize(value, ProtocolVersion.V3);
	}

	public static UUID touuid(ByteBuffer value) {
		return (UUID) DataType.uuid().deserialize(value, ProtocolVersion.V3);
	}

	public static int toint(ByteBuffer value) {
		return (int) DataType.cint().deserialize(value, ProtocolVersion.V3);
	}

	public static long tolong(ByteBuffer value) {
		return (long) DataType.bigint().deserialize(value, ProtocolVersion.V3);
	}

	public static double todouble(ByteBuffer value) {
		return (double) DataType.cdouble().deserialize(value, ProtocolVersion.V3);
	}
	
	public static Date toDate(ByteBuffer value) {
		return (Date) DataType.timestamp().deserialize(value, ProtocolVersion.V3);
	}

	public static ByteBuffer toblob(Enum<?> e) {
		return toblob(e.name());
	}

	public static <T extends Enum<T>> T toenum(ByteBuffer value, Class<T> clazz) {
		String val = tostr(value);
		return Enum.valueOf(clazz, val);
	}

	
	 public static void recUpdateRecording(ResultSet results, VideoRecording recording) throws Exception {
	      for (Row row : results) {
	         double ts = row.getDouble(0);
	         long bo = row.getLong(1);
	         ByteBuffer bl = row.getBytes(2);

	         if (ts == VideoConstants.REC_TS_END) {
	            if (bo == RecordingTableField.DURATION.bo()) {
	               recording.duration = todouble(bl);
	            } else if (bo == RecordingTableField.SIZE.bo()) {
	               recording.size = tolong(bl);
	            } else {
	               logger.warn("unknown end metadata for recording: {}", bo);
	            }
	         } else {
	            recording.iframes.add(new VideoIFrame(ts,bo,tolong(bl)));
	         }
	      }
	   }
   

	 @SuppressWarnings("unchecked")
	 public static Iterator<UUID> intersection(Iterator<UUID>... iterators) {
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
		
		@SuppressWarnings("unchecked")
		public static Iterator<UUID> union(Iterator<UUID>... iterators) {
			List<PeekingIterator<UUID>> filters = new ArrayList<>(iterators.length);
			for(Iterator<UUID> iterator: iterators) {
				if(iterator != null) {
					filters.add(Iterators.peekingIterator(iterator));
				}
			}
			if(filters.size() > 1) {
				return new UnionIterator(filters);
			}
			else {
				return filters.get(0);
			}
		}
		
		public static Iterator<Row> difference(Iterator<Row> delegate, Iterator<Row> subtract) {
			if(delegate == null) {
				return ImmutableSet.<Row>of().iterator();
			}
			if(subtract == null) {
				return delegate;
			}
			return new DifferenceIterator(delegate, subtract);
		}
		
		private static int compare(UUID timeUUID1, UUID timeUUID2) {
			return IrisUUID.descTimeUUIDComparator().compare(timeUUID1, timeUUID2);
		}
		
		
		/**
		 * Combines a collection of sorted iterators so that each value
		 * is returned once and only once, maintaining ordering.
		 * @author tweidlin
		 *
		 */
		private static class UnionIterator implements Iterator<UUID> {
			private UUID currentId;
			private Collection<PeekingIterator<UUID>> iterators;
			
			public UnionIterator(Collection<PeekingIterator<UUID>> iterators) {
				this.iterators = iterators;
				this.advance();
			}
			
			private void advance() {
				UUID latestTs = IrisUUID.minTimeUUID();
				Iterator<UUID> nextIt = null;
				// find the largest value
				for(PeekingIterator<UUID> it: iterators) {
					if(!it.hasNext()) {
						continue;
					}
					if(currentId != null && currentId.equals(it.peek())) {
						// throw away duplicate entry
						it.next();
						if(!it.hasNext()) {
							continue;
						}
					}
					int comp = compare(latestTs, it.peek());
					if(comp > 0) {
						latestTs = it.peek();
						nextIt = it;
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
		
		private static class DifferenceIterator implements Iterator<Row> {
			private Row next;
			private UUID nextToSkip;
			private Iterator<Row> delegate;
			private Iterator<Row> subtract;
			
			public DifferenceIterator(Iterator<Row> delegate, Iterator<Row> subtract) {
				this.delegate = delegate;
				this.subtract = subtract;
				this.nextToSkip = nextToSkip();
				this.advance();
			}
			
			private UUID nextToSkip() {
				return subtract.hasNext() ? subtract.next().getUUID(AbstractPlaceRecordingIndexV2Table.COL_RECORDINGID) : null;
			}
			
			private void advance() {
				next = null;
				
				while(delegate.hasNext()) {
					Row row = delegate.next();
					UUID id = row.getUUID(AbstractPlaceRecordingIndexV2Table.COL_RECORDINGID);
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
			public Row next() {
				if(next == null) {
					throw new NoSuchElementException();
				}
				else {
					Row current = next;
					advance();
					return current;
				}
			}
		}
}


