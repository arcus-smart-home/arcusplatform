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
package com.iris.client.service;

import java.util.Collection;

import com.iris.client.model.device.ClientDeviceModel;

/**
 * 
 * Clients should implement this interface as a base contract for
 * services requiring access to the caching mechanism used.
 *
 */
public interface ClientCachingService {
	/**
	 * Load item from cache.
	 * 
	 * @return item(s) from cache or empty collection
	 */
	public <C> Collection<C> loadCache(Class<?> clazz);

	/**
	 * Saves an item to cache.
	 * 
	 * @return true if the item was successfully saved to cache, false if not.
	 */
	public <C> boolean saveToCache(C itemToSave);

	/**
	 * Load devices from cache.
	 * 
	 * @return item(s) from cache or empty collection
	 */
	public <D extends ClientDeviceModel> Collection<D> loadDeviceCache();

	/**
	 * Load preferences from cache.
	 * 
	 * @return item(s) from cache or empty collection
	 */
	public <P> Collection<P> loadPreferenceCache();

	/**
	 * 
	 * Load a single item from the cache.
	 * 
	 * @param key
	 * @param clazz
	 * 
	 * @return the item from the cache or null.
	 */
	public <I> I loadItemFromCache(String key, Class<?> clazz);
}

