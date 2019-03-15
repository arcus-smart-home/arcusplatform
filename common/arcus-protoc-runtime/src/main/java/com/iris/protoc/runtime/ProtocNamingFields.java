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
package com.iris.protoc.runtime;

import java.util.Map;

public interface ProtocNamingFields {
   Map<String,Class<?>> getFields();
   ProtocStruct create(Map<String,Object> context);

   boolean isClusterSpecific();
   boolean isFromServer();

   int getMessageId();
   int getSize(String field, Map<String,Object> context);
   int getOffset(String field, Map<String,Object> context);
}

