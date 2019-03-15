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
package com.iris.video.service;

import static com.iris.video.service.VideoServiceMetrics.ADD_TAGS_BAD;
import static com.iris.video.service.VideoServiceMetrics.ADD_TAGS_FAIL;
import static com.iris.video.service.VideoServiceMetrics.ADD_TAGS_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.DELETE_ALL_BAD;
import static com.iris.video.service.VideoServiceMetrics.DELETE_ALL_FAIL;
import static com.iris.video.service.VideoServiceMetrics.DELETE_ALL_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.DELETE_FAIL;
import static com.iris.video.service.VideoServiceMetrics.DELETE_RECORDING_ALREADY_DELETED;
import static com.iris.video.service.VideoServiceMetrics.DELETE_RECORDING_BAD;
import static com.iris.video.service.VideoServiceMetrics.DELETE_RECORDING_DOESNT_EXIST;
import static com.iris.video.service.VideoServiceMetrics.DELETE_RECORDING_INPROGRESS;
import static com.iris.video.service.VideoServiceMetrics.DELETE_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.DOWNLOAD_FAIL;
import static com.iris.video.service.VideoServiceMetrics.DOWNLOAD_RECORDING_BAD;
import static com.iris.video.service.VideoServiceMetrics.DOWNLOAD_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.GET_ATTRIBUTES_BAD;
import static com.iris.video.service.VideoServiceMetrics.GET_ATTRIBUTES_FAIL;
import static com.iris.video.service.VideoServiceMetrics.GET_ATTRIBUTES_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.GET_QUOTA_BAD;
import static com.iris.video.service.VideoServiceMetrics.GET_QUOTA_FAIL;
import static com.iris.video.service.VideoServiceMetrics.GET_QUOTA_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.GET_QUOTA_USED;
import static com.iris.video.service.VideoServiceMetrics.LIST_FAIL;
import static com.iris.video.service.VideoServiceMetrics.LIST_RECORDINGS_BAD;
import static com.iris.video.service.VideoServiceMetrics.LIST_RECORDINGS_NUM;
import static com.iris.video.service.VideoServiceMetrics.LIST_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.METRICS;
import static com.iris.video.service.VideoServiceMetrics.PAGE_FAIL;
import static com.iris.video.service.VideoServiceMetrics.PAGE_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.QUOTA_ENFORCEMENT_ALLOW;
import static com.iris.video.service.VideoServiceMetrics.QUOTA_ENFORCEMENT_DENY;
import static com.iris.video.service.VideoServiceMetrics.REFRESH_QUOTA_FAIL;
import static com.iris.video.service.VideoServiceMetrics.REFRESH_QUOTA_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.REMOVE_TAGS_BAD;
import static com.iris.video.service.VideoServiceMetrics.REMOVE_TAGS_FAIL;
import static com.iris.video.service.VideoServiceMetrics.REMOVE_TAGS_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.SET_ATTRIBUTES_BAD;
import static com.iris.video.service.VideoServiceMetrics.SET_ATTRIBUTES_FAIL;
import static com.iris.video.service.VideoServiceMetrics.SET_ATTRIBUTES_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.START_FAIL;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_BAD;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_INCIDENT;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_OTHER;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_PERSON;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_RULE;
import static com.iris.video.service.VideoServiceMetrics.START_RECORDING_SCENE;
import static com.iris.video.service.VideoServiceMetrics.START_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.STOP_FAIL;
import static com.iris.video.service.VideoServiceMetrics.STOP_RECORDING_BAD;
import static com.iris.video.service.VideoServiceMetrics.STOP_SUCCESS;
import static com.iris.video.service.VideoServiceMetrics.VIEW_FAIL;
import static com.iris.video.service.VideoServiceMetrics.VIEW_RECORDING_BAD;
import static com.iris.video.service.VideoServiceMetrics.VIEW_SUCCESS;
import static com.iris.video.service.VideoServiceUtil.getAccountId;
import static com.iris.video.service.VideoServiceUtil.getPlaceId;
import static com.iris.video.service.VideoServiceUtil.getRecordingId;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.Utils;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.SingleThreadDispatcher;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformDispatcher;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.service.VideoService.DeleteAllRequest;
import com.iris.messages.service.VideoService.GetFavoriteQuotaRequest;
import com.iris.messages.service.VideoService.GetFavoriteQuotaResponse;
import com.iris.messages.service.VideoService.GetQuotaRequest;
import com.iris.messages.service.VideoService.GetQuotaResponse;
import com.iris.messages.service.VideoService.ListRecordingsRequest;
import com.iris.messages.service.VideoService.ListRecordingsResponse;
import com.iris.messages.service.VideoService.PageRecordingsRequest;
import com.iris.messages.service.VideoService.QuotaReportEvent;
import com.iris.messages.service.VideoService.RefreshQuotaRequest;
import com.iris.messages.service.VideoService.RefreshQuotaResponse;
import com.iris.messages.service.VideoService.StartRecordingRequest;
import com.iris.messages.service.VideoService.StartRecordingResponse;
import com.iris.messages.service.VideoService.StopRecordingRequest;
import com.iris.messages.service.VideoService.StopRecordingResponse;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.PagedResults;
import com.iris.util.IrisUUID;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoQuery;
import com.iris.video.VideoUtil;
import com.iris.video.cql.PlaceQuota;
import com.iris.video.cql.VideoConstants;
import com.iris.video.recording.CameraDeletedListener;
import com.iris.video.service.dao.VideoServiceDao;
import com.iris.video.service.handler.PageRecordingsHandler;
import com.iris.video.service.handler.QuotaReportListener;
import com.iris.video.service.quota.QuotaManager;
import com.iris.video.service.quota.VideoQuotaEnforcer;

