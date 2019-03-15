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
package com.iris.platform.ivr.pronounce;

import static com.google.common.collect.Lists.reverse;
import static com.iris.platform.ivr.pronounce.ParsedStreetAddressLine.Token.Type.DIRECTIONAL;
import static com.iris.platform.ivr.pronounce.ParsedStreetAddressLine.Token.Type.OTHER;
import static com.iris.platform.ivr.pronounce.ParsedStreetAddressLine.Token.Type.SECONDARY_UNIT;
import static com.iris.platform.ivr.pronounce.ParsedStreetAddressLine.Token.Type.STREET_SUFFIX;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.split;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.iris.platform.ivr.pronounce.ParsedStreetAddressLine.Token;
import com.iris.platform.location.StreetSuffix;
import com.iris.platform.location.UspsDataService;

/**
 * Uses USPS standard abbreviations to expand directionals, street suffixes, and secondary units in a street address
 * line, so that IVR services can pronounce the line more accurately.
 * <p>
 * Parses the line in two passes:
 * <ol>
 * <li>First looks for the "anchor" tokens ({@code STREET_SUFFIX} and {@code SECONDARY_UNIT})</li>
 *    <ul>
 *    <li>Searches from the end of the line, in case there are false matches in the street name
 *        (e.g. {@code "Ave Maria St"} should become {@code "Ave Maria Street"})</li>
 *    </ul>
 * <li>Then looks for {@code DIRECTIONAL}s</li>
 *    <ul>
 *    <li>Searches context-sensitively and only expands {@code DIRECTIONAL}s that are <strong>not</strong> immediately
 *        before a {@code STREET_SUFFIX} or immediately after a {@code SECONDARY_UNIT}, to prevent false matches
 *        (e.g. {@code "1 N N St Apt N"} should become {@code "1 North N St Apt N"}, and
 *        {@code "1 N St N Apt N"} should become {@code "1 N St North Apt N"})</li>
 *    </ul>
 * </ol>
 * Then expands the tokens that it marked as {@code DIRECTIONAL}s, {@code STREET_SUFFIX}es, or {@code SECONDARY_UNIT}s.
 * 
 * @author Dan Ignat
 */
public class AddressLinePronouncer
{
   private final UspsDataService uspsDataService;

   public AddressLinePronouncer(UspsDataService uspsDataService)
   {
      this.uspsDataService = uspsDataService;
   }

   public String pronounce(String line)
   {
      if (isBlank(line)) return line;

      String normalizedLine = line.toUpperCase();

      ParsedStreetAddressLine parsedAddressLine = new ParsedStreetAddressLine(normalizedLine);

      setAnchorTokenTypes(parsedAddressLine);

      setRemainingTokenTypes(parsedAddressLine);

      pronounceTokenValues(parsedAddressLine);

      return parsedAddressLine.join();
   }

   private void setAnchorTokenTypes(ParsedStreetAddressLine parsedAddressLine)
   {
      for (Token token : reverse(parsedAddressLine.getTokens()))
      {
         if (!parsedAddressLine.hasFoundSecondaryUnit() &&
            uspsDataService.getSecondaryUnits().containsKey(token.getValue()))
         {
            token.setType(SECONDARY_UNIT);

            parsedAddressLine.setFoundSecondaryUnit();
         }
         else if (!parsedAddressLine.hasFoundStreetSuffix() &&
            uspsDataService.isStreetSuffix(token.getValue()))
         {
            token.setType(STREET_SUFFIX);

            parsedAddressLine.setFoundStreetSuffix();
         }
      }
   }

   private void setRemainingTokenTypes(ParsedStreetAddressLine parsedAddressLine)
   {
      List<Token> tokens = parsedAddressLine.getTokens();

      for (int i = 0; i < tokens.size(); i++)
      {
         Token token = tokens.get(i);

         if (!token.hasType())
         {
            Token previousToken = i > 0 ? tokens.get(i - 1) : null;

            Token nextToken = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

            if ((nextToken == null || nextToken.getType() != STREET_SUFFIX) &&
               (previousToken == null || previousToken.getType() != SECONDARY_UNIT) &&
               uspsDataService.getDirectionals().containsKey(token.getValue()))
            {
               token.setType(DIRECTIONAL);
            }
            else
            {
               token.setType(OTHER);
            }
         }
      }
   }

   private void pronounceTokenValues(ParsedStreetAddressLine parsedAddressLine)
   {
      for (Token token : parsedAddressLine.getTokens())
      {
         switch (token.getType())
         {
            case DIRECTIONAL:

               String pronouncedDirectional = uspsDataService.getDirectionals().get(token.getValue());

               token.setValue(pronouncedDirectional);

               break;

            case STREET_SUFFIX:

               StreetSuffix streetSuffix = uspsDataService.getStreetSuffixesByCode().get(token.getValue());

               if (streetSuffix == null)
               {
                  streetSuffix = uspsDataService.getStreetSuffixesByVariant().get(token.getValue());
               }

               if (streetSuffix != null)
               {
                  token.setValue(streetSuffix.getName());
               }

               break;

            case SECONDARY_UNIT:

               String pronouncedSecondaryUnit = uspsDataService.getSecondaryUnits().get(token.getValue());

               token.setValue(pronouncedSecondaryUnit);

               break;

            case OTHER:

               break;

            default:
         }
      }
   }
}

class ParsedStreetAddressLine
{
   private List<Token> tokens = new ArrayList<>();

   private boolean foundStreetSuffix;
   private boolean foundSecondaryUnit;

   public ParsedStreetAddressLine(String addressLine)
   {
      for (String tokenValue : split(addressLine))
      {
         tokens.add(new Token(tokenValue));
      }
   }

   public List<Token> getTokens()
   {
      return tokens;
   }

   public boolean hasFoundStreetSuffix()
   {
      return foundStreetSuffix;
   }

   public void setFoundStreetSuffix()
   {
      foundStreetSuffix = true;
   }

   public boolean hasFoundSecondaryUnit()
   {
      return foundSecondaryUnit;
   }

   public void setFoundSecondaryUnit()
   {
      foundSecondaryUnit = true;
   }

   public String join()
   {
      List<String> tokenValues = tokens.stream().map(t -> t.getValue()).collect(toList());

      return StringUtils.join(tokenValues, ' ');
   }

   static class Token
   {
      private String value;
      private Type type;

      public Token(String value)
      {
         this.value = value;
      }

      public String getValue()
      {
         return value;
      }

      public void setValue(String value)
      {
         this.value = value;
      }

      public Type getType()
      {
         return type;
      }

      public boolean hasType()
      {
         return type != null;
      }

      public void setType(Type type)
      {
         this.type = type;
      }

      enum Type
      {
         DIRECTIONAL,
         STREET_SUFFIX,
         SECONDARY_UNIT,
         OTHER
      };
   }
}

