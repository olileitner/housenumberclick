package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

final class CornerSnapService {

    private static final double EPSILON = 1e-9;
    private static final double ENDPOINT_SNAP_TOLERANCE_METERS = 1.0;
    private static final double ENDPOINT_SNAP_TOLERANCE_DEGREES = 1e-5;

    IntersectionScanResult findSplitIntersections(Way buildingWay, LatLon lineStart, LatLon lineEnd) {
        if (buildingWay == null) {
            return IntersectionScanResult.failure("No building way provided");
        }
        if (lineStart == null || lineEnd == null) {
            return IntersectionScanResult.failure("Split line is not defined");
        }
        if (!buildingWay.isClosed()) {
            return IntersectionScanResult.failure("Building way must be closed");
        }

        List<Node> ringNodes = getOpenRingNodes(buildingWay);
        if (ringNodes.size() < 3) {
            return IntersectionScanResult.failure("Building outline is invalid");
        }

        List<IntersectionPoint> intersections = new ArrayList<>();
        for (int i = 0; i < ringNodes.size(); i++) {
            Node nodeA = ringNodes.get(i);
            Node nodeB = ringNodes.get((i + 1) % ringNodes.size());
            LatLon a = nodeA.getCoor();
            LatLon b = nodeB.getCoor();
            if (a == null || b == null) {
                continue;
            }

            SegmentIntersection segmentIntersection = intersectSegments(a, b, lineStart, lineEnd);
            if (segmentIntersection.type == IntersectionType.COLLINEAR_OVERLAP) {
                return IntersectionScanResult.failure("Split line overlaps building edge");
            }
            if (segmentIntersection.type != IntersectionType.POINT) {
                continue;
            }

            LatLon coordinate = segmentIntersection.intersection;
            Node existingNode = findNearestEndpointNode(coordinate, nodeA, nodeB);
            if (existingNode != null && existingNode.getCoor() != null) {
                coordinate = existingNode.getCoor();
            }

            addOrMerge(intersections, new IntersectionPoint(coordinate, i, existingNode, existingNode != null));
        }

        return IntersectionScanResult.success(intersections);
    }

    private List<Node> getOpenRingNodes(Way way) {
        List<Node> nodes = new ArrayList<>(way.getNodes());
        if (nodes.size() > 1 && nodes.get(0).equals(nodes.get(nodes.size() - 1))) {
            nodes.remove(nodes.size() - 1);
        }
        return nodes;
    }

    private void addOrMerge(List<IntersectionPoint> intersections, IntersectionPoint candidate) {
        for (int i = 0; i < intersections.size(); i++) {
            IntersectionPoint existing = intersections.get(i);
            if (!isSamePoint(existing.getCoordinate(), candidate.getCoordinate())) {
                continue;
            }
            if (!existing.isExistingNode() && candidate.isExistingNode()) {
                intersections.set(i, candidate);
            }
            return;
        }
        intersections.add(candidate);
    }

    private Node findNearestEndpointNode(LatLon intersectionCoordinate, Node nodeA, Node nodeB) {
        if (intersectionCoordinate == null) {
            return null;
        }
        LatLon a = nodeA == null ? null : nodeA.getCoor();
        LatLon b = nodeB == null ? null : nodeB.getCoor();

        double distanceToA = distanceBetween(intersectionCoordinate, a);
        double distanceToB = distanceBetween(intersectionCoordinate, b);

        boolean nearA = isWithinSnapTolerance(distanceToA);
        boolean nearB = isWithinSnapTolerance(distanceToB);

        if (!nearA && !nearB) {
            return null;
        }
        if (nearA && nearB) {
            return distanceToA <= distanceToB ? nodeA : nodeB;
        }
        return nearA ? nodeA : nodeB;
    }

    private boolean isWithinSnapTolerance(double distance) {
        if (!Double.isFinite(distance)) {
            return false;
        }
        if (ProjectionRegistry.getProjection() != null) {
            return distance <= ENDPOINT_SNAP_TOLERANCE_METERS;
        }
        return distance <= ENDPOINT_SNAP_TOLERANCE_DEGREES;
    }

    private double distanceBetween(LatLon first, LatLon second) {
        if (first == null || second == null) {
            return Double.POSITIVE_INFINITY;
        }

        if (ProjectionRegistry.getProjection() != null) {
            EastNorth firstEn = ProjectionRegistry.getProjection().latlon2eastNorth(first);
            EastNorth secondEn = ProjectionRegistry.getProjection().latlon2eastNorth(second);
            if (firstEn != null && secondEn != null) {
                return firstEn.distance(secondEn);
            }
        }

        double latDiff = first.lat() - second.lat();
        double lonDiff = first.lon() - second.lon();
        return Math.hypot(latDiff, lonDiff);
    }