@Singleton
public class VideoService extends AbstractPlatformMessageListener {
   private static final Logger log = LoggerFactory.getLogger(VideoService.class);
   private static final Address VIDEO_SERVICE_ADDRESS = Address.platformService(PlatformConstants.SERVICE_VIDEO);

   private static final Map<String,Timer> MESSAGE_SUCCESS = ImmutableMap.<String,Timer>builder()
      .put(Capability.CMD_GET_ATTRIBUTES, GET_ATTRIBUTES_SUCCESS)
      .put(Capability.CMD_SET_ATTRIBUTES, SET_ATTRIBUTES_SUCCESS)
      .put(Capability.CMD_ADD_TAGS, ADD_TAGS_SUCCESS)
      .put(Capability.CMD_REMOVE_TAGS, REMOVE_TAGS_SUCCESS)
      .put(ListRecordingsRequest.NAME, LIST_SUCCESS)
      .put(PageRecordingsRequest.NAME, PAGE_SUCCESS)
      .put(StartRecordingRequest.NAME, START_SUCCESS)
      .put(StopRecordingRequest.NAME, STOP_SUCCESS)
      .put(GetQuotaRequest.NAME, GET_QUOTA_SUCCESS)
      .put(RecordingCapability.ViewRequest.NAME, VIEW_SUCCESS)
      .put(RecordingCapability.DownloadRequest.NAME, DOWNLOAD_SUCCESS)
      .put(RecordingCapability.DeleteRequest.NAME, DELETE_SUCCESS)
      .put(DeleteAllRequest.NAME, DELETE_ALL_SUCCESS)
      .put(RefreshQuotaRequest.NAME, REFRESH_QUOTA_SUCCESS)
      .build();

   private static final Map<String,Timer> MESSAGE_FAIL = ImmutableMap.<String,Timer>builder()
      .put(Capability.CMD_GET_ATTRIBUTES, GET_ATTRIBUTES_FAIL)
      .put(Capability.CMD_SET_ATTRIBUTES, SET_ATTRIBUTES_FAIL)
      .put(Capability.CMD_ADD_TAGS, ADD_TAGS_FAIL)
      .put(Capability.CMD_REMOVE_TAGS, REMOVE_TAGS_FAIL)
      .put(ListRecordingsRequest.NAME, LIST_FAIL)
      .put(PageRecordingsRequest.NAME, PAGE_FAIL)
      .put(StartRecordingRequest.NAME, START_FAIL)
      .put(StopRecordingRequest.NAME, STOP_FAIL)
      .put(GetQuotaRequest.NAME, GET_QUOTA_FAIL)
      .put(RecordingCapability.ViewRequest.NAME, VIEW_FAIL)
      .put(RecordingCapability.DownloadRequest.NAME, DOWNLOAD_FAIL)
      .put(RecordingCapability.DeleteRequest.NAME, DELETE_FAIL)
      .put(DeleteAllRequest.NAME, DELETE_ALL_FAIL)
      .put(RefreshQuotaRequest.NAME, REFRESH_QUOTA_FAIL)
      .build();

