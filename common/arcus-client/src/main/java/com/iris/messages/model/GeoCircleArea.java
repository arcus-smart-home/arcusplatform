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
package com.iris.messages.model;

public class GeoCircleArea implements GeoArea {

	private GeoCoord centroid;
	private double radius;

	public GeoCircleArea(GeoCoord centroid, double radius) {
		this.centroid = centroid;
		this.radius = radius;
	}

	@Override
	public boolean containsPoint(GeoCoord point) {
		double dx = point.getLongitude() - centroid.getLongitude();
		double dy = point.getLatitude() - centroid.getLatitude();
		double r2 = radius * radius;
		dx *= dx;
		dy *= dy;
		return ( (dx + dy) < r2 );
	}

	@Override
	public GeoCoord getCentroid() {
		return centroid;
	}

	public double getRadius() {
		return radius;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((centroid == null) ? 0 : centroid.hashCode());
		long temp;
		temp = Double.doubleToLongBits(radius);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		GeoCircleArea other = (GeoCircleArea) obj;
		if (centroid == null) {
			if (other.centroid != null)
				return false;
		} else if (!centroid.equals(other.centroid))
			return false;
		if (Double.doubleToLongBits(radius) != Double
				.doubleToLongBits(other.radius))
			return false;
		return true;
	}

   @Override
   public GeoArea copy() {
      try {
         return (GeoArea) clone();
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      GeoCircleArea area = (GeoCircleArea) super.clone();
      area.centroid = this.centroid == null ? null : this.centroid.copy();
      return area;
   }


}

