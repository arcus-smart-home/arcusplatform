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
package com.iris.platform.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.lang3.StringUtils;

import com.opencsv.CSVWriter;

public class CSVWriterHelper {
	public static CSVWriter createOutputWriter(String filePath) throws FileNotFoundException {
		OutputStream os = null;
		if(StringUtils.isNotBlank(filePath)) {
			os = new FileOutputStream(filePath) ;
		}else {
			os = System.out;
		}
		return new CSVWriter(new OutputStreamWriter(os));
	}
}

