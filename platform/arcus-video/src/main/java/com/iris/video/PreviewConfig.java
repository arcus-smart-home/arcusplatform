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

import java.io.File;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.microsoft.azure.storage.StorageCredentials;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;

@Singleton
public class PreviewConfig {
	public static final String PREVIEWS_STORAGE_TYPE_FS = "fs";
	public static final String PREVIEWS_STORAGE_TYPE_AZURE = "azure";
	public static final String PREVIEWS_STORAGE_TYPE_NULL = "null";

	@Inject(optional = true)
	@Named("previews.storage.type")
	protected String storageType = PREVIEWS_STORAGE_TYPE_FS;

	////////////////////////////////////////////
	// File Specific
	////////////////////////////////////////////

	@Inject(optional = true)
	@Named("previews.storage.fs.base_path")
	protected String storageFsBasePath = "/data/video/previews";	

	////////////////////////////////////////////
	// Azure Specific
	////////////////////////////////////////////

	@Inject(optional = true)
	@Named("previews.storage.azure.access.duration")
	protected long storageAzureAccessDuration = TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);

	@Inject
	@Named("previews.storage.azure.container")
	protected String storageAzureContainer;

	@Inject(optional = true)
	@Named("previews.storage.azure.instrument")
	protected boolean storageAzureInstrument = false;

	@Inject(optional = true)
	@Named("previews.storage.azure.buffer.size")
	protected int storageAzureBufferSize = -1;

	@Inject(optional = true)
	@Named("previews.storage.azure.fetch.size")
	protected int storageAzureFetchSize = 16 * 1024;

	@Inject(optional = true)
	@Named("previews.storage.azure.max.threads")
	protected int storageAzureMaxThreads = 10;

	@Inject(optional = true)
	@Named("previews.storage.azure.keep.alive.ms")
	protected long storageAzureKeepAliveMs = TimeUnit.MINUTES.toMillis(5);

	
	@Inject
	protected ConfigurationProvider configProvider;

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	////////////////////////////////////////////
	// File Specific
	////////////////////////////////////////////

	public File getStorageFsBasePath() {
		return new File(storageFsBasePath);
	}

	public void setStorageFsBasePath(String storageFsBasePath) {
		this.storageFsBasePath = storageFsBasePath;
	}

	
	////////////////////////////////////////////
	// Azure Specific
	// TODO:  Create common azure configs for video preview and video.
	////////////////////////////////////////////

	public long getStorageAzureAccessDuration() {
		return storageAzureAccessDuration;
	}

	public void setStorageAzureAccessDuration(long storageAzureAccessDuration) {
		this.storageAzureAccessDuration = storageAzureAccessDuration;
	}

	public String getStorageAzureContainer() {
		return storageAzureContainer;
	}

	public void setStorageAzureContainer(String storageAzureContainer) {
		this.storageAzureContainer = storageAzureContainer;
	}

	public boolean isStorageAzureInstrument() {
		return storageAzureInstrument;
	}

	public void setStorageAzureInstrument(boolean storageAzureInstrument) {
		this.storageAzureInstrument = storageAzureInstrument;
	}

	public int getStorageAzureBufferSize() {
		return storageAzureBufferSize;
	}

	public void setStorageAzureBufferSize(int storageAzureBufferSize) {
		this.storageAzureBufferSize = storageAzureBufferSize;
	}

	public int getStorageAzureFetchSize() {
		return storageAzureFetchSize;
	}

	public void setStorageAzureFetchSize(int storageAzureFetchSize) {
		this.storageAzureFetchSize = storageAzureFetchSize;
	}

	public List<StorageCredentials> getStorageAzureAccounts() {
		List<StorageCredentials> result = new ArrayList<>();

		for (int i = 1; true; ++i) {
			String rawAccount = "previews.storage.azure.account" + i;
			ConfigurationKey confAccount = new ConfigurationKey(rawAccount, KeyParser.parse(rawAccount));
			Supplier<String> supAccount = configProvider.getStringSupplier(confAccount, null);
			String account = (supAccount == null) ? null : supAccount.get();

			if (account == null || account.trim().isEmpty()) {
				break;
			}

			try {
				StorageCredentials creds = StorageCredentials.tryParseCredentials(account);
				if (creds == null) {
					throw new RuntimeException("invalid azure storage credentials");
				}

				result.add(creds);
			} catch (InvalidKeyException ex) {
				throw new RuntimeException(ex);
			}
		}

		return result;
	}

	public void setStorageAzureMaxThreads(int maxThreads) {
		storageAzureMaxThreads = maxThreads;
	}

	public int getStorageAzureMaxThreads() {
		return storageAzureMaxThreads;
	}


	public void setStorageAzureKeepAliveMs(long ms) {
		storageAzureKeepAliveMs = ms;
	}

	public long getStorageAzureKeepAliveMs() {
		return storageAzureKeepAliveMs;
	}

}

