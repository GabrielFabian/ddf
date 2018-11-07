/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.opensearch.source;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolygonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolygonUtils.class);

  private PolygonUtils() {}

  public static Geometry convertPointRadiusToCircle1(PointRadius pointRadius, int numPoints) {
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
    shapeFactory.setNumPoints(numPoints);
    shapeFactory.setCentre(new Coordinate(pointRadius.getLon(), pointRadius.getLat()));
    shapeFactory.setSize((pointRadius.getRadius() * 0.000621371) * 2);
    return shapeFactory.createCircle();
  }
}
