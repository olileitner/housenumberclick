package org.openstreetmap.josm.plugins.housenumberclick;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.Logging;

/**
 * Utility for collecting and spatially disambiguating street names from highway ways in the current dataset,
 * and for resolving local same-name street chains from a concrete seed way.
 */
final class StreetNameCollector {

    // Intentionally larger so short mapping gaps/junction splits do not fragment one logical street.
    private static final double COMPONENT_ENDPOINT_CONNECT_DISTANCE_METERS = 75.0;

    private StreetNameCollector() {
        // Utility class
    }

    /**
     * Immutable lookup/index for disambiguated street clusters in the current dataset/view scope.
     */
    static final class StreetIndex {
        private final List<StreetOption> streetOptions;
        private final Map<String, StreetOption> optionsByDisplayName;
        private final Map<String, List<StreetOption>> optionsByBaseStreetName;
        private final Map<Way, StreetOption> optionByWay;
        private final Map<String, List<Way>> waysByBaseStreetName;
        private final Map<String, Way> seedWayByClusterId;
        private final Map<String, EastNorth> clusterCentroids;

        private StreetIndex(List<StreetOption> streetOptions, Map<String, List<StreetOption>> optionsByBaseStreetName,
                Map<Way, StreetOption> optionByWay, Map<String, List<Way>> waysByBaseStreetName,
                Map<String, Way> seedWayByClusterId, Map<String, EastNorth> clusterCentroids) {
            this.streetOptions = streetOptions == null ? List.of() : List.copyOf(streetOptions);
            this.optionsByBaseStreetName = optionsByBaseStreetName == null
                    ? Map.of()
                    : Collections.unmodifiableMap(optionsByBaseStreetName);
            this.optionByWay = optionByWay == null ? Map.of() : Collections.unmodifiableMap(optionByWay);
            this.waysByBaseStreetName = waysByBaseStreetName == null
                    ? Map.of()
                    : Collections.unmodifiableMap(waysByBaseStreetName);
            this.seedWayByClusterId = seedWayByClusterId == null
                    ? Map.of()
                    : Collections.unmodifiableMap(seedWayByClusterId);
            this.clusterCentroids = clusterCentroids == null ? Map.of() : Collections.unmodifiableMap(clusterCentroids);

            Map<String, StreetOption> displayLookup = new HashMap<>();
            for (StreetOption option : this.streetOptions) {
                if (option == null || !option.isValid()) {
                    continue;
                }
                displayLookup.put(option.getDisplayStreetName().toLowerCase(Locale.ROOT), option);
            }
            this.optionsByDisplayName = Collections.unmodifiableMap(displayLookup);
        }

        List<StreetOption> getStreetOptions() {
            return new ArrayList<>(streetOptions);
        }

        StreetOption findByDisplayStreetName(String displayStreetName) {
            String key = normalize(displayStreetName).toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                return null;
            }
            return optionsByDisplayName.get(key);
        }

        StreetOption findByClusterId(String clusterId) {
            String normalizedClusterId = normalize(clusterId);
            if (normalizedClusterId.isEmpty()) {
                return null;
            }
            for (StreetOption option : streetOptions) {
                if (option != null && normalizedClusterId.equals(option.getClusterId())) {
                    return option;
                }
            }
            return null;
        }

