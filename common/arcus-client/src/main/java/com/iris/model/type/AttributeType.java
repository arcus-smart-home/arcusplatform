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
package com.iris.model.type;

import java.lang.reflect.Type;

public interface AttributeType {

	public Class<?> getJavaType();
	public String getTypeName();
	public Object coerce(Object obj);
   /**
    * This should return {@code true} if {@link #coerce(Object)}
    * would not throw an exception.
    * @param returnType
    * @return
    */
	public boolean isAssignableFrom(Type type);

}

