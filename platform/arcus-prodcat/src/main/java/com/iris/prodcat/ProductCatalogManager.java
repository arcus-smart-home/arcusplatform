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
package com.iris.prodcat;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.type.Population;
import com.iris.prodcat.parser.Parser;
import com.iris.resource.Resource;

@Singleton
public class ProductCatalogManager {

   private static final Logger logger = LoggerFactory.getLogger(ProductCatalogManager.class);

   private Resource catalogResource; // The resource used to find the catalog. This could be a file or a directory.
   private String productCatalogPath; // The path to the resource used to load the current catalog.  This is always a file.
   private volatile Map<String, ProductCatalog> cacheRef; // reference assignments are already atomic.  Needs to be volatile to ensure new catalogs are picked up across threads.
   private final RedirectBaseUrlHelper urlHelper;
   private final Pattern versioning = Pattern.compile("^\\D*(\\d+)\\.xml$");

   @Inject
   public ProductCatalogManager(ProductCatalogConfig config) {
      this.urlHelper = config.getRedirectBaseUrlHelper();
      this.catalogResource = config.getProductCatalogResource();
      initResource();
   }

   public ProductCatalogManager(Resource catalogResource) {
      this.urlHelper = null;
      this.catalogResource = catalogResource;
      initResource();
   }

   public ProductCatalog getCatalog(String population) {
      if (StringUtils.isEmpty(population)) {
         population = Population.NAME_GENERAL;
      }
      return cacheRef.get(population);
   }

   private void initResource() {
      loadCatalogs();
      if (catalogResource.isWatchable()) {
         catalogResource.addWatch(() -> loadCatalogs());
      }
   }

   private void loadCatalogs() {
      if (this.catalogResource.isFile()) {
         loadCatalogsFromFile(this.catalogResource);
      }
      else if (this.catalogResource.isDirectory()) {
         Resource resource = getNewestResource(this.catalogResource);
         loadCatalogsFromFile(resource);
      }
      else {
         throw new NoSuchElementException(MessageFormat.format("Product catalog not found: [{0}]", this.catalogResource.getRepresentation()));
      }
   }

   /**
    * Convenience method for calling the right loadCatalogs from the listener
    * 
    * @param version
    */
   public void loadCatalogs(Integer version) {
      if (version != null) {
         loadCatalogsVersion(version);
      }
      else {
         loadCatalogs();
      }
   }

   protected void loadCatalogsVersion(int version) {
      if (this.catalogResource.isFile()) {
         throw new IllegalArgumentException(MessageFormat.format("Base product catalog resource is not a directory.  Requested version [{0}] cannot be loaded.  Current catalog file will continue to be used [{1}]", version, this.productCatalogPath));
      }
      else if (this.catalogResource.isDirectory()) {
         Resource resource = getVersionedResource(this.catalogResource, version);

         if (resource != null) {
            loadCatalogsFromFile(resource);
         }
         else {
            throw new NoSuchElementException(MessageFormat.format("Requested product catalog version [{0}] could not be found.  Current catalog file will continue to be used [{1}]", version, this.productCatalogPath));
         }
      }
      else {
         throw new NoSuchElementException(MessageFormat.format("The base product catalog cannot found: [{0}]", this.catalogResource.getRepresentation()));
      }
   }

   private void loadCatalogsFromFile(Resource catalogFile) {
      logger.info("Attempting to load product catalog from [{}]", catalogFile.getRepresentation());

      Map<String, ProductCatalog> catalogs;
      try (InputStream is = catalogFile.open()) {
         Parser parser = new Parser(urlHelper);
         catalogs = parser.parse(is);

         this.cacheRef = catalogs;
         this.productCatalogPath = catalogFile.getRepresentation();
         logger.info("Product catalog loaded from: " + catalogFile.getRepresentation());
      }
      catch (Exception e) {
         throw new RuntimeException(MessageFormat.format("Unable to parse product catalog at [{0}]", catalogFile.getRepresentation()), e);
      }
   }

   /**
    * Search the supplied resource for the file with the largest trailing number.  That is the 'latest' prodcat.
    * 
    * @param rez - Directory resource
    * @return The best available resource
    */
   protected Resource getNewestResource(Resource rez) {
      List<Resource> files = rez.listResources();

      if (files != null && files.size() > 0) {
         List<Resource> sorted = files.stream().sorted(Comparator.comparing((Resource r1) -> getProdCatVersion(r1)).reversed()).collect(Collectors.toList()); // reverse sort: Largest first 
         rez = sorted.get(0);
      }

      return rez; // if files is null or empty, rez is probably a file instead of a directory.
   }

   /**
    * Search the supplied resource for the file the supplied version number.
    * 
    * @param rez - Directory resource
    * @param version - the version of prod cat desired
    * @return The versioned resource or null
    */
   protected Resource getVersionedResource(Resource rez, int version) {
      List<Resource> files = rez.listResources();
      Resource rtn = null;

      if (files != null && files.size() > 0) {
         rtn = files.stream().filter(file -> getProdCatVersion(file).equals(version)).findAny().orElse(null);
      }

      return rtn;
   }

   /**
    * Pulls trailing numbers off of an .xml file.  The numbers are the 'version' of the prodcat.  Highest number is newest.
    * 
    * @param cat
    * @return
    */
   private Integer getProdCatVersion(Resource cat) {
      Integer rtn = 0;
      Matcher match = this.versioning.matcher(cat.getRepresentation());

      if (match.find()) {
         rtn = Integer.parseInt(match.group(1));
      }

      return rtn;
   }

   public String getProductCatalogPath() {
      return this.productCatalogPath;
   }
   
   public int getProductCatalogVersion() {
      return getProdCatVersion(this.catalogResource);
   }

   public Resource getCatalogResource() {
      return this.catalogResource;
   }

   // for testing only.
   void setCatalogResource(Resource catalog) {
      this.catalogResource = catalog;
      initResource();
   }
}

