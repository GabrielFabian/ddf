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
      PointRadius pointRadius, int maxVerticies) {
    double distanceTolerance = 100;
    Measure measure = Measure.valueOf(pointRadius.getRadius(), SI.METER);
    Point jtsPoint =
        new GeometryFactory()
            .createPoint(new Coordinate(pointRadius.getLon(), pointRadius.getLat()));

    TopologyPreservingSimplifier simplifier =
        new TopologyPreservingSimplifier(
            bufferPoint(measure, DefaultGeographicCRS.WGS84, jtsPoint));

    while (simplifier.getResultGeometry().getCoordinates().length > maxVerticies + 1) {
      simplifier.setDistanceTolerance(distanceTolerance += distanceTolerance);
    }

    return simplifier.getResultGeometry();
  }

  public static Geometry bufferPoint(
      Measure<Double, Length> distance, CoordinateReferenceSystem origCRS, Geometry geom) {
    Geometry pGeom = geom;
    MathTransform toTransform, fromTransform = null;

    // project the geometry as UTM so that a buffer can be accurately applied
    Unit<Length> unit = distance.getUnit();
    if (!(origCRS instanceof ProjectedCRS)) {

      double x = geom.getCoordinate().x;
      double y = geom.getCoordinate().y;

      String code = "AUTO:42001," + x + "," + y;

      CoordinateReferenceSystem auto;

      try {
        auto = CRS.decode(code);
        toTransform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, auto);
        fromTransform = CRS.findMathTransform(auto, DefaultGeographicCRS.WGS84);
        pGeom = JTS.transform(geom, toTransform);
        unit = SI.METER;
      } catch (MismatchedDimensionException | TransformException | FactoryException e) {
        LOGGER.debug("e");
        e.printStackTrace();
      }

    } else {
      unit = (Unit<Length>) origCRS.getCoordinateSystem().getAxis(0).getUnit();
    }

    // buffer
    Geometry out = pGeom.buffer(distance.doubleValue(unit));
    Geometry retGeom = out;
    // reproject the geometry to the original projection
    if (!(origCRS instanceof ProjectedCRS)) {
      try {
        retGeom = JTS.transform(out, fromTransform);

      } catch (MismatchedDimensionException | TransformException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return retGeom;
  }
}
