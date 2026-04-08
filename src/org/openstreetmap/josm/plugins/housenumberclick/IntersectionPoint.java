package org.openstreetmap.josm.plugins.housenumberclick;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;

final class IntersectionPoint {

    private final LatLon coordinate;
    private final int segmentIndex;
    private final Node existingNode;
    private final boolean existingNodeIntersection;

    IntersectionPoint(LatLon coordinate, int segmentIndex, Node existingNode, boolean existingNodeIntersection) {
        this.coordinate = coordinate;
        this.segmentIndex = segmentIndex;
        this.existingNode = existingNode;
        this.existingNodeIntersection = existingNodeIntersection;
    }

    LatLon getCoordinate() {
        return coordinate;
    }

    int getSegmentIndex() {
        return segmentIndex;
    }

    Node getExistingNode() {
        return existingNode;
    }

    boolean isExistingNode() {
        return existingNodeIntersection;
    }
}

