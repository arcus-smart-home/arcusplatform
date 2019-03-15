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
package com.iris.common.alarm;

import java.util.Comparator;

public enum AlertType {
   CO, 
   SMOKE, 
   PANIC, 
   SECURITY, 
   CARE, 
   WATER, 
   WEATHER;
   
   public boolean isAutoConfirmed() {
      switch(this) {
      case CO:    
      case PANIC:    return true;
      default: return false;
      }
   }
   
   public static final Comparator<String> ALERT_PRIORITY_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return AlertType.valueOf(o1).compareTo(AlertType.valueOf(o2));
		}
	};
}

