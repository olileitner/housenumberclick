package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Logging;

/**
 * Estimates whether a street is likely incomplete to decide when reference street loading is useful.
 */
final class StreetCompletenessHeuristic {

    private static final double EDGE_MARGIN_RATIO = 0.01;
    private static final double EDGE_MARGIN_MIN = 0.0001;

    boolean isStreetPossiblyIncomplete(DataSet dataSet, String streetName) {
        String normalizedStreet = normalize(streetName);
        if (dataSet == null || normalizedStreet.isEmpty()) {
            return false;
        }

        Collection<Bounds> dataSourceBounds = dataSet.getDataSourceBounds();
        if (dataSourceBounds == null || dataSourceBounds.isEmpty()) {
            return false;
        }

        List<Way> streetWays = collectStreetWays(dataSet, normalizedStreet);
        if (streetWays.isEmpty()) {
            return false;
        }

        for (Way way : streetWays) {
            if (way == null) {
                continue;
            }

            if (way.isClosed()) {
                continue;
            }

            if (isLikelyCutOffEndpoint(way.firstNode(), dataSourceBounds)
                    || isLikelyCutOffEndpoint(way.lastNode(), dataSourceBounds)) {
                Logging.debug("HouseNumberClick completeness heuristic: cutoff-endpoint for street='" + normalizedStreet + "'.");
                return true;
            }
        }

        return false;
    }

    private List<Way> collectStreetWays(DataSet dataSet, String streetName) {
        List<Way> result = new ArrayList<>();
        for (Way way : dataSet.getWays()) {
            if (way == null || !way.isUsable() || !way.hasKey("highway")) {
                continue;
            }
            if (!normalize(way.get("name")).equalsIgnoreCase(streetName)) {
                continue;
            }
            result.add(way);
        }
        return result;
    }

    private boolean isLikelyCutOffEndpoint(Node node, Collection<Bounds> boundsCollection) {
        return isNearAnyDownloadEdge(node, boundsCollection)
                && countConnectedHighwayWays(node) <= 1;
    }

    private boolean isNearAnyDownloadEdge(Node node, Collection<Bounds> boundsCollection) {
        if (boundsCollection == null || boundsCollection.isEmpty()) {
            return false;
        }
        for (Bounds bounds : boundsCollection) {
            if (bounds == null) {
                continue;
            }
            double latSpan = Math.abs(bounds.getMaxLat() - bounds.getMinLat());
            double lonSpan = Math.abs(bounds.getMaxLon() - bounds.getMinLon());
            double latMargin = Math.max(EDGE_MARGIN_MIN, latSpan * EDGE_MARGIN_RATIO);
            double lonMargin = Math.max(EDGE_MARGIN_MIN, lonSpan * EDGE_MARGIN_RATIO);
            if (isNearDownloadEdge(node, bounds, latMargin, lonMargin)) {
                return true;
            }
        }
        return false;
    }

    private int countConnectedHighwayWays(Node node) {
        if (node == null) {
            return 0;
        }
        int count = 0;
        for (OsmPrimitive referrer : node.getReferrers()) {
            if (referrer instanceof Way) {
                Way way = (Way) referrer;
                if (way.isUsable() && way.hasKey("highway")) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean isNearDownloadEdge(Node node, Bounds bounds, double latMargin, double lonMargin) {
        if (node == null || bounds == null) {
            return false;
        }
        LatLon coor = node.getCoor();
        if (coor == null) {
            return false;
        }

        double lat = coor.lat();
        double lon = coor.lon();
        return Math.abs(lat - bounds.getMinLat()) <= latMargin
                || Math.abs(lat - bounds.getMaxLat()) <= latMargin
                || Math.abs(lon - bounds.getMinLon()) <= lonMargin
                || Math.abs(lon - bounds.getMaxLon()) <= lonMargin;
    }


    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
