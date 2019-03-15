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
package com.iris.notification.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;
import com.iris.notification.dispatch.DispatchUnsupportedByUserException;
import com.iris.platform.notification.Notification;

public abstract class AbstractPushNotificationProvider implements NotificationProvider {

    @Inject
    private PersonDAO personDao;

    @Inject
    private MobileDeviceDAO mobileDeviceDao;

    @Override
   public boolean supportedByUser(Notification notification) {
       if(listMobileDevicesForPerson(notification.getPersonId()).size()==0){
          return false;
       }
       return true;
    }

   public List<String> listMobileDevicesForPerson(UUID personId) {
       return mobileDeviceDao.listForPerson(personId).stream().map(d->d.getAddress()).collect(Collectors.toList());
    }
    
    protected List<String> getDeviceTokensForPerson(UUID personId, MobileOS osType) throws DispatchUnsupportedByUserException {
        List<String> deviceTokens = new ArrayList<String>();

        Person recipient = personDao.findById(personId);
        if (recipient == null) {
            throw new DispatchUnsupportedByUserException("No person found with id: " + personId);
        }

        List<MobileDevice> mobileDevices = mobileDeviceDao.listForPerson(recipient);
        if (mobileDevices == null || mobileDevices.size() == 0) {
            throw new DispatchUnsupportedByUserException("No mobile devices associated with person id: " + personId);
        }

        for (MobileDevice thisDevice : mobileDevices) {
            if (osType.toString().equalsIgnoreCase(thisDevice.getOsType()) && !StringUtils.isBlank(thisDevice.getNotificationToken())) {
                deviceTokens.add(thisDevice.getNotificationToken());
            }
        }

        return deviceTokens;
    }

    public static enum MobileOS {
        IOS, ANDROID;
    }
}

