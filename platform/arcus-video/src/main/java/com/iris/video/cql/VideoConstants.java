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
package com.iris.video.cql;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public interface VideoConstants {
   public static final double REC_TS_START = Double.NEGATIVE_INFINITY;
   public static final double REC_TS_END = Double.POSITIVE_INFINITY;
   // if we haven't received an iframe for this long assume the recording is done
   public static final long MAX_IFRAME_QUIET_PERIOD_MS = TimeUnit.MINUTES.toMillis(5);
   
   public static final Date PURGE_TABLE_METADATA_DATE = new Date(0);
	public static final long PURGE_TABLE_METADATA_UUID_RANDOM = 0x6221E031CA7E19BAL;
	
	public static final String TAG_FAVORITE = "FAVORITE";
	public static final String ATTR_TAG_PREFIX = "tag:";
	
	public static final Date DELETE_TIME_SENTINEL = new Date(0);
	public static final int DELETION_PARTITION_UNKNOWN = -1;
}

