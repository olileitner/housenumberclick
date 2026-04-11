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

final class StreetCompletenessHeuristic {

    boolean isStreetPossiblyIncomplete(DataSet dataSet, String streetName) {
        String normalizedStreet = normalize(streetName);
        if (dataSet == null || normalizedStreet.isEmpty()) {
            return false;
        }

        Bounds downloadedBounds = mergedDataSourceBounds(dataSet.getDataSourceBounds());
        if (downloadedBounds == null) {
            return false;
        }

        List<Way> streetWays = collectStreetWays(dataSet, normalizedStreet);
        if (streetWays.isEmpty()) {
            return false;
        }

        double latSpan = Math.abs(downloadedBounds.getMaxLat() - downloadedBounds.getMinLat());
        double lonSpan = Math.abs(downloadedBounds.getMaxLon() - downloadedBounds.getMinLon());
        double latMargin = Math.max(0.0001, latSpan * 0.01);
        double lonMargin = Math.max(0.0001, lonSpan * 0.01);

        for (Way way : streetWays) {
            if (way == null) {
                continue;
            }
            for (Node node : way.getNodes()) {
                if (isNearDownloadEdge(node, downloadedBounds, latMargin, lonMargin)) {
                    return true;
                }
            }

            if (!way.isClosed()) {
                if (isLikelyCutOffEndpoint(way.firstNode(), downloadedBounds, latMargin, lonMargin)
                        || isLikelyCutOffEndpoint(way.lastNode(), downloadedBounds, latMargin, lonMargin)) {
                    return true;
                }
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

    private boolean isLikelyCutOffEndpoint(Node node, Bounds bounds, double latMargin, double lonMargin) {
        return isNearDownloadEdge(node, bounds, latMargin, lonMargin)
                && countConnectedHighwayWays(node) <= 1;
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

    private Bounds mergedDataSourceBounds(Collection<Bounds> boundsCollection) {
        if (boundsCollection == null || boundsCollection.isEmpty()) {
            return null;
        }

        Bounds merged = null;
        for (Bounds bounds : boundsCollection) {
            if (bounds == null) {
                continue;
            }
            if (merged == null) {
                merged = new Bounds(bounds);
            } else {
                merged.extend(bounds);
            }
        }
        return merged;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

