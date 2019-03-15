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
package com.iris.billing.client.model;

public class Constants {
   // Why are these here instead of in ServiceLevel?
   public static final String PLAN_CODE_BASIC                           = "basic";
   public static final String PLAN_CODE_PREMIUM                         = "premium";
   public static final String PLAN_CODE_PREMIUM_FREE                    = "premium_free";
   public static final String PLAN_CODE_PREMIUM_PROMON                  = "premium_promon";
   public static final String PLAN_CODE_PREMIUM_PROMON_FREE             = "premium_promon_free";
   public static final String PLAN_CODE_PREMIUM_ANNUAL                  = "premium_annual";
   public static final String PLAN_CODE_PREMIUM_PROMON_ANNUAL           = "premium_promon_annual";

   public static final String CURRENCY_USD = "USD";
   
   public static final String ADDON_CODE_CARE = "care";
   public static final String ADDON_CODE_CELL_BACKUP = "CellBackupPremium";
   public static final String ADDON_CODE_CELL_PRIMARY = "CellPrimaryPremium";
   public static final String ADDON_CODE_EXTRA_VIDEO_STORAGE_1GB = "extravideo1";
   public static final String ADDON_CODE_EXTRA_VIDEO_STORAGE_5GB = "extravideo5";
   
   public static final String STATE_ACTIVE = "active";
   public static final String STATE_CANCELED = "canceled";
   public static final String STATE_EXPIRED = "expired";
   public static final String STATE_FUTURE = "future";
   public static final String STATE_IN_TRIAL = "in_trial";
   public static final String STATE_LIVE = "live";
   public static final String STATE_PAST_DUE = "past_due";
}

