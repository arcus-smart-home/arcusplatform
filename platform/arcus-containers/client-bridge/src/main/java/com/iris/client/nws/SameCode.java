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
package com.iris.client.nws;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;

public class SameCode implements Comparable<SameCode> {

	/*
	 * The code is a Stringified version of numerical code with leading zeroes
	 * This helps in sorting on a natural order based on the numerical value
	 * 
	 * It turns out that NWS assigns codes to stations in a manner that provides
	 * a Lexical sort order based on the station type and location. This is a
	 * FIPS code mapping
	 */
	private final String code;

	private final String county;
	private final String state;
	private final String stateCode;

	/*
	 * We are going to be sorting based on the numerical value of the
	 * Stringified code and not the lexical ordering. Thus I will maintain an
	 * internal ID which will be used for comparTo. This requires that comparTo
	 * and equals are consistent for storage in sorted maps.
	 * 
	 * i.e Maps are defined in terms of the equals operation but sorted maps use
	 * the compareTo for or compare for comparisons.
	 * 
	 * In order to keep ordering consistent with equals I will maintain this
	 * internal ID.
	 * 
	 * The class itself is immutable so it need be set only once.
	 */
	private final BigInteger id;

	public SameCode(String code, String county, String stateCode, String state) {
		// do validations
		Utils.assertNotEmpty(county, "County cannot be null or empty");
		Utils.assertNotEmpty(state, "State cannot be null or empty");
		Utils.assertNotEmpty(stateCode, "State Code cannot be null or empty");

		Utils.assertTrue(!StringUtils.isEmpty(code) && StringUtils.isNumeric(code),
				"SAME code cannot be null or empty AND must be numeric in nature");

		this.code = code;
		this.county = county;
		this.state = state;
		this.stateCode = stateCode;

		// BigInteger will strip out the leading zeros to form an int
		this.id = new BigInteger(code);
	}

	public String getCode() {
		return code;
	}

	public String getCounty() {
		return county;
	}

	public String getState() {
		return state;
	}

	public String getStateCode() {
		return stateCode;
	}

	/*
	 * Sort based on the NWR SAME Code Column:
	 * 
	 * This is a 6-digit sequence uniquely describes each county.
	 * 
	 * For coding of a whole county, the first digit is zero. For coding of a
	 * part of a county, the first digit is a non-zero number.
	 * 
	 * The 2nd through 6th digits use the INCITS 31 - 2009 FIPS codes. The 2nd
	 * and 3rd digits are the 2-digit state/equivalent territory identifier; the
	 * last three digits are the county or equivalent area identifier.
	 * 
	 * These codes allow us to sort stations based on geographical locations on
	 * a state by state and county by county manner. The county mappings turn
	 * out to be lexical in order.
	 * 
	 * e.g. A Sample using Alabama which has a FIPS based state code of 01 would
	 * be assigned and sorted as follows:
	 * 
	 * 001001,Autauga, AL 001003,Baldwin, AL 001005,Barbour, AL 001007,Bibb, AL
	 * 001009,Blount, AL 001011,Bullock, AL
	 * 
	 * Florida with a FIPS code of 12
	 * 
	 * 012095,Orange, FL 012097,Osceola, FL 012099,Palm Beach, FL 012101,Pasco,
	 * FL 012103,Pinellas, FL
	 * 
	 * See {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt} 
	 * 
	 * See {@linktourl https://www.census.gov/geo/reference/codes/cou.html}
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SameCode o) {

		if (this.id.equals(o.id) && !this.equals(o)) {
			/*
			 * THIS SHOULD NEVER HAPPEN!
			 * 
			 * This would be a data integrity issue.
			 *  
			 * SAME Codes should be UNIQUE. The question is
			 * whether or not we should enforce the uniqueness here.
			 */
			throw new IllegalArgumentException(String.format(
					"Two conflicting NWS SAME Code instances exist with the same SAME Code value %s and %s",
					this.toString(), o.toString()));
		}

		// will facilitate ascending based sorting
		return this.id.compareTo(o.id);
	}
	
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((code == null) ? 0 : code.hashCode());
      result = prime * result + ((county == null) ? 0 : county.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result + ((stateCode == null) ? 0 : stateCode.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      SameCode other = (SameCode) obj;
      if (code == null) {
         if (other.code != null)
            return false;
      } else if (!code.equals(other.code))
         return false;
      if (county == null) {
         if (other.county != null)
            return false;
      } else if (!county.equals(other.county))
         return false;
      if (id == null) {
         if (other.id != null)
            return false;
      } else if (!id.equals(other.id))
         return false;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (stateCode == null) {
         if (other.stateCode != null)
            return false;
      } else if (!stateCode.equals(other.stateCode))
         return false;
      return true;
   }
   
   @Override
   public String toString() {
      return "SameCode [code=" + code + ", county=" + county + ", state=" + state + ", stateCode=" + stateCode + "]";
   }  
}

