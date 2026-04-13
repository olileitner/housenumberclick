package org.openstreetmap.josm.plugins.housenumberclick;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Utility for collecting unique street names from highway ways in the current dataset.
 */
final class StreetNameCollector {

    private StreetNameCollector() {
        // Utility class
    }

    static List<String> collectStreetNames(DataSet dataSet) {
        if (dataSet == null) {
            return List.of();
        }

        Collection<Way> candidateWays = getWaysFromCurrentView(dataSet);
        Set<String> names = new TreeSet<>(Collator.getInstance());

        for (Way way : candidateWays) {
            if (!way.isUsable() || !way.hasTag("highway")) {
                continue;
            }
            String name = way.get("name");
            if (name != null) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }

        return new ArrayList<>(names);
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
}
