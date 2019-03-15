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
package com.iris.resource.azure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.resource.AbstractResource;
import com.iris.resource.Resource;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

/**
 * An example of such resource can be referenced as "azure:/system-test/product_catalog.xml".
 * 
 * @author daniellep
 */
public class AzureResource extends AbstractResource {

   private static final Logger logger = LoggerFactory.getLogger(AzureResource.class);
   private final CloudBlobClient blobClient;
   private final String container;
   private final String blob;

   AzureResource(CloudBlobClient blobClient, String container, String blob) throws URISyntaxException {
      super(new URI(AzureResourceFactory.SCHEME, "/" + container + "/" + blob, null));
      this.blobClient = blobClient;
      this.container = container;
      this.blob = blob;
   }

   @Override
   public boolean exists() {
      return isFile() || isDirectory();
   }

   @Override
   public boolean isReadable() {
      return exists();
   }

   @Override
   public boolean isFile() {
      try {
         CloudBlobContainer blobContainer = blobClient.getContainerReference(this.container);

         if (blobContainer.exists()) {
            CloudBlockBlob blockBlob = blobContainer.getBlockBlobReference(this.blob);
            return blockBlob.exists();
         }
      }
      catch (Exception e) {
         logger.debug("isFile failed to lookup URI - [{}]", getUri(), e);
      }
      return false;
   }

   @Override
   public boolean isDirectory() {
      try {
         CloudBlobContainer blobContainer = blobClient.getContainerReference(this.container);

         if (blobContainer.exists()) {
            CloudBlobDirectory blockBlobDir = blobContainer.getDirectoryReference(this.blob);
            // Blob Directories don't exists unless they have something underneath
            return blockBlobDir.listBlobs().iterator().hasNext();
         }
      }
      catch (Exception e) {
         logger.debug("isDirectory failed to lookup URI - [{}]", getUri(), e);
      }
      return false;
   }

   @Override
   public List<Resource> listResources() {
      ImmutableList.Builder<Resource> bldr = ImmutableList.builder();
      try {

         CloudBlobContainer blobContainer = this.blobClient.getContainerReference(this.container);
         AzureResourceFactory factory = new AzureResourceFactory(this.blobClient);

         if (blobContainer.exists()) {
            CloudBlobDirectory blockBlobDir = blobContainer.getDirectoryReference(this.blob);

            if (blockBlobDir != null) {
               for (ListBlobItem blob : blockBlobDir.listBlobs()) {
                  Resource resource = factory.create(blob.getUri());
                  bldr.add(resource);
               }
            }
         }
      }
      catch (Exception e) {
         logger.debug("isDirectory failed to lookup URI - [{}]", getUri(), e);
      }

      return bldr.build();
   }

   @Override
   public File getFile() {
      return null;
   }

   /* (non-Javadoc)
    * @see com.iris.resource.AbstractResource#open()
    */
   @Override
   public InputStream open() throws IOException {
      try {
         CloudBlobContainer blobContainer = blobClient.getContainerReference(container);
         blobContainer.createIfNotExists();
         CloudBlockBlob blockBlob = blobContainer.getBlockBlobReference(blob);
         if (blockBlob.exists()) {
            return blockBlob.openInputStream();
         }
         else {
            throw new IOException("Blob does not exist: " + container + ", " + blob);
         }
      }
      catch (Exception e) {
         throw new IOException("Unable to initialize blob: " + container + ", " + blob, e);
      }
   }
}

