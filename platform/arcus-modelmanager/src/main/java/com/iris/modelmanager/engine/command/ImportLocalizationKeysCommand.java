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
package com.iris.modelmanager.engine.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FilenameUtils;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.iris.modelmanager.engine.ExecutionContext;


public class ImportLocalizationKeysCommand implements ExecutionCommand {

   private static final String RESOURCEBUNDLE_DIR = "resourcebundles";
   private static final String DEFAULT_LOCALE = "en";
   private static final int BATCH_SIZE_LIMIT = 100;

   private String UPSERT = "UPDATE resource_bundle SET value = ? WHERE bundle = ? AND locale = ? AND key = ?";

   @Override
   public void execute(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      PreparedStatement stmt = context.getSession().prepare(UPSERT);
      try {
         Collection<URL> propertyFiles = context.getManagerContext().getResourceLocator().listDirectory(RESOURCEBUNDLE_DIR);
         for(URL u : propertyFiles) {
            if(isProperties(u)) {
               System.out.println("Loading localization strings from: " + u.getFile());
               upsertBundle(context.getSession(), stmt, u);
            }
         }
      } catch(IOException ioe) {
         throw new CommandExecutionException(ioe);
      }
   }

   @Override
   public void rollback(ExecutionContext context, boolean autoRollback) throws CommandExecutionException {
      System.out.println("Can't rollback localization keys");
   }

   private boolean isProperties(URL u) {
      return FilenameUtils.getExtension(u.getFile()).equalsIgnoreCase("properties");
   }

   private void upsertBundle(Session session, PreparedStatement stmt, URL u) throws IOException {
      BundleLocalePair bundleLocale = BundleLocalePair.parse(u.getFile());

      try(InputStream is = u.openStream()) {
         Properties props = new Properties();
         props.load(is);
         BatchStatement batch = new BatchStatement();
         final AtomicInteger counter = new AtomicInteger();
         props.entrySet().forEach((e) -> {
            if( counter.incrementAndGet() <= BATCH_SIZE_LIMIT ) {
               batch.add(bindLocalizationKey(stmt, bundleLocale.bundle, bundleLocale.locale.toString(), (String) e.getKey(), (String) e.getValue()));
            } else {
               session.execute(batch);
               batch.clear();
               batch.add(bindLocalizationKey(stmt, bundleLocale.bundle, bundleLocale.locale.toString(), (String) e.getKey(), (String) e.getValue()));
               counter.set(1);
            }
         });
         if(counter.get() > 0) {
            session.execute(batch);
         }
      }
   }

   private BoundStatement bindLocalizationKey(PreparedStatement stmt, String bundle, String locale, String key, String value) {
      return new BoundStatement(stmt)
         .setString("bundle", bundle)
         .setString("locale", locale)
         .setString("key", key)
         .setString("value", value);
   }

   private static class BundleLocalePair {

      static BundleLocalePair parse(String file) {
         String[] parts = FilenameUtils.getBaseName(file).split("_");

         BundleLocalePair pair = new BundleLocalePair();
         pair.bundle = parts[0];

         String lang = parts.length > 1 ? parts[1] : DEFAULT_LOCALE;
         String country = parts.length == 3 ? parts[2] : null;

         pair.locale = country == null ? new Locale(lang) : new Locale(lang, country);

         return pair;
      }

      String bundle;
      Locale locale;
   }

}

