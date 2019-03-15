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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A closed polygon of geographic coordinates
 * where coordinates[n] = coordinates[0]
 * @author sperry
 *
 */
public class GeoPolygonArea implements GeoArea {

	// list of vertices of the polygon where coordinates[n] = coordinates[0]
	private List<GeoCoord> coordinates;
	private GeoCoord centroid;
	private double minLatitude = 180.0d;
	private double minLongitude = 180.0d;
	private double maxLatitude = -180.0d;
	private double maxLongitude = -180.0d;
	private List<GeoCoord> boundingBox;

	public GeoPolygonArea(GeoCoord... coordinates) {
		this.coordinates = Arrays.asList(coordinates);
		if (!this.coordinates.get(0).equals(this.coordinates.get(this.coordinates.size() - 1)))
			this.coordinates.add(this.coordinates.get(0));
		this.boundingBox = _calcBoundingBox(this.coordinates);
		this.centroid = _calcCentroid(this.coordinates);
	}

	public List<GeoCoord> getCoordinates() {
		return new ArrayList<GeoCoord>(coordinates);
	}

	public double getMinLatitude() {
		return minLatitude;
	}

	public double getMinLongitude() {
		return minLongitude;
	}

	public double getMaxLatitude() {
		return maxLatitude;
	}

	public double getMaxLongitude() {
		return maxLongitude;
	}

	public List<GeoCoord> getBoundingBox() {
		return new ArrayList<GeoCoord>(boundingBox);
	}

	@Override
	public boolean containsPoint(GeoCoord point) {
		// do quick check against bounding box -- if not in box cannot be in poly
		if (point.getLatitude() > maxLatitude ||
		    point.getLatitude() < minLatitude ||
		    point.getLongitude() > maxLongitude ||
		    point.getLongitude() < minLongitude)
		{
			return false;
		} else {
			return _wn(point, coordinates) > 0;
		}
	}


	@Override
	public GeoCoord getCentroid() {
		return centroid;
	}



	private double _isLeft(GeoCoord p0, GeoCoord p1, GeoCoord p2) {
		return ( (p1.getLongitude() - p0.getLongitude()) *
				 (p2.getLatitude() - p0.getLatitude() )  -
				 (p2.getLongitude() - p0.getLongitude()) *
				 (p1.getLatitude() - p0.getLatitude() )
				);
	}

	private int _wn(GeoCoord p, List<GeoCoord> poly) {
		int wn = 0;

		for (int i = 0; i < poly.size(); i++) {
			if (poly.get(i).getLatitude() <= p.getLatitude()) {
				if (poly.get(i+1).getLatitude() > p.getLatitude()) {
					if (_isLeft(poly.get(i), poly.get(i+1), p) > 0) {
						++wn;
					}
				}
			} else {
				if (poly.get(i+1).getLatitude() <= p.getLatitude()) {
					if (_isLeft(poly.get(i), poly.get(i+1), p) < 0) {
						--wn;
					}
				}
			}
		}
		return wn;
	}


	private List<GeoCoord> _calcBoundingBox(List<GeoCoord> poly) {

		for (GeoCoord c : poly) {
			if (c.getLatitude() < minLatitude) {
				minLatitude = c.getLatitude();
			}
			if (c.getLatitude() > maxLatitude) {
				maxLatitude = c.getLatitude();
			}
			if (c.getLongitude() < minLongitude) {
				minLongitude = c.getLongitude();
			}
			if (c.getLongitude() > maxLongitude) {
				maxLongitude = c.getLongitude();
			}
		}
		// construct bounding box
		return Arrays.asList(
				new GeoCoord(maxLatitude, minLongitude),
				new GeoCoord(maxLatitude, maxLongitude),
				new GeoCoord(minLatitude, maxLongitude),
				new GeoCoord(minLatitude, minLongitude));
	}

	private GeoCoord _calcCentroid(List<GeoCoord> poly) {
		double cLat = 0.0;
		double cLong = 0.0;
		double area = _calcArea(poly);

		int in = 1;
		double t;
		for (int i = 0; i < poly.size(); i++) {
			t = poly.get(i).getLongitude() * poly.get(in).getLatitude() - poly.get(in).getLongitude() * poly.get(i).getLatitude();
			cLong += (poly.get(i).getLongitude() + poly.get(in).getLongitude()) * t;
			cLat += (poly.get(i).getLatitude() + poly.get(in).getLatitude()) * t;
			in = (in + 1) % poly.size();
		}
		cLong = cLong / (6.0 * area);
		cLat = cLat / (6.0 * area);
		return new GeoCoord(cLat, cLong);
	}

	private double _calcArea(List<GeoCoord> poly) {
		double area = 0.0;
		int in = 1;
		for (int i = 0; i < poly.size(); i++) {
			area += poly.get(i).getLongitude() * poly.get(in).getLatitude() - poly.get(in).getLongitude() * poly.get(i).getLatitude();
			in = (in+1) % poly.size();
		}
		area *= 0.5;
		return area;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((coordinates == null) ? 0 : coordinates.hashCode());
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
		GeoPolygonArea other = (GeoPolygonArea) obj;
		if (coordinates == null) {
			if (other.coordinates != null)
				return false;
		} else if (!coordinates.equals(other.coordinates))
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
      GeoPolygonArea geoArea = (GeoPolygonArea) super.clone();

      if(this.boundingBox != null) {
         geoArea.boundingBox = new LinkedList<GeoCoord>();
         deepCopyGeoCoordList(this.boundingBox, geoArea.boundingBox);
      }

      if(this.coordinates != null) {
         geoArea.coordinates = new LinkedList<GeoCoord>();
         deepCopyGeoCoordList(this.coordinates, geoArea.coordinates);
      }

      geoArea.centroid = this.centroid == null ? null : this.centroid.copy();
      return geoArea;
   }

   private void deepCopyGeoCoordList(List<GeoCoord> source, List<GeoCoord> dest) {
      for(GeoCoord coord : source) {
         dest.add(coord.copy());
      }
   }
}