    private SegmentIntersection intersectSegments(LatLon p1, LatLon p2, LatLon q1, LatLon q2) {
        double x1 = p1.lon();
        double y1 = p1.lat();
        double x2 = p2.lon();
        double y2 = p2.lat();
        double x3 = q1.lon();
        double y3 = q1.lat();
        double x4 = q2.lon();
        double y4 = q2.lat();

        double rX = x2 - x1;
        double rY = y2 - y1;
        double sX = x4 - x3;
        double sY = y4 - y3;

        double qpx = x3 - x1;
        double qpy = y3 - y1;
        double rCrossS = cross(rX, rY, sX, sY);
        double qMinusPCrossR = cross(qpx, qpy, rX, rY);

        if (Math.abs(rCrossS) <= EPSILON) {
            if (Math.abs(qMinusPCrossR) > EPSILON) {
                return SegmentIntersection.none();
            }

            LatLon sharedEndpoint = detectSharedEndpointTouch(p1, p2, q1, q2);
            if (sharedEndpoint != null) {
                return SegmentIntersection.point(sharedEndpoint);
            }

            if (hasCollinearSegmentOverlap(p1, p2, q1, q2)) {
                return SegmentIntersection.collinearOverlap();
            }
            return SegmentIntersection.none();
        }

        double t = cross(qpx, qpy, sX, sY) / rCrossS;
        double u = cross(qpx, qpy, rX, rY) / rCrossS;

        if (!isWithinSegmentParam(t) || !isWithinSegmentParam(u)) {
            return SegmentIntersection.none();
        }

        LatLon point = new LatLon(y1 + (t * rY), x1 + (t * rX));
        if (!isPointOnSegment(point, p1, p2) || !isPointOnSegment(point, q1, q2)) {
            return SegmentIntersection.none();
        }
        return SegmentIntersection.point(point);
    }

    private LatLon detectSharedEndpointTouch(LatLon p1, LatLon p2, LatLon q1, LatLon q2) {
        if (isSamePoint(p1, q1) || isSamePoint(p1, q2)) {
            return p1;
        }
        if (isSamePoint(p2, q1) || isSamePoint(p2, q2)) {
            return p2;
        }
        return null;
    }

    private boolean hasCollinearSegmentOverlap(LatLon p1, LatLon p2, LatLon q1, LatLon q2) {
        if (!isPointOnSegment(p1, q1, q2)
                && !isPointOnSegment(p2, q1, q2)
                && !isPointOnSegment(q1, p1, p2)
                && !isPointOnSegment(q2, p1, p2)) {
            return false;
        }

        double pDx = Math.abs(p2.lon() - p1.lon());
        double pDy = Math.abs(p2.lat() - p1.lat());
        if (pDx >= pDy) {
            return overlapLength(p1.lon(), p2.lon(), q1.lon(), q2.lon()) > EPSILON;
        }
        return overlapLength(p1.lat(), p2.lat(), q1.lat(), q2.lat()) > EPSILON;
    }

    private double overlapLength(double a1, double a2, double b1, double b2) {
        double minA = Math.min(a1, a2);
        double maxA = Math.max(a1, a2);
        double minB = Math.min(b1, b2);
        double maxB = Math.max(b1, b2);
        return Math.min(maxA, maxB) - Math.max(minA, minB);
    }

    private boolean isPointOnSegment(LatLon point, LatLon segmentStart, LatLon segmentEnd) {
        double px = point.lon();
        double py = point.lat();
        double ax = segmentStart.lon();
        double ay = segmentStart.lat();
        double bx = segmentEnd.lon();
        double by = segmentEnd.lat();

        double cross = cross(px - ax, py - ay, bx - ax, by - ay);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }

        double minX = Math.min(ax, bx) - EPSILON;
        double maxX = Math.max(ax, bx) + EPSILON;
        double minY = Math.min(ay, by) - EPSILON;
        double maxY = Math.max(ay, by) + EPSILON;
        return px >= minX && px <= maxX && py >= minY && py <= maxY;
    }

    private boolean isSamePoint(LatLon first, LatLon second) {
        if (first == null || second == null) {
            return false;
        }
        return Math.abs(first.lat() - second.lat()) <= EPSILON
                && Math.abs(first.lon() - second.lon()) <= EPSILON;
    }

    private boolean isWithinSegmentParam(double value) {
        return value >= -EPSILON && value <= 1.0 + EPSILON;
    }

    private double cross(double ax, double ay, double bx, double by) {
        return (ax * by) - (ay * bx);
    }

    private enum IntersectionType {
        NONE,
        POINT,
        COLLINEAR_OVERLAP
    }

    private static final class SegmentIntersection {
        private final IntersectionType type;
        private final LatLon intersection;

        private SegmentIntersection(IntersectionType type, LatLon intersection) {
            this.type = type;
            this.intersection = intersection;
        }

        private static SegmentIntersection none() {
            return new SegmentIntersection(IntersectionType.NONE, null);
        }

        private static SegmentIntersection point(LatLon intersection) {
            return new SegmentIntersection(IntersectionType.POINT, intersection);
        }

        private static SegmentIntersection collinearOverlap() {
            return new SegmentIntersection(IntersectionType.COLLINEAR_OVERLAP, null);
        }
    }
}


