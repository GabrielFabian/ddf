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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolygonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolygonUtils.class);

  private PolygonUtils() {}

  public static Geometry convertPointRadiusToCirclePolygon(
      PointRadius pointRadius, int maxVerticies, int distanceTolerance) {
    Measure measure = Measure.valueOf(pointRadius.getRadius(), SI.METER);
    Point jtsPoint =
        new GeometryFactory()
            .createPoint(new Coordinate(pointRadius.getLon(), pointRadius.getLat()));

    TopologyPreservingSimplifier simplifier =
        new TopologyPreservingSimplifier(
            createBufferedCircleFromPoint(measure, DefaultGeographicCRS.WGS84, jtsPoint));

    int maxVerticiesWithClosedPoint = maxVerticies + 1;

    while (simplifier.getResultGeometry().getCoordinates().length > maxVerticiesWithClosedPoint) {
      simplifier.setDistanceTolerance(distanceTolerance += distanceTolerance);
    }

    return simplifier.getResultGeometry();
  }

  private static Geometry createBufferedCircleFromPoint(
      Measure<Double, Length> distance, CoordinateReferenceSystem origCRS, Geometry point) {
    Geometry pGeom = point;
    MathTransform toTransform, fromTransform = null;

    Unit<Length> unit = distance.getUnit();
    if (!(origCRS instanceof ProjectedCRS)) {

      double x = point.getCoordinate().x;
      double y = point.getCoordinate().y;

      String crsCode = "AUTO:42001," + x + "," + y;

      CoordinateReferenceSystem utmCrs;

      try {
        utmCrs = CRS.decode(crsCode);
        toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, utmCrs);
        fromTransform = CRS.findMathTransform(utmCrs, DefaultGeographicCRS.WGS84);
        pGeom = JTS.transform(point, toTransform);
        return JTS.transform(pGeom.buffer(distance.doubleValue(SI.METER)), fromTransform);
      } catch (MismatchedDimensionException | TransformException | FactoryException e) {
        LOGGER.debug("Unable to transform original CRS to UTM.", e);
      }
    } else {
      unit = (Unit<Length>) origCRS.getCoordinateSystem().getAxis(0).getUnit();
    }

    return pGeom.buffer(distance.doubleValue(unit));
  }
}
