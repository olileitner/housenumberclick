package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;

final class PostcodeCollector {

    private PostcodeCollector() {
        // Utility class
    }

    static String detectUniformVisiblePostcode(DataSet dataSet) {
        if (dataSet == null) {
            return "";
        }

        Set<String> postcodes = new HashSet<>();

        Collection<Way> ways = getWaysFromCurrentView(dataSet);
        for (Way way : ways) {
            if (!way.isUsable() || !way.hasTag("building")) {
                continue;
            }
            addPostcode(postcodes, way.get("addr:postcode"));
            if (postcodes.size() > 1) {
                return "";
            }
        }

        Collection<Relation> relations = getRelationsFromCurrentView(dataSet);
        for (Relation relation : relations) {
            if (!relation.isUsable() || !relation.hasTag("building")) {
                continue;
            }
            addPostcode(postcodes, relation.get("addr:postcode"));
            if (postcodes.size() > 1) {
                return "";
            }
        }

        return postcodes.size() == 1 ? postcodes.iterator().next() : "";
    }

    static java.util.List<String> collectVisiblePostcodes(DataSet dataSet) {
        if (dataSet == null) {
            return Collections.emptyList();
        }

        Set<String> postcodes = new HashSet<>();
        for (Way way : getWaysFromCurrentView(dataSet)) {
            if (!way.isUsable() || !way.hasTag("building")) {
                continue;
            }
            addPostcode(postcodes, way.get("addr:postcode"));
        }
        for (Relation relation : getRelationsFromCurrentView(dataSet)) {
            if (!relation.isUsable() || !relation.hasTag("building")) {
                continue;
            }
            addPostcode(postcodes, relation.get("addr:postcode"));
        }

        ArrayList<String> sorted = new ArrayList<>(postcodes);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    private static Collection<Way> getWaysFromCurrentView(DataSet dataSet) {
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapView != null) {
            Bounds bounds = map.mapView.getRealBounds();
            if (bounds != null) {
                return dataSet.searchWays(bounds.toBBox());
            }
        }
        return dataSet.getWays();
    }

    private static Collection<Relation> getRelationsFromCurrentView(DataSet dataSet) {
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapView != null) {
            Bounds bounds = map.mapView.getRealBounds();
            if (bounds != null) {
                return dataSet.searchRelations(bounds.toBBox());
            }
        }
        return dataSet.getRelations();
    }

    private static void addPostcode(Set<String> postcodes, String postcode) {
        if (postcode == null) {
            return;
        }
        String trimmed = postcode.trim();
        if (!trimmed.isEmpty()) {
            postcodes.add(trimmed);
        }
    }
}

