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
package com.iris.resource.manager;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

public abstract class BaseCsvParser<T> implements ResourceParser<T>
{
   private static final Logger logger = getLogger(BaseCsvParser.class);

   @Override
   public T parse(InputStream in)
   {
      logger.trace("Parsing CSV file");

      final int startingLine = getStartingLine();

      try (CSVReader reader = new CSVReader(new InputStreamReader(in), startingLine, new CSVParser()))
      {
         T result = newResult();

         int expectedLength = getExpectedLength();

         String[] nextLine;

         for (int i = startingLine; (nextLine = reader.readNext()) != null; i++)
         {
            if (nextLine.length < expectedLength)
            {
               if (nextLine.length == 1 && isBlank(nextLine[0]))
               {
                  // Ignore blank lines
                  continue;
               }
               else
               {
                  throw new IllegalStateException(format("Invalid length %d on line %d", nextLine.length, i));
               }
            }

            processLine(nextLine, result, i);
         }

         return finalizeResult(result);
      }
      catch (IOException e)
      {
         logger.warn("Error parsing CSV file", e);

         throw new IllegalStateException(e);
      }
   }

   protected int getStartingLine()
   {
      return 1;
   }

   protected abstract T newResult();

   protected abstract int getExpectedLength();

   protected abstract void processLine(String[] nextLine, T result, int lineNumber);

   protected abstract T finalizeResult(T result);
}

