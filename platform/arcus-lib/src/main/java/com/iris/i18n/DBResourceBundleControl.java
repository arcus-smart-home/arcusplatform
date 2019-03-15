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
package com.iris.i18n;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.core.dao.ResourceBundleDAO;

public class DBResourceBundleControl extends ResourceBundle.Control {

   private static final String DAO = "dao";
   private final Supplier<ResourceBundleDAO> resourceBundleDaoProvider;

   public DBResourceBundleControl() {
      this.resourceBundleDaoProvider = () -> ServiceLocator.getInstance(ResourceBundleDAO.class);
   }
   
   @Inject
   public DBResourceBundleControl(ResourceBundleDAO resourceBundleDao) {
      this.resourceBundleDaoProvider = () -> resourceBundleDao; 
   }
   
   @Override
   public List<String> getFormats(String baseName) {
      return Collections.<String>singletonList(DAO);
   }

   @Override
   public ResourceBundle newBundle(
         String baseName,
         Locale locale,
         String format,
         ClassLoader loader,
         boolean reload) throws IllegalAccessException, InstantiationException, IOException {

      Preconditions.checkArgument(!StringUtils.isBlank(baseName), "baseName must not be blank");

      if(!DAO.equals(format)) {
         return null;
      }

      ResourceBundleDAO bundleDao = resourceBundleDaoProvider.get();
      Map<String,String> bundle = bundleDao.loadBundle(baseName, locale);
      return new DBResourceBundle(bundle);
   }

   private class DBResourceBundle extends ResourceBundle {
      private final Map<String,String> bundle;

      private DBResourceBundle(Map<String,String> bundle) {
         this.bundle = Collections.unmodifiableMap(bundle);
      }

      @Override
      protected Object handleGetObject(String key) {
         Object value = bundle.get(key);
         if(value == null) {
            return parent != null ? parent.getObject(key) : null;
         }
         return value;
      }

      @Override
      public Enumeration<String> getKeys() {
         if(bundle.isEmpty()) {
            return parent != null ? parent.getKeys() : Collections.<String>emptyEnumeration();
         }
         return Iterators.asEnumeration(bundle.keySet().iterator());
      }
   }
}

