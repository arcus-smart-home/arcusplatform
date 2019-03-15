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
package com.iris.resource.manager;

import static com.google.common.base.Charsets.UTF_8;
import static com.iris.io.json.JSON.fromJson;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.iris.util.TypeMarker;

public abstract class BaseJsonParser<T> implements ResourceParser<T>
{
   @Override
   public T parse(InputStream input)
   {
      try
      {
         return fromJson(IOUtils.toString(input, UTF_8), getTypeMarker());
      }
      catch (IOException e)
      {
         throw new IllegalStateException("Unable to load JSON resource", e);
      }
   }

   protected abstract TypeMarker<T> getTypeMarker();
}

