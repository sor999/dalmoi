package com.sor999.dalmoi.course.service;

import com.sor999.dalmoi.course.dto.CoordinateRequest;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoSupport {

    public static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static LineString route(List<CoordinateRequest> points) {
        return FACTORY.createLineString(
                points.stream().map(p -> new Coordinate(p.longitude(), p.latitude())).toArray(Coordinate[]::new));
    }

    public static List<CoordinateRequest> points(LineString route) {
        return Arrays.stream(route.getCoordinates()).map(p -> new CoordinateRequest(p.y, p.x)).toList();
    }

    public static double distance(Coordinate a, Coordinate b) {
        double lat = Math.toRadians(b.y - a.y);
        double lon = Math.toRadians(b.x - a.x);
        double h = Math.pow(Math.sin(lat / 2), 2)
                + Math.cos(Math.toRadians(a.y)) * Math.cos(Math.toRadians(b.y)) * Math.pow(Math.sin(lon / 2), 2);

        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, h)));
    }

    public static double length(LineString route) {
        double meters = 0;

        for (int i = 1; i < route.getNumPoints(); i++) {
            meters += distance(route.getCoordinateN(i - 1), route.getCoordinateN(i));
        }

        return meters;
    }

    public static double km(double meters) {
        return Math.round(meters / 10.0) / 100.0;
    }
}
