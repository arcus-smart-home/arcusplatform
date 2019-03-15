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
package com.iris.oculus.modules.video;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.RecordingModel;
import com.iris.client.service.VideoService;
import com.iris.client.service.VideoService.GetQuotaResponse;
import com.iris.client.service.VideoService.PageRecordingsRequest;
import com.iris.client.service.VideoService.PageRecordingsResponse;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.video.ux.VideoToolbar;

@Singleton
public class VideoController extends BaseController<RecordingModel> {
   private static final int PAGE_RECORDINGS_SIZE = 50;

   private VideoToolbar toolbar;

   @Inject
   public VideoController() {
      super(RecordingModel.class);
   }

   public void setToolbar(VideoToolbar toolbar) {
      this.toolbar = toolbar;
   }

   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      VideoService service = IrisClientFactory.getService(VideoService.class);

      service.getQuota(getPlaceId())
         .onSuccess((e) -> onQuotaLoad(e))
         .onFailure((e) -> Oculus.warn("Error loading video quota", e))
         ;
      return service
               .pageRecordings(getPlaceId(), PAGE_RECORDINGS_SIZE, null, true, true, PageRecordingsRequest.TYPE_ANY, null, null, null, null)
               .transform(PageRecordingsResponse::getRecordings);
   }
   
   public ClientFuture<PageRecordingsResponse> loadPage(VideoFilter filter, @Nullable String token) {
      VideoService service = IrisClientFactory.getService(VideoService.class);
      
      return
         service.pageRecordings(
               getPlaceId(), 
               PAGE_RECORDINGS_SIZE, 
               token, 
               filter.isIncludeDeleted(), 
               filter.isIncludeInProgress(),
               filter.getType(), 
               filter.getNewest().orElse(null), 
               filter.getOldest().orElse(null), 
               filter.getCameras(),
               filter.getTags()
         )
         .onSuccess((response) -> {
            IrisClientFactory
               .getStore(RecordingModel.class)
               .replace((List) IrisClientFactory.getModelCache().addOrUpdate(response.getRecordings()));
         });
   }

   protected void onQuotaLoad(GetQuotaResponse e) {
      try {
         toolbar.updateQuota(e.getUsed(), e.getTotal());
      } catch (Exception ex) {
         Oculus.warn("Unable to check quota", ex);
      }
   }
}

