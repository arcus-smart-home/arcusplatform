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
package com.iris.agent.db;

import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.Nullable;

import com.almworks.sqlite4java.SQLiteConnection;

public interface DbTask<V> extends Future<V> {
   @Nullable
   V execute(SQLiteConnection conn) throws Exception;

   void set(@Nullable V value);
   void setException(Throwable cause);
}

