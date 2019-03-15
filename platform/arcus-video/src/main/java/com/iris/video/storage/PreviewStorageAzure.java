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
package com.iris.video.storage;

import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_CREATE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_CREATE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_DOWNLOAD_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_DOWNLOAD_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_NO_CONTAINER;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_DELETE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_AZURE_DELETE_FAIL;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class PreviewStorageAzure implements PreviewStorage {
	private static final Logger log = LoggerFactory.getLogger(PreviewStorageAzure.class);

	private final List<CloudBlobContainer> containers;
	private final Map<String, CloudBlobClient> accounts;
	private final ExecutorService executor;

	public PreviewStorageAzure(List<StorageCredentials> accounts, String container, ExecutorService executor) {
		this.containers = new ArrayList<>();
		this.accounts = new HashMap<>();
		this.executor = executor;

		for (StorageCredentials account : accounts) {
			try {
				CloudStorageAccount storage = new CloudStorageAccount(account, true);
				this.accounts.put(account.getAccountName(), storage.createCloudBlobClient());

				CloudBlobClient client = storage.createCloudBlobClient();
				this.containers.add(getStorageContainer(client, container));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		log.info("configured azure storage with {} accounts", accounts.size());
	}

	private static CloudBlobContainer getStorageContainer(CloudBlobClient client, String container) {
		try {
			CloudBlobContainer cnt = client.getContainerReference(container);
			cnt.createIfNotExists();

			return cnt;
		} catch (Exception ex) {
			VIDEO_PREVIEW_AZURE_NO_CONTAINER.inc();
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void write(String id, byte[] image, long ttl, TimeUnit unit) throws IOException {
		// Pass through Azure does not support ttl.
		write(id, image);
	}

	@Override
	public void write(String id, byte[] image) throws IOException {
		executor.execute(() -> { 		
			long startTime = System.nanoTime();
	
			CloudBlobContainer container = getContainer(id);
			try {
				CloudBlockBlob blob = container.getBlockBlobReference(id);
				blob.uploadFromByteArray(image, 0, image.length);
				VIDEO_PREVIEW_AZURE_CREATE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				VIDEO_PREVIEW_AZURE_CREATE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
				log.error("Failed to write preview image {}", id, e);
			}
		});
	}

	@Override
	public byte[] read(String id) throws IOException {
		long startTime = System.nanoTime();

		CloudBlobContainer container = getContainer(id);
		try {
			CloudBlockBlob blob = container.getBlockBlobReference(id);
			if (!blob.exists()) return null;
			blob.downloadAttributes();
			int imageSize = (int) blob.getProperties().getLength();
			byte[] image = new byte[imageSize];
			blob.downloadToByteArray(image, 0);
			VIDEO_PREVIEW_AZURE_DOWNLOAD_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			return image;
		} catch (Exception e) {
			VIDEO_PREVIEW_AZURE_DOWNLOAD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			throw new IOException("Failed to read image " + id);
		}
	}

	private CloudBlobContainer getContainer(String id) {
		int index = id.hashCode() % containers.size();
		return containers.get(index);
	}

	@Override
	public void delete(String id) throws Exception {
		executor.execute(() -> { 		
			long startTime = System.nanoTime();
	
			CloudBlobContainer container = getContainer(id);
			try {
				CloudBlockBlob blob = container.getBlockBlobReference(id);
				blob.deleteIfExists();
				VIDEO_PREVIEW_AZURE_DELETE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				VIDEO_PREVIEW_AZURE_DELETE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
				log.error("Failed to delete preview image {}", id, e);
			}
		});
		
	}
}