   private final VideoServiceConfig config;
   private final VideoServiceDao videoDao;
   private final PlaceDAO placeDao;
   private final VideoQuotaEnforcer premiumQuotaEnforcer;
   private final VideoQuotaEnforcer basicQuotaEnforcer;
   private final QuotaManager quotaManager;
   private final LoadingCache<UUID,SingleThreadDispatcher<PlatformMessage>> dispatchers;
   private final PlatformDispatcher listeners;
   private final CameraDeletedListener cameraDeletedListener;

   @Inject
   public VideoService(
         PlatformMessageBus platformBus,
         VideoConfig config,
         PlaceDAO placeDao,
         VideoServiceConfig videoConfig,
         VideoServiceDao videoDao,
         @Named("video.quota.enforcer.impl.premium")
         VideoQuotaEnforcer premiumQuotaEnforcer,
         @Named("video.quota.enforcer.impl.basic")
         VideoQuotaEnforcer basicQuotaEnforcer,
         QuotaManager quotaManager,
         PlatformDispatcherFactory factory,
         PageRecordingsHandler pageRecordingsHandler,
         QuotaReportListener quotaReportListener,
         CameraDeletedListener cameraDeletedListener
      ) {
      super(platformBus, "video-service", config.getMaxThreads(), config.getThreadKeepAliveMs());

      this.config = videoConfig;
      this.placeDao = placeDao;
      this.videoDao = videoDao;
      this.premiumQuotaEnforcer = premiumQuotaEnforcer;
      this.basicQuotaEnforcer = basicQuotaEnforcer;
      this.quotaManager = quotaManager;
      this.cameraDeletedListener = cameraDeletedListener;
      this.listeners =
            factory
               .buildDispatcher()
               .addAddressMatcher(AddressMatchers.BROADCAST_MESSAGE_MATCHER)
               .addAddressMatcher(AddressMatchers.platformService(MessageConstants.SERVICE, com.iris.messages.service.VideoService.NAMESPACE))
               .addAnnotatedHandler(pageRecordingsHandler)
               .addAnnotatedHandler(quotaReportListener)
               .addRequestHandler(Capability.CMD_GET_ATTRIBUTES, handler(this::handleGetAttributes))
               .addRequestHandler(Capability.CMD_SET_ATTRIBUTES, handler(this::handleSetAttributes))
               .addRequestHandler(Capability.CMD_ADD_TAGS, handler(this::handleAddTags))
               .addRequestHandler(Capability.CMD_REMOVE_TAGS, handler(this::handleRemoveTags))
               .addRequestHandler(ListRecordingsRequest.NAME, handler(this::handleListRecordings))
               .addRequestHandler(StartRecordingRequest.NAME, handler(this::handleStartRecording))
               .addRequestHandler(StopRecordingRequest.NAME, handler(this::handleStopRecording))
               .addRequestHandler(GetQuotaRequest.NAME, handler(this::handleGetQuota))
               .addRequestHandler(GetFavoriteQuotaRequest.NAME, handler(this::handleGetFavoriteQuota))
               .addRequestHandler(RecordingCapability.ViewRequest.NAME, handler(this::handleViewRecording))
               .addRequestHandler(RecordingCapability.DownloadRequest.NAME, handler(this::handleDownloadRecording))
               .addRequestHandler(RecordingCapability.DeleteRequest.NAME, handler(this::handleDeleteRecording))
               .addRequestHandler(RecordingCapability.ResurrectRequest.NAME, handler(this::handleResurrectRecording))
               .addRequestHandler(DeleteAllRequest.NAME, handler(this::handleDeleteAll))
               .addRequestHandler(RefreshQuotaRequest.NAME, handler(this::handleRefreshQuota))
               .addEventConsumer(Capability.EVENT_DELETED, consumer(this::handleEventDeleted))
               .addUnsupportedFallbackRequestHandler()
               .build();
      this.dispatchers = CacheBuilder.newBuilder()
         .concurrencyLevel(config.getMaxThreads())
         .expireAfterAccess(config.getDispatchCacheTimeMs(), TimeUnit.MILLISECONDS)
         .recordStats()
         .build(new CacheLoader<UUID,SingleThreadDispatcher<PlatformMessage>>() {
            @Override
            public SingleThreadDispatcher<PlatformMessage> load(@Nullable UUID unused) {
               return new SingleThreadDispatcher<PlatformMessage>(listeners::onMessage, new LinkedBlockingQueue<>(config.getMaxQueueDepth()));
            }
         });

       METRICS.monitor("dispatch.cache", dispatchers);
   }
   
