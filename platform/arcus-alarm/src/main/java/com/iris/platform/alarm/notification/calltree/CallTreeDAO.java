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
package com.iris.platform.alarm.notification.calltree;

import com.iris.core.dao.PersonDAO;
import com.iris.messages.address.Address;
import com.iris.messages.model.Person;
import com.iris.messages.type.CallTreeEntry;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public interface CallTreeDAO {

   List<CallTreeEntry> callTreeForPlace(UUID placeId);

   default List<Person> enabledAsPeople(UUID placeId) {
      List<CallTreeEntry> callTreeList = callTreeForPlace(placeId);
      return callTreeList
         .stream()
         .filter((e) -> e != null && Boolean.TRUE.equals(e.getEnabled()))
         .map((e) -> personDAO().findById((UUID) (Address.fromString(e.getPerson()).getId())))
         .filter(Objects::nonNull)
         .collect(Collectors.toList());
   }

   PersonDAO personDAO();

}

