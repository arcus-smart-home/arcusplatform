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
package com.iris.capability.attribute.transform;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.iris.i18n.DBResourceBundleControl;
import com.iris.i18n.I18NBundle;
import com.iris.messages.type.HistoryLog;
import com.iris.platform.history.HistoryLogEntry;
import com.netflix.governator.annotations.WarmUp;

/**
 * 
 */
@Singleton
public class HistoryLogEntryTransformer implements BeanAttributesTransformer<HistoryLogEntry> {
   private static final Logger logger = LoggerFactory.getLogger(HistoryLogEntryTransformer.class);

   private ResourceBundle bundle;
   
   /**
    * 
    */
   public HistoryLogEntryTransformer() {
   }
   
   @WarmUp
   public void initialize() {
      try {
         this.bundle = ResourceBundle.getBundle(I18NBundle.HISTORY_LOG.getBundleName(), new DBResourceBundleControl());
      }
      catch(Exception e) {
         logger.warn("Unable to load localization keys", e);
      }
   }

   @Override
   public Map<String, Object> transform(HistoryLogEntry bean) {
      return 
            ImmutableMap
               .<String, Object>builder()
               .put(HistoryLog.ATTR_TIMESTAMP, bean.getTimestamp())
               .put(HistoryLog.ATTR_KEY, bean.getMessageKey())
               .put(HistoryLog.ATTR_SUBJECTADDRESS, bean.getSubjectAddress() == null ? "" : bean.getSubjectAddress())
               .put(HistoryLog.ATTR_SUBJECTNAME, format(bean.getMessageKey() + ".name", bean.getValues()))
               .put(HistoryLog.ATTR_SHORTMESSAGE, format(bean.getMessageKey() + ".short", bean.getValues()))
               .put(HistoryLog.ATTR_LONGMESSAGE, format(bean.getMessageKey() + ".long", bean.getValues()))
               .build()
               ;
   }

   @Override
   public HistoryLogEntry transform(Map<String, Object> attributes) {
      throw new UnsupportedOperationException("Reverse transforms are not currently supported");
   }

   @Override
   public Map<String, Object> merge(HistoryLogEntry bean, Map<String, Object> newAttributes) {
      throw new UnsupportedOperationException("Reverse transforms are not currently supported");
   }
   
   private String format(String key, Collection<String> values) {
      try {
         String message = bundle.getString(key);
         if(message != null) {
            return MessageFormat.format(message, values.toArray());
         }
         logger.warn("Missing localization key {}", key);
      }
      catch(Exception e) {
//         logger.warn("Unable to localize {}", key, e);
      }
      /// TODO
      return key + " " + values;
   }

}

