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
import java.io.InputStream;

/**
 * Class which can serialize objects of type {@code T}.  This class is
 * currently defined to handle binary serializers rather than character / String
 * serializers.
 */
public interface Deserializer<T> {

	/**
	 * Deserializes an object from a byte array.  This call assumes
	 * that the byte array contains a single object.  If the byte array
	 * is longer than the number of bytes required by the deserializer, the
	 * extra bytes will be ignored.  If it is too short and {@link IllegalArgumentException}
	 * will be thrown.
	 * @param input
	 * @return
	 * @throws IllegalArgumentException
	 */
	public T deserialize(byte[] input) throws IllegalArgumentException;

	/**
	 * Serializes value to the OutputStream.  This call MUST NOT close
	 * the input stream in order to allow multiple objects to be read from
	 * a single stream.
	 * @param input
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public T deserialize(InputStream input) throws IOException, IllegalArgumentException;

}

