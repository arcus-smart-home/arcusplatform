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
package com.iris.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class which can serialize objects of type {@code T}.  This class is
 * currently defined to handle binary serializers rather than character / String
 * serializers.
 */
public interface Serializer<T> {

	/**
	 * Serializes a value to a byte array.  If the value
	 * is not serializable, this throws an {@link IllegalArgumentException}.
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 */
	public byte[] serialize(T value) throws IllegalArgumentException;

	/**
	 * Serializes value to the OutputStream.  This call MUST NOT close
	 * the output stream in order to allow multiple objects to be written.
	 * @param value
	 * @param out
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public void serialize(T value, OutputStream out) throws IOException, IllegalArgumentException;

}