   private interface MethodHandler {
      MessageBody handle(PlatformMessage message, MessageBody req) throws Exception;
   }
   
   private interface MethodListener {
      void onEvent(PlatformMessage message, MessageBody event) throws Exception;
   }
   
   private Function<PlatformMessage, MessageBody> handler(MethodHandler handler) {
      return (message) -> {
         long startTime = System.nanoTime();
         String type = message.getMessageType();
         try {
            MessageBody response = handler.handle(message, message.getValue());

            Timer success = MESSAGE_SUCCESS.get(type);
            if (success != null) {
               success.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
            return response;
         } catch(Exception e) {
            Timer fail = MESSAGE_FAIL.get(type);
            if (fail != null) {
               fail.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
            return Errors.fromException(e);
         }
      };
   }
   
   private Consumer<PlatformMessage> consumer(MethodListener listener) {
      return (message) -> {
         long startTime = System.nanoTime();
         String type = message.getMessageType();
         try {
            listener.onEvent(message, message.getValue());

            Timer success = MESSAGE_SUCCESS.get(type);
            if (success != null) {
               success.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
         } catch(Exception e) {
            log.warn("Error processing event", e);
            Timer fail = MESSAGE_FAIL.get(type);
            if (fail != null) {
               fail.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
         }
      };
   }

   @Override
   protected void onStart() {
      log.info("Started video service");
      addListeners(listeners.matchers());
   }

   @Override
   protected void onStop() {
      log.info("Shutting down video service");
   }

   @Override
   public void handleMessage(@Nullable PlatformMessage message) {
      long startTime = System.nanoTime();
      if (message == null) {
         return;
      }

      String type = message.getMessageType();
      try {
         switch (type) {
         case RefreshQuotaRequest.NAME:         	
         case Capability.CMD_GET_ATTRIBUTES:
         case Capability.CMD_SET_ATTRIBUTES:
         case Capability.CMD_ADD_TAGS:
         case Capability.CMD_REMOVE_TAGS:
         case ListRecordingsRequest.NAME:
         case PageRecordingsRequest.NAME:
         case StartRecordingRequest.NAME:
         case StopRecordingRequest.NAME:
         case GetQuotaRequest.NAME:
         case GetFavoriteQuotaRequest.NAME:         
         case QuotaReportEvent.NAME:
         case RecordingCapability.ViewRequest.NAME:
         case RecordingCapability.DownloadRequest.NAME:
         case RecordingCapability.DeleteRequest.NAME:
         case RecordingCapability.ResurrectRequest.NAME:
         case DeleteAllRequest.NAME:
         case Capability.EVENT_DELETED:
            MessageBody body = message.getValue();
            UUID placeId = getPlaceId(message, body);
            dispatchers.get(placeId).dispatchOrQueue(message);
            break;
         case Capability.EVENT_VALUE_CHANGE:
            Address src = message.getSource();
            if (RecordingCapability.NAMESPACE.equals(src.getNamespace()) && message.getActor() == null) {
               // NOTE: This service sends value changes on videos using an actor header to ensure that
               //       we aren't processing our own value changes when maintaining quota. We ignore
               //       value changes that have an actor header present so we don't double process
               //       quota changes. We need to update the quota immediately, rather than wait for
               //       the value change from ourself, so other start video requests work when they 
               //       should.
               MessageBody vcBody = message.getValue();
               UUID vcPlaceId = getPlaceId(message, vcBody);
               dispatchers.get(vcPlaceId).dispatchOrQueue(message);
            }
            break;
         default:
            super.handleMessage(message);
            break;
         }
      } catch(Exception e) {
         if (message.isRequest()) {
            PlatformMessage response =
                  PlatformMessage
                     .respondTo(message)
                     .withPayload(Errors.fromException(e))
                     .create()
                     ;
            sendMessage(response);
         }

         Timer fail = MESSAGE_FAIL.get(type);
         if (fail != null) {
            fail.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         }
      }
   }

   private void filter(Map<String,Object> attrs, Collection<String> names) {
      if (names == null || names.isEmpty()) {
         return;
      }

      Iterator<String> it = attrs.keySet().iterator();
      while (it.hasNext()) {
         String key = it.next();

         boolean remove = true;
         for (String match : names) {
            if (match == null) continue;
            remove = (Utils.isNamespaced(match)) ?  match.equals(key) : key.startsWith(match + ":");
            if (remove) break;
         }

         if (remove) {
            it.remove();
         }
      }
   }

   protected MessageBody handleGetAttributes(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if (placeId == null || recordingId == null) {
         GET_ATTRIBUTES_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      VideoMetadata recording = videoDao.getRecording(placeId,recordingId);
      Errors.assertFound(recording, message.getDestination());

      Map<String,Object> attrs = recording.toMap();
      filter(attrs, (Collection<String>) req.getAttributes().get("names"));

      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, attrs);
   }

   protected MessageBody handleSetAttributes(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if(placeId == null || recordingId == null) {
         SET_ATTRIBUTES_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      Map<String,Object> attrs = req.getAttributes();
      if (attrs != null && !attrs.isEmpty()) {
         Map<String,Object> changed = videoDao.setRecording(placeId, recordingId, attrs);
         MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changed);
         return body;
      }

      return MessageBody.emptyMessage();
   }

   private void sendTagsValueChange(Address addr, UUID placeId, String population, UUID recordingId, Set<String> expectedTagsAfterwards) throws Exception {      
      Map<String, Object> changedValues = ImmutableMap.of(Capability.ATTR_TAGS,expectedTagsAfterwards);
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changedValues);
      PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
         .withPlaceId(placeId)
         .withPopulation(population)
         .create();

      sendMessage(evt);
   }

   private void sendDeleteTimeValueChange(Address addr, UUID placeId, String population, UUID recordingId, Date deleteTime) throws Exception {
      Map<String,Object> attrs = new HashMap<>();

      if (deleteTime == null) {
         attrs.put(RecordingCapability.ATTR_DELETED,false);
         attrs.put(RecordingCapability.ATTR_DELETETIME,VideoConstants.DELETE_TIME_SENTINEL);
      } else {
         attrs.put(RecordingCapability.ATTR_DELETED,true);
         attrs.put(RecordingCapability.ATTR_DELETETIME,deleteTime);
      }

      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs);
      PlatformMessage evt = PlatformMessage.buildBroadcast(body, addr)
         .withPlaceId(placeId)
         .withPopulation(population)
         .withActor(VIDEO_SERVICE_ADDRESS)
         .create();

      sendMessage(evt);
   }

   protected MessageBody handleAddTags(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if(placeId == null || recordingId == null) {
         ADD_TAGS_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      Collection<String> tags = (Collection<String>)req.getAttributes().get("tags");
      if (tags != null && !tags.isEmpty()) {
      	PlaceQuota favoriteQuota = null;
      	boolean containFavorite = false;
      	if(tags.contains(VideoConstants.TAG_FAVORITE)) {
      		//check favorite quote
      		containFavorite = true;
      		favoriteQuota = quotaManager.getQuotaForPlace(placeId, true);
      		Errors.assertValidRequest(favoriteQuota.isUnderQuota(), "Exceeded max number of favorite videos");  
      	}
         videoDao.addTags(placeId, recordingId, tags);
         VideoMetadata metadata = videoDao.getRecording(placeId, recordingId);
         sendTagsValueChange(message.getDestination(), placeId, message.getPopulation(), recordingId, metadata.getTags());
         if(containFavorite) {
         	updateFavoriteQuotaAndSendEvent(placeId, favoriteQuota, 1);
         }
      }

      return MessageBody.emptyMessage();
   }
   
   private void updateFavoriteQuotaAndSendEvent(UUID placeId, PlaceQuota favoriteQuota, int count) {
   	if(favoriteQuota != null) {
      	quotaManager.sendQuotaReportEvent(placeId, favoriteQuota.getUsed()+count, (new Date()).getTime(), favoriteQuota.getUnit(), true);
      }
   }

   protected MessageBody handleRemoveTags(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if(placeId == null || recordingId == null) {
         REMOVE_TAGS_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      Collection<String> tags = (Collection<String>)req.getAttributes().get("tags");
      if (tags != null && !tags.isEmpty()) {
      	boolean containFavorite = false;
			if(tags.contains(VideoConstants.TAG_FAVORITE)) {
				containFavorite = true;     		      		 
      	}
						
			ListenableFuture<Set<String>> futureResult = videoDao.removeTags(placeId, recordingId, tags);
			PlaceQuota favoriteQuota = quotaManager.getQuotaForPlace(placeId, true);
      	futureResult.addListener(() -> {
            try {
            	sendTagsValueChange(message.getDestination(), placeId, message.getPopulation(), recordingId, futureResult.get());
            }catch(Exception e) {
               log.warn("Unable to send tags value change event for recording [{}] place [{}]", recordingId, placeId, e);
            }
         }, MoreExecutors.directExecutor());
      	if(containFavorite) {
      		futureResult.addListener(() -> {
               try {             	
               	updateFavoriteQuotaAndSendEvent(placeId, favoriteQuota, -1);
               }catch(Exception e) {
                  log.warn("Unable to update favorite quota for recording [{}] place [{}]", recordingId, placeId, e);
               }
            },
            MoreExecutors.directExecutor());
      	}
		}
      return MessageBody.emptyMessage();
   }

   protected MessageBody handleListRecordings(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      if(placeId == null) {
         LIST_RECORDINGS_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      boolean all = ListRecordingsRequest.getAll(req);

      VideoQuery query = new VideoQuery();
      query.setPlaceId(placeId);
      query.setLimit(VideoServiceDao.MAX_LIST_RECORDING);
      query.setListDeleted(all);
      query.setToken(null);

      PagedResults<Map<String, Object>> results = videoDao.listPagedRecordingsForPlace(query);
      List<Map<String,Object>> recs = results.getResults();

      LIST_RECORDINGS_NUM.update(recs.size());
      MessageBody body = ListRecordingsResponse.builder()
         .withRecordings(recs)
         .build();

      return body;
   }

   protected MessageBody handleStartRecording(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      if (placeId == null) {
         START_RECORDING_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      Place plc = placeDao.findById(placeId);
      if (plc == null) {
         throw new IllegalStateException("could not load place");
      }

      PlaceQuota quota = quotaManager.getQuotaForPlace(placeId, false);
      Boolean stream = StartRecordingRequest.getStream(req);

      boolean premium = ServiceLevel.isPremiumOrPromon(plc.getServiceLevel());
      VideoQuotaEnforcer quotaEnforcer = premium ? premiumQuotaEnforcer : basicQuotaEnforcer;

      long quotaStartTime = System.nanoTime();
      if (quotaEnforcer.allowRecording(plc, Boolean.TRUE.equals(stream), quota, (deleted) -> quotaManager.decrementQuota(placeId, deleted))) {
         QUOTA_ENFORCEMENT_ALLOW.update(System.nanoTime() - quotaStartTime, TimeUnit.NANOSECONDS);
      } else {
         QUOTA_ENFORCEMENT_DENY.update(System.nanoTime() - quotaStartTime, TimeUnit.NANOSECONDS);
         return Errors.fromCode("quota.exceeded", "Video recording quota has been exceeded");
      }

      UUID accountId = getAccountId(message, req);
      UUID cameraId  = getCameraId(message, req);
      Integer duration = StartRecordingRequest.getDuration(req);
      if(duration == null || duration <= 0) {
         duration = config.getVideoStreamDefaultTime();
      }

      UUID personId = IrisUUID.nilUUID();
      if(message.getActor() != null && message.getActor() instanceof PlatformServiceAddress) {
         PlatformServiceAddress addr = (PlatformServiceAddress) message.getActor();
         if(addr.getGroup().equals(PersonCapability.NAMESPACE)) {
            START_RECORDING_PERSON.inc();
            personId = (UUID) addr.getContextId();
         } else if(addr.getGroup().equals(RuleCapability.NAMESPACE)) {
            START_RECORDING_RULE.inc();
            personId = new UUID(0, addr.getContextQualifier());
         } else if(addr.getGroup().equals(SceneCapability.NAMESPACE)) {
            START_RECORDING_SCENE.inc();
            personId = new UUID(1, addr.getContextQualifier());
         } else if(addr.getGroup().equals(AlarmIncidentCapability.NAMESPACE)) {
            START_RECORDING_INCIDENT.inc();
            personId = (UUID) addr.getContextId();
         } else {
            START_RECORDING_OTHER.inc();
         }
      } else {
         START_RECORDING_OTHER.inc();
      }

      UUID uuid = stream ? VideoUtil.timeUUIDForStream() : VideoUtil.timeUUIDForRecording();
      SecretKeySpec secret = config.getRecordSecretAsSpec();
      String recordingUser = VideoUtil.generateRecordUsername(secret, cameraId, accountId, placeId);
      String recordingPass = VideoUtil.generateRecordPassword(secret, cameraId, accountId, placeId, personId, uuid);

      MessageBody startStream = CameraCapability.StartStreamingRequest.builder()
            .withPassword(recordingPass)
            .withUrl(config.getVideoRecordUrl())
            .withUsername(recordingUser)
            .withMaxDuration(Math.min(duration, config.getVideoStreamMaxTime()))
            .withStream(stream)
            .build();
      PlatformMessage msg = PlatformMessage.buildRequest(
            startStream,
            Address.platformService(com.iris.messages.service.VideoService.NAMESPACE),
            Address.platformDriverAddress(cameraId))
            .withCorrelationId(UUID.randomUUID().toString())
            .withPlaceId(placeId)
            .withPopulation(message.getPopulation())
            .withActor(message.getActor())
            .create();
      sendMessage(msg);

      long expTime = System.currentTimeMillis() + config.getVideoStreamAccessTime();
      String exp = VideoUtil.generateAccessExpiration(expTime);
      String sig = VideoUtil.generateAccessSignature(config.getStreamSecretAsSpec(), uuid, expTime);

      MessageBody body = StartRecordingResponse.builder()
            .withHls(VideoUtil.getHlsUri(config.getVideoStreamingUrl(), uuid, exp, sig))           
            .withPreview(VideoUtil.getJpgUri(config.getVideoStreamingUrl(), uuid, exp, sig))
            .withExpiration(new Date(expTime))
            .withRecordingId(uuid.toString())
            .build();

      return body;
   }

   private UUID getCameraId(PlatformMessage message, MessageBody req) {
      String address = StartRecordingRequest.getCameraAddress(req);
      Errors.assertRequiredParam(address, StartRecordingRequest.ATTR_CAMERAADDRESS);
      Address cameraAddr = Address.fromString(address);
      UUID cameraId = (UUID) cameraAddr.getId();
      Errors.assertValidRequest(cameraId != null, "Invalid camera address");
      return cameraId;
   }

	protected MessageBody handleStopRecording(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recId = UUID.fromString(StopRecordingRequest.getRecordingId(req));
      if(placeId == null || recId == null) {
         STOP_RECORDING_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      return StopRecordingResponse.instance();
   }

   protected MessageBody handleGetQuota(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      if(placeId == null) {
         GET_QUOTA_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      PlaceQuota quota = quotaManager.getQuotaForPlace(placeId, false);
      if (quota.getQuota() > 0) {
         GET_QUOTA_USED.update((100L * quota.getUsed()) / quota.getQuota());
	   }

      MessageBody body = GetQuotaResponse.builder()
         .withUsed((double) quota.getUsed())
         .withTotal((double) quota.getQuota())
         .build();

      return body;
   }
   
   protected MessageBody handleGetFavoriteQuota(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      if(placeId == null) {
         GET_QUOTA_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      PlaceQuota quota = quotaManager.getQuotaForPlace(placeId, true);
      if (quota.getQuota() > 0) {
         GET_QUOTA_USED.update((100L * quota.getUsed()) / quota.getQuota());
	   }

      MessageBody body = GetFavoriteQuotaResponse.builder()
         .withUsed((int) quota.getUsed())
         .withTotal((int) quota.getQuota())
         .build();

      return body;
   }

   protected MessageBody handleViewRecording(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if(placeId == null || recordingId == null) {
         VIEW_RECORDING_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      long expTime = System.currentTimeMillis() + config.getVideoStreamAccessTime();
      String exp = VideoUtil.generateAccessExpiration(expTime);
      String sig = VideoUtil.generateAccessSignature(config.getStreamSecretAsSpec(), recordingId, expTime);

      MessageBody msg = RecordingCapability.ViewResponse.builder()
         .withHls(VideoUtil.getHlsUri(config.getVideoStreamingUrl(), recordingId, exp, sig))
         .withPreview(VideoUtil.getJpgUri(config.getVideoStreamingUrl(), recordingId, exp, sig))
         .withExpiration(new Date())
         .build();

      return msg;
   }

   protected MessageBody handleDownloadRecording(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if(placeId == null || recordingId == null) {
         DOWNLOAD_RECORDING_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      VideoMetadata recording = videoDao.getRecording(placeId, recordingId);
      if (recording == null) {
         return Errors.notFound(message.getDestination());
      }

      if (recording.isInProgress()) {
         return Errors.invalidRequest("recording still in progress");
      }

      if (recording.isDeleted()) {
         return Errors.invalidRequest("recording deleted");
      }

      long expTime = System.currentTimeMillis() + config.getVideoDownloadAccessTime();
      String exp = VideoUtil.generateAccessExpiration(expTime);
      String sig = VideoUtil.generateAccessSignature(config.getDownloadSecretAsSpec(), recordingId, expTime);
      long size = recording.getSize();

      MessageBody msg = RecordingCapability.DownloadResponse.builder()
         .withMp4(VideoUtil.getMp4Uri(config.getVideoDownloadUrl(), recordingId, exp, sig))
         .withPreview(VideoUtil.getJpgUri(config.getVideoStreamingUrl(), recordingId, exp, sig))
         .withExpiration(new Date())
         .withMp4SizeEstimate((size*91L)/100L)
         .build();

      return msg;
   }

   protected MessageBody handleDeleteRecording(PlatformMessage message, MessageBody req) throws Exception {
      UUID placeId = getPlaceId(message, req);
      UUID recordingId = getRecordingId(message, req);
      if (placeId == null || recordingId == null) {
         DELETE_RECORDING_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      VideoMetadata recording = videoDao.getRecording(placeId, recordingId);
      if (recording == null) {
         DELETE_RECORDING_DOESNT_EXIST.inc();
         return Errors.notFound(message.getDestination());
      }

      if (recording.isInProgress()) {
         DELETE_RECORDING_INPROGRESS.inc();
         return Errors.invalidRequest("recording still in progress");
      }

      if (recording.isDeleted()) {
         DELETE_RECORDING_ALREADY_DELETED.inc();
         // this request is idempotent, deleting a previously deleted video silently succeeds to enable safe retries
         return RecordingCapability.DeleteResponse.instance();
      }

      log.info("marking recording for deletion: place={}, recording={}", placeId, recordingId);
      Date scheduledAt = videoDao.deleteRecording(placeId, recordingId, recording.isFavorite());

      try {
         try {
            sendDeleteTimeValueChange(message.getDestination(), placeId, message.getPopulation(), recordingId, scheduledAt);
         } catch (Exception ex) {
            log.warn("failed to send video recording deleteTime value change event: {}", ex.getMessage(), ex);
         }

         return RecordingCapability.DeleteResponse.instance();
      } finally {
         if(recording.isFavorite()) {
         	PlaceQuota favoriteQuota = quotaManager.getQuotaForPlace(placeId, true);
         	updateFavoriteQuotaAndSendEvent(placeId, favoriteQuota, -1);
         }
      }
   }

   protected MessageBody handleResurrectRecording(PlatformMessage message, MessageBody req) throws Exception {
   	return Errors.unsupportedMessageType(message.getMessageType());      
   }

   protected MessageBody handleDeleteAll(PlatformMessage message, MessageBody body) throws Exception{
      UUID placeId = getPlaceId(message, body);
      if (placeId == null) {
         DELETE_ALL_BAD.inc();
         return Errors.notFound(message.getDestination());
      }

      boolean deleteFavorites = Optional.ofNullable(DeleteAllRequest.getIncludeFavorites(body)).orElse(false);
	   videoDao.deleteAllRecordings(placeId);

      MessageBody eventBody = com.iris.messages.service.VideoService.RecordingsDeletedEvent.builder()
            .withIncludeFavorites(deleteFavorites)
            .build();

      PlatformMessage eventMessage = PlatformMessage.buildEvent(message, eventBody).create();
      sendMessage(eventMessage);
      
      return com.iris.messages.service.VideoService.DeleteAllResponse.instance();
   }


   protected void handleEventDeleted(PlatformMessage message, MessageBody value) throws Exception {
	   Address address = message.getSource();
	   if(PlatformConstants.SERVICE_PLACES.equals(address.getGroup())) {
	   	videoDao.deleteAllRecordings((UUID)address.getId());
	   }else if(PlatformConstants.SERVICE_DEVICES.equals(address.getGroup())) {
	   	cameraDeletedListener.onMessage(message);
	   }
   }
   
   protected MessageBody handleRefreshQuota(PlatformMessage message, MessageBody value) throws Exception {	  
   	UUID placeId = getPlaceId(message, value);
   	log.info("Refresh quota from the in-memory cache for place [{}]", placeId);
   	quotaManager.invalidateQuotaCache(placeId);	   
	   return RefreshQuotaResponse.instance();
   }
   
}