        List<StreetOption> getOptionsForBaseStreetName(String baseStreetName) {
            String key = normalize(baseStreetName).toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                return List.of();
            }
            List<StreetOption> options = optionsByBaseStreetName.get(key);
            return options == null ? List.of() : new ArrayList<>(options);
        }

        List<Way> getWaysForStreetOption(StreetOption option) {
            if (option == null || !option.isValid()) {
                return List.of();
            }
            List<Way> ways = new ArrayList<>();
            for (Map.Entry<Way, StreetOption> entry : optionByWay.entrySet()) {
                if (Objects.equals(option, entry.getValue())) {
                    ways.add(entry.getKey());
                }
            }
            return ways;
        }

        List<Way> getLocalStreetChainWays(StreetOption option) {
            return getLocalStreetChainWays(option, null);
        }

        List<Way> getLocalStreetChainWays(StreetOption option, Way preferredSeedWay) {
            if (option == null || !option.isValid()) {
                return List.of();
            }
            return getLocalStreetChainWays(option.getBaseStreetName(), preferredSeedWay, option.getClusterId());
        }

        List<Way> getLocalStreetChainWays(String baseStreetName, Way preferredSeedWay) {
            return getLocalStreetChainWays(baseStreetName, preferredSeedWay, "");
        }

        private List<Way> getLocalStreetChainWays(String baseStreetName, Way preferredSeedWay, String fallbackClusterId) {
            String baseKey = normalize(baseStreetName).toLowerCase(Locale.ROOT);
            List<Way> candidates = waysByBaseStreetName.get(baseKey);
            if (candidates == null || candidates.isEmpty()) {
                return List.of();
            }

            Way seed = resolvePreferredSeedWay(candidates, preferredSeedWay, baseStreetName);
            if (seed == null) {
                seed = findSeedWayForClusterId(fallbackClusterId, baseStreetName);
            }
            if (seed == null) {
                Logging.warn("HouseNumberClick: no valid seedWay found for baseStreetName='"
                        + normalize(baseStreetName) + "', clusterId='" + normalize(fallbackClusterId) + "'.");
                return List.of();
            }

            LinkedHashSet<Way> visited = new LinkedHashSet<>();
            Queue<Way> queue = new ArrayDeque<>();
            queue.add(seed);
            visited.add(seed);

            while (!queue.isEmpty()) {
                Way current = queue.poll();
                for (Way candidate : candidates) {
                    if (candidate == null || visited.contains(candidate)) {
                        continue;
                    }
                    if (areWaysSpatiallyConnected(current, candidate)) {
                        visited.add(candidate);
                        queue.add(candidate);
                    }
                }
            }

            return new ArrayList<>(visited);
        }

        Way findSeedWayForClusterId(String clusterId, String baseStreetName) {
            String normalizedClusterId = normalize(clusterId);
            if (normalizedClusterId.isEmpty()) {
                return null;
            }
            Way clusterSeed = seedWayByClusterId.get(normalizedClusterId);
            if (clusterSeed == null || !clusterSeed.isUsable()) {
                return null;
            }

            String baseKey = normalize(baseStreetName).toLowerCase(Locale.ROOT);
            List<Way> candidates = waysByBaseStreetName.get(baseKey);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            if (candidates.contains(clusterSeed)) {
                return clusterSeed;
            }

            for (Way candidate : candidates) {
                if (candidate != null && candidate.getUniqueId() == clusterSeed.getUniqueId()) {
                    return candidate;
                }
            }
            return null;
        }

        Way findNearestWayForBaseStreetName(String baseStreetName, LatLon referencePoint) {
            if (ProjectionRegistry.getProjection() == null) {
                return null;
            }
            String baseKey = normalize(baseStreetName).toLowerCase(Locale.ROOT);
            if (baseKey.isEmpty() || referencePoint == null) {
                return null;
            }
            List<Way> candidates = waysByBaseStreetName.get(baseKey);
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            EastNorth referenceEastNorth = ProjectionRegistry.getProjection().latlon2eastNorth(referencePoint);
            if (referenceEastNorth == null) {
                return null;
            }

            Way nearest = null;
            double bestDistanceSquared = Double.POSITIVE_INFINITY;
            for (Way candidate : candidates) {
                if (candidate == null || !candidate.isUsable()) {
                    continue;
                }
                double distanceSquared = distanceSquaredToWay(referenceEastNorth, candidate);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    nearest = candidate;
                }
            }
            return nearest;
        }

        StreetOption resolveForAddressPrimitive(OsmPrimitive primitive) {
            String baseStreetName = primitive == null ? "" : normalize(primitive.get("addr:street"));
            return resolveForBaseStreetAndPrimitive(baseStreetName, primitive);
        }

        StreetOption resolveForBaseStreetAndPrimitive(String baseStreetName, OsmPrimitive primitive) {
            List<StreetOption> options = getOptionsForBaseStreetName(baseStreetName);
            if (options.isEmpty()) {
                return null;
            }
            if (options.size() == 1 || primitive == null) {
                return options.get(0);
            }

            EastNorth primitivePoint = resolvePrimitivePoint(primitive);
            if (primitivePoint == null) {
                return options.get(0);
            }

            StreetOption bestOption = options.get(0);
            double bestDistanceSquared = Double.POSITIVE_INFINITY;
            for (StreetOption option : options) {
                EastNorth centroid = clusterCentroids.get(option.getClusterId());
                if (centroid == null) {
                    continue;
                }
                double dx = primitivePoint.east() - centroid.east();
                double dy = primitivePoint.north() - centroid.north();
                double distanceSquared = (dx * dx) + (dy * dy);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestOption = option;
                }
            }
            return bestOption;
        }

        private EastNorth resolvePrimitivePoint(OsmPrimitive primitive) {
            if (primitive == null) {
                return null;
            }
            BBox bbox = primitive.getBBox();
            if (bbox == null || !bbox.isValid()) {
                return null;
            }
            LatLon center = bbox.getCenter();
            if (center == null || ProjectionRegistry.getProjection() == null) {
                return null;
            }
            return ProjectionRegistry.getProjection().latlon2eastNorth(center);
        }

        private Way resolvePreferredSeedWay(List<Way> candidates, Way preferredSeedWay, String baseStreetName) {
            if (preferredSeedWay == null || !preferredSeedWay.isUsable()) {
                return null;
            }
            if (!normalize(preferredSeedWay.get("name")).equalsIgnoreCase(normalize(baseStreetName))) {
                return null;
            }
            if (candidates.contains(preferredSeedWay)) {
                return preferredSeedWay;
            }
            for (Way candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                if (candidate.getUniqueId() == preferredSeedWay.getUniqueId()) {
                    return candidate;
                }
            }
            return null;
        }

        private double distanceSquaredToWay(EastNorth referencePoint, Way way) {
            double bestDistanceSquared = Double.POSITIVE_INFINITY;
            List<org.openstreetmap.josm.data.osm.Node> nodes = way.getNodes();
            if (nodes == null || nodes.isEmpty()) {
                return bestDistanceSquared;
            }

            for (int i = 0; i < nodes.size(); i++) {
                EastNorth nodePoint = toEastNorth(nodes.get(i));
                if (nodePoint == null) {
                    continue;
                }
                double nodeDistanceSquared = distanceSquared(referencePoint, nodePoint);
                if (nodeDistanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = nodeDistanceSquared;
                }
                if (i == 0) {
                    continue;
                }
                EastNorth previousPoint = toEastNorth(nodes.get(i - 1));
                if (previousPoint == null) {
                    continue;
                }
                double segmentDistanceSquared = distanceSquaredToSegment(referencePoint, previousPoint, nodePoint);
                if (segmentDistanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = segmentDistanceSquared;
                }
            }
            return bestDistanceSquared;
        }

        private EastNorth toEastNorth(org.openstreetmap.josm.data.osm.Node node) {
            if (node == null || node.getCoor() == null || ProjectionRegistry.getProjection() == null) {
                return null;
            }
            return ProjectionRegistry.getProjection().latlon2eastNorth(node.getCoor());
        }

        private double distanceSquared(EastNorth first, EastNorth second) {
            if (first == null || second == null) {
                return Double.POSITIVE_INFINITY;
            }
            double dx = first.east() - second.east();
            double dy = first.north() - second.north();
            return (dx * dx) + (dy * dy);
        }

        private double distanceSquaredToSegment(EastNorth point, EastNorth segmentStart, EastNorth segmentEnd) {
            if (point == null || segmentStart == null || segmentEnd == null) {
                return Double.POSITIVE_INFINITY;
            }
            double px = point.east();
            double py = point.north();
            double ax = segmentStart.east();
            double ay = segmentStart.north();
            double bx = segmentEnd.east();
            double by = segmentEnd.north();
            double dx = bx - ax;
            double dy = by - ay;
            double lengthSquared = (dx * dx) + (dy * dy);
            if (lengthSquared <= 0.0) {
                return distanceSquared(point, segmentStart);
            }
            double t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
            t = Math.max(0.0, Math.min(1.0, t));
            double projectionX = ax + (t * dx);
            double projectionY = ay + (t * dy);
            double ex = px - projectionX;
            double ey = py - projectionY;
            return (ex * ex) + (ey * ey);
        }
    }

    static StreetIndex collectStreetIndex(DataSet dataSet) {
        if (dataSet == null) {
            return new StreetIndex(List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        Collection<Way> candidateWays = getWaysFromCurrentView(dataSet);
        Map<String, List<Way>> waysByBaseStreetName = new HashMap<>();
        for (Way way : candidateWays) {
            if (way == null || !way.isUsable() || !way.hasTag("highway")) {
                continue;
            }
            String baseStreetName = normalize(way.get("name"));
            if (baseStreetName.isEmpty()) {
                continue;
            }
            waysByBaseStreetName.computeIfAbsent(baseStreetName, ignored -> new ArrayList<>()).add(way);
        }

        List<String> sortedBaseStreetNames = new ArrayList<>(waysByBaseStreetName.keySet());
        sortedBaseStreetNames.sort(Collator.getInstance());

        List<StreetOption> streetOptions = new ArrayList<>();
        Map<String, List<StreetOption>> optionsByBaseStreetName = new LinkedHashMap<>();
        Map<Way, StreetOption> optionByWay = new IdentityHashMap<>();
        Map<String, List<Way>> waysByBaseStreetNameIndex = new LinkedHashMap<>();
        Map<String, Way> seedWayByClusterId = new HashMap<>();
        Map<String, EastNorth> clusterCentroids = new HashMap<>();

        for (String baseStreetName : sortedBaseStreetNames) {
            List<Way> namedWays = waysByBaseStreetName.getOrDefault(baseStreetName, List.of());
            waysByBaseStreetNameIndex.put(baseStreetName.toLowerCase(Locale.ROOT), List.copyOf(namedWays));
            List<List<Way>> components = splitIntoConnectedComponents(namedWays);
            components.sort(Comparator.comparing(StreetNameCollector::computeComponentSortKey));
            if (components.size() > 1) {
                Logging.debug("HouseNumberClick cluster build: base='" + baseStreetName
                        + "', clusters=" + components.size()
                        + ", sizes=" + formatComponentSizes(components) + ".");
            }

            List<StreetOption> optionsForBaseStreet = new ArrayList<>();
            for (int i = 0; i < components.size(); i++) {
                int clusterIndex = i + 1;
                String clusterId = buildClusterId(baseStreetName, clusterIndex);
                String displayStreetName = components.size() <= 1
                        ? baseStreetName
                        : (clusterIndex == 1 ? baseStreetName : baseStreetName + " [" + clusterIndex + "]");
                StreetOption option = new StreetOption(baseStreetName, displayStreetName, clusterId);
                streetOptions.add(option);
                optionsForBaseStreet.add(option);
                clusterCentroids.put(clusterId, computeComponentCentroid(components.get(i)));
                Way seed = components.get(i).isEmpty() ? null : components.get(i).get(0);
                if (seed != null) {
                    seedWayByClusterId.put(clusterId, seed);
                }
                for (Way way : components.get(i)) {
                    optionByWay.put(way, option);
                }
            }
            optionsByBaseStreetName.put(baseStreetName.toLowerCase(Locale.ROOT), List.copyOf(optionsForBaseStreet));
        }

        return new StreetIndex(streetOptions, optionsByBaseStreetName, optionByWay,
                waysByBaseStreetNameIndex, seedWayByClusterId, clusterCentroids);
    }

    static List<String> collectStreetNames(DataSet dataSet) {
        Set<String> names = new TreeSet<>(Collator.getInstance());
        for (StreetOption option : collectStreetIndex(dataSet).getStreetOptions()) {
            names.add(option.getDisplayStreetName());
        }
        return new ArrayList<>(names);
    }

    private static List<List<Way>> splitIntoConnectedComponents(List<Way> ways) {
        if (ways == null || ways.isEmpty()) {
            return List.of();
        }

        List<List<Way>> components = new ArrayList<>();
        Set<Way> visited = new LinkedHashSet<>();
        for (Way start : ways) {
            if (start == null || visited.contains(start)) {
                continue;
            }
            List<Way> component = new ArrayList<>();
            Queue<Way> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                Way current = queue.poll();
                component.add(current);
                for (Way candidate : ways) {
                    if (candidate == null || visited.contains(candidate)) {
                        continue;
                    }
                    if (areWaysSpatiallyConnected(current, candidate)) {
                        visited.add(candidate);
                        queue.add(candidate);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    private static boolean areWaysSpatiallyConnected(Way first, Way second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }

        for (org.openstreetmap.josm.data.osm.Node firstNode : first.getNodes()) {
            if (firstNode == null) {
                continue;
            }
            if (second.getNodes().contains(firstNode)) {
                return true;
            }
        }

        List<org.openstreetmap.josm.data.osm.Node> firstEndpoints = collectEndpoints(first);
        List<org.openstreetmap.josm.data.osm.Node> secondEndpoints = collectEndpoints(second);
        for (org.openstreetmap.josm.data.osm.Node firstEndpoint : firstEndpoints) {
            if (firstEndpoint == null || firstEndpoint.getCoor() == null) {
                continue;
            }
            for (org.openstreetmap.josm.data.osm.Node secondEndpoint : secondEndpoints) {
                if (secondEndpoint == null || secondEndpoint.getCoor() == null) {
                    continue;
                }
                double distanceMeters = firstEndpoint.getCoor().greatCircleDistance(secondEndpoint.getCoor());
                if (distanceMeters <= COMPONENT_ENDPOINT_CONNECT_DISTANCE_METERS) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<org.openstreetmap.josm.data.osm.Node> collectEndpoints(Way way) {
        if (way == null || way.getNodesCount() == 0) {
            return List.of();
        }
        if (way.getNodesCount() == 1) {
            return List.of(way.firstNode());
        }
        return List.of(way.firstNode(), way.lastNode());
    }

    private static String computeComponentSortKey(List<Way> component) {
        EastNorth centroid = computeComponentCentroid(component);
        if (centroid == null) {
            return "~";
        }
        return String.format(Locale.ROOT, "%020.4f|%020.4f", centroid.north(), centroid.east());
    }

    private static EastNorth computeComponentCentroid(List<Way> component) {
        if (component == null || component.isEmpty()) {
            return null;
        }
        if (ProjectionRegistry.getProjection() == null) {
            return null;
        }
        double eastSum = 0.0;
        double northSum = 0.0;
        int points = 0;
        for (Way way : component) {
            if (way == null) {
                continue;
            }
            for (org.openstreetmap.josm.data.osm.Node node : way.getNodes()) {
                if (node == null || node.getCoor() == null) {
                    continue;
                }
                EastNorth eastNorth = ProjectionRegistry.getProjection().latlon2eastNorth(node.getCoor());
                if (eastNorth == null) {
                    continue;
                }
                eastSum += eastNorth.east();
                northSum += eastNorth.north();
                points++;
            }
        }
        if (points == 0) {
            return null;
        }
        return new EastNorth(eastSum / points, northSum / points);
    }

    private static String formatComponentSizes(List<List<Way>> components) {
        List<String> sizes = new ArrayList<>();
        for (List<Way> component : components) {
            sizes.add(Integer.toString(component == null ? 0 : component.size()));
        }
        return String.join(",", sizes);
    }

    private static String buildClusterId(String baseStreetName, int clusterIndex) {
        return normalize(baseStreetName).toLowerCase(Locale.ROOT) + "#" + clusterIndex;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
