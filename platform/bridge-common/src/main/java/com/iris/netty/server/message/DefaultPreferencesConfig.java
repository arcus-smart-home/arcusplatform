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
package com.iris.netty.server.message;

import static com.iris.messages.type.CardPreference.SERVICENAME_ALARMS;
import static com.iris.messages.type.CardPreference.SERVICENAME_CAMERAS;
import static com.iris.messages.type.CardPreference.SERVICENAME_CARE;
import static com.iris.messages.type.CardPreference.SERVICENAME_CLIMATE;
import static com.iris.messages.type.CardPreference.SERVICENAME_DOORS_N_LOCKS;
import static com.iris.messages.type.CardPreference.SERVICENAME_FAVORITES;
import static com.iris.messages.type.CardPreference.SERVICENAME_HISTORY;
import static com.iris.messages.type.CardPreference.SERVICENAME_HOME_N_FAMILY;
import static com.iris.messages.type.CardPreference.SERVICENAME_LAWN_N_GARDEN;
import static com.iris.messages.type.CardPreference.SERVICENAME_LIGHTS_N_SWITCHES;
import static com.iris.messages.type.CardPreference.SERVICENAME_SANTA_TRACKER;
import static com.iris.messages.type.CardPreference.SERVICENAME_WATER;
import static org.apache.commons.lang3.StringUtils.join;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class DefaultPreferencesConfig
{
   private static final boolean DEFAULT_HIDE_TUTORIALS = false;
   private static final boolean DEFAULT_HIDE_CARD = false;

   private static final String[] DEFAULT_DASHBOARD_CARDS_ORDER =
   {
      SERVICENAME_SANTA_TRACKER,
      SERVICENAME_FAVORITES,
      SERVICENAME_HISTORY,
      SERVICENAME_LIGHTS_N_SWITCHES,
      SERVICENAME_ALARMS,
      SERVICENAME_CLIMATE,
      SERVICENAME_DOORS_N_LOCKS,
      SERVICENAME_CAMERAS,
      SERVICENAME_CARE,
      SERVICENAME_HOME_N_FAMILY,
      SERVICENAME_LAWN_N_GARDEN,
      SERVICENAME_WATER
   };

   @Inject(optional = true) @Named("preferences.defaults.dashboardcards.order")
   private String dashboardCardOrder = join(DEFAULT_DASHBOARD_CARDS_ORDER, ',');
   @Inject(optional = true) @Named("preferences.defaults.dashboardcards.hide")
   private boolean dashboardCardsHidden = DEFAULT_HIDE_CARD;
   @Inject(optional = true) @Named("preferences.defaults.tutorials.hide")
   private boolean hideTutorials = DEFAULT_HIDE_TUTORIALS;

   private List<String> dashboardCardOrderList;

   @PostConstruct
   public void init() {
      this.dashboardCardOrderList = ImmutableList.copyOf(StringUtils.split(dashboardCardOrder, ','));
   }
   
   public String getDashboardCardOrder() {
      return dashboardCardOrder;
   }

   public void setDashboardCardOrder(String dashboardCardOrder) {
      this.dashboardCardOrder = dashboardCardOrder;
      this.dashboardCardOrderList = ImmutableList.copyOf(StringUtils.split(dashboardCardOrder, ','));
   }

   public boolean isDashboardCardsHidden() {
      return dashboardCardsHidden;
   }

   public void setDashboardCardsHidden(boolean dashboardCardsHidden) {
      this.dashboardCardsHidden = dashboardCardsHidden;
   }

   public boolean isHideTutorials() {
      return hideTutorials;
   }

   public void setHideTutorials(boolean hideTutorials) {
      this.hideTutorials = hideTutorials;
   }

   public List<String> getDashboardCardOrderList() {
      Preconditions.checkState(dashboardCardOrderList != null, "Must call init or set the card order first");
      return dashboardCardOrderList;
   }

}

