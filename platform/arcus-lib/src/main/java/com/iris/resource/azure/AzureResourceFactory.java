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
package com.iris.resource.azure;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.google.common.base.Preconditions;
import com.iris.resource.Resource;
import com.iris.resource.ResourceFactory;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobClient;

/**
 * An example of the internal URI for the azure storage can be: azure://myContainer/myBlob
 */
public class AzureResourceFactory implements ResourceFactory {
   public static final String SCHEME = "azure";
   private final CloudBlobClient blobClient;
	
   
   /**
    * @throws InvalidKeyException 
    * @throws URISyntaxException 
    * 
    */
   public AzureResourceFactory(String credentialsString, Boolean isHttps) throws InvalidKeyException, URISyntaxException {
   	StorageCredentials storageCredentials = StorageCredentialsAccountAndKey.tryParseCredentials(credentialsString);
      CloudStorageAccount csa = new CloudStorageAccount(storageCredentials, isHttps);
      this.blobClient = csa.createCloudBlobClient();
   }
   
   public AzureResourceFactory(AzureConfig config) throws InvalidKeyException, URISyntaxException {
      StorageCredentials credentials = new StorageCredentialsAccountAndKey(config.getAccountName(), config.getAccountKey());
		Preconditions.checkArgument(credentials != null, "Invalid account credentials");
		this.blobClient = new CloudStorageAccount(credentials, config.isHttps()).createCloudBlobClient();
   }
   
   public AzureResourceFactory(CloudBlobClient client) {
      this.blobClient = client;
   }
   
   @Override
   public String getScheme() {
      return SCHEME;
   }

   @Override
   public Resource create(URI uri) throws IllegalArgumentException {
      String path = uri.getPath();
      if(path.charAt(0) == '/') {
      	path = path.substring(1);
      }
      try {
      	String[] parts = path.split("/", 2); // we want the container and the path, so we split on the first "/" found
      	if(parts == null || parts.length < 2) {
      		throw new IllegalArgumentException("Invalid URI : " + path);
      	}
         return new AzureResource(blobClient, parts[0], parts[1]);
      }catch(URISyntaxException e) {
         throw new IllegalArgumentException("Invalid URI : " + path, e);
      }
   }

}

