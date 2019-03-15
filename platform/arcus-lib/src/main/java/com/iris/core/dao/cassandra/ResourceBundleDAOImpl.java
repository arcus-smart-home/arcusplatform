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
package com.iris.core.dao.cassandra;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.ResourceBundleDAO;
import com.iris.core.dao.metrics.DaoMetrics;

@Singleton
public class ResourceBundleDAOImpl implements ResourceBundleDAO {

   private static final Timer loadBundleTimer = DaoMetrics.readTimer(ResourceBundleDAO.class, "loadBundle");
   private static final Timer saveBundleTimer = DaoMetrics.insertTimer(ResourceBundleDAO.class, "saveBundle");

   private static final String LOAD_BUNDLE = "SELECT key,value FROM resource_bundle WHERE bundle=? AND locale=?";
   private static final String SAVE_BUNDLE = "INSERT INTO resource_bundle (bundle, locale, key, value) VALUES (?, ?, ?, ?)";
   private static final String DEFAULT_LANGUAGE = "en";
   private static final Logger logger = LoggerFactory.getLogger(ResourceBundleDAOImpl.class);

   private Session session;
   private PreparedStatement loadBundleStmt;
   private PreparedStatement saveBundleStmt;

   @Inject
   public ResourceBundleDAOImpl(Session session) {
      this.session = session;
      loadBundleStmt = session.prepare(LOAD_BUNDLE);
      saveBundleStmt = session.prepare(SAVE_BUNDLE);
   }

   @Override
   public Map<String, String> loadBundle(String bundleName, Locale locale) {
      if(locale == null) {
         locale = Locale.getDefault();
         logger.warn("Attempt to load bundle with a null locale, falling back to the default of {}", locale);
      }
      if(StringUtils.isBlank(locale.getLanguage())) {
         locale = Locale.forLanguageTag(DEFAULT_LANGUAGE);
         logger.warn("Attempt to load bundle with a locale that does not specify a language, falling back to the default of {}", locale);
      }

      BoundStatement boundStatement = new BoundStatement(loadBundleStmt);
      boundStatement.setString("bundle", bundleName);
      boundStatement.setString("locale", sanitizeLocale(locale));

      try(Timer.Context ctxt = loadBundleTimer.time()) {
         List<Row> rows = session.execute(boundStatement).all();
         return rows.stream()
               .collect(Collectors.toMap(
                     (r) -> { return r.getString("key"); },
                     (r) -> { return r.getString("value"); }));
      }
   }

   @Override
   public void saveBundle(final String bundleName, final Locale locale, Map<String, String> localizedValues) {
      Preconditions.checkArgument(!StringUtils.isBlank(bundleName), "bundleName must not be blank");
      Preconditions.checkNotNull(locale, "locale must not be null");

      if(localizedValues.isEmpty()) {
         return;
      }

      localizedValues = new HashMap<String,String>(localizedValues);
      final BatchStatement batch = new BatchStatement();

      localizedValues.forEach((k,v) -> {
         batch.add(new BoundStatement(saveBundleStmt).bind(bundleName, sanitizeLocale(locale), k, v));
      });

      try(Timer.Context ctxt = saveBundleTimer.time()) {
         session.execute(batch);
      }

      // make sure the cache is flushed for the bundle
      // TODO:  this works for this node, but we have no way of notifying the cluster to flush the cache
      ResourceBundle.clearCache();
   }

   private String sanitizeLocale(Locale locale) {
      StringBuilder sb = new StringBuilder(locale.getLanguage().toLowerCase());
      if(!StringUtils.isBlank(locale.getCountry())) {
         sb.append("_").append(locale.getCountry());
      }
      return sb.toString();
   }
}

