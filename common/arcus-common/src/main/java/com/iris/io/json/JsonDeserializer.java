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
package com.iris.io.json;

import java.io.IOException;
import java.io.Reader;

import com.iris.util.TypeMarker;

// TODO:  i'd like to get rid of this, it doesn't provide much value other than a bridge
// to GSON.
public interface JsonDeserializer {
   
   <T> T fromJson(String json, TypeMarker<T> type);
   
   <T> T fromJson(Reader json, TypeMarker<T> type) throws IOException;
   
   <T> T fromJson(String json, Class<T> clazz);

   <T> T fromJson(Reader json, Class<T> clazz) throws IOException;

}

