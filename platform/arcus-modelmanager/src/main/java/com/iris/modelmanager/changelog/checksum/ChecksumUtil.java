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
package com.iris.modelmanager.changelog.checksum;

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import com.iris.modelmanager.changelog.CQLCommand;
import com.iris.modelmanager.changelog.ChangeSet;
import com.iris.modelmanager.changelog.Command;
import com.iris.modelmanager.changelog.JavaCommand;

public class ChecksumUtil {

   private ChecksumUtil() {
   }

   public static void updateChecksum(ChangeSet changeSet) {
      if(changeSet.getCommands().isEmpty()) {
         return;
      }

      MessageDigest checksum = DigestUtils.getMd5Digest();

      for(Command command : changeSet.getCommands()) {
         if(command instanceof CQLCommand) {
            DigestUtils.updateDigest(checksum, ((CQLCommand) command).getUpdateCql());
            DigestUtils.updateDigest(checksum, ((CQLCommand) command).getRollbackCql());
         } else if(command instanceof JavaCommand) {
            DigestUtils.updateDigest(checksum, ((JavaCommand) command).getClassName());
         }
      }

      byte[] bytes = checksum.digest();

      changeSet.setChecksum(Hex.encodeHexString(bytes));
   }

   public static void verifyChecksums(ChangeSet expected, ChangeSet actual) throws ChecksumInvalidException {
      if(!StringUtils.equals(expected.getChecksum(), actual.getChecksum())) {
         throw new ChecksumInvalidException(expected.getIdentifier(), expected.getChecksum(), actual.getChecksum());
      }
   }
}

