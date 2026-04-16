package org.openstreetmap.josm.plugins.housenumberclick;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;

/**
 * Collects and normalizes addressed buildings near the locally resolved selected street segment,
 * canonicalizing relation/outer-way representations of the same real building.
 */
final class HouseNumberOverlayCollector {

    private static final Pattern HOUSE_NUMBER_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*([^\\d].*)?$");
    private static final double MAX_BUILDING_DISTANCE_TO_SELECTED_STREET_METERS = 120.0;
    private static final String LOG_PREFIX = "HouseNumberClick overlay diagnostics";

    List<HouseNumberOverlayEntry> collect(DataSet dataSet, StreetOption selectedStreet,
            StreetNameCollector.StreetIndex streetIndex, Way seedWayHint) {
        if (dataSet == null || selectedStreet == null || !selectedStreet.isValid()) {
            return new ArrayList<>();
        }

        StreetNameCollector.StreetIndex effectiveStreetIndex = streetIndex != null
                ? streetIndex
                : StreetNameCollector.collectStreetIndex(dataSet);
        List<Way> selectedStreetWays = effectiveStreetIndex.getLocalStreetChainWays(selectedStreet, seedWayHint);

        Map<Long, HouseNumberOverlayEntry> entriesByCanonicalPrimitiveId = new LinkedHashMap<>();
        CollectionStats stats = new CollectionStats();
        stats.selectedStreetWays = selectedStreetWays.size();

        for (Way way : dataSet.getWays()) {
            collectPrimitive(entriesByCanonicalPrimitiveId, way, selectedStreet, selectedStreetWays, stats);
        }

        for (Relation relation : dataSet.getRelations()) {
            collectPrimitive(entriesByCanonicalPrimitiveId, relation, selectedStreet, selectedStreetWays, stats);
        }

        List<HouseNumberOverlayEntry> entries = new ArrayList<>(entriesByCanonicalPrimitiveId.values());
        entries.sort(createComparator());
        logCollectionResult(selectedStreet, stats, entries.size());
        return entries;
    }

    private void collectPrimitive(Map<Long, HouseNumberOverlayEntry> entriesByCanonicalPrimitiveId, OsmPrimitive primitive,
            StreetOption selectedStreet, List<Way> selectedStreetWays, CollectionStats stats) {
        stats.scannedPrimitives++;

        if (!AddressedBuildingMatcher.isBuildingGeometry(primitive)) {
            stats.rejectedNotAddressedForStreet++;
            return;
        }

        OsmPrimitive canonicalPrimitive = resolveCanonicalPrimitive(primitive);
        if (canonicalPrimitive == null || !AddressedBuildingMatcher.isBuildingGeometry(canonicalPrimitive)) {
            stats.rejectedNotAddressedForStreet++;
            return;
        }

        long canonicalId = canonicalPrimitive.getUniqueId();
        if (entriesByCanonicalPrimitiveId.containsKey(canonicalId)) {
            stats.rejectedCanonicalDuplicate++;
            return;
        }

        HouseNumberOverlayEntry entry = buildEntry(canonicalPrimitive, primitive, selectedStreet, selectedStreetWays,
                entriesByCanonicalPrimitiveId.size(), stats);
        if (entry != null) {
            entriesByCanonicalPrimitiveId.put(canonicalId, entry);
            stats.canonicalizedPrimitives = entriesByCanonicalPrimitiveId.size();
        }
    }

    private Comparator<HouseNumberOverlayEntry> createComparator() {
        return Comparator
                .comparingInt(HouseNumberOverlayEntry::getNumberPart)
                .thenComparing(HouseNumberOverlayEntry::getSuffixPart, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(HouseNumberOverlayEntry::getHouseNumber, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(HouseNumberOverlayEntry::getStableIndex);
    }

    private HouseNumberOverlayEntry buildEntry(OsmPrimitive canonicalPrimitive, OsmPrimitive sourcePrimitive,
            StreetOption selectedStreet,
            List<Way> selectedStreetWays, int stableIndex, CollectionStats stats) {
        OsmPrimitive addressedPrimitive = resolveAddressedPrimitive(canonicalPrimitive, sourcePrimitive);
        if (!AddressedBuildingMatcher.isAddressedBuildingForStreet(addressedPrimitive, selectedStreet.getBaseStreetName())) {
            stats.rejectedNotAddressedForStreet++;
            return null;
        }

        EastNorth labelPoint = resolveLabelPoint(canonicalPrimitive);
        if (labelPoint == null) {
            stats.rejectedMissingLabelPoint++;
            return null;
        }
        if (!isNearSelectedStreet(labelPoint, selectedStreetWays)) {
            stats.rejectedByDistance++;
            return null;
        }

        String street = normalize(addressedPrimitive.get("addr:street"));
        String postcode = normalize(addressedPrimitive.get("addr:postcode"));
        if (postcode.isEmpty()) {
            stats.acceptedMissingPostcode++;
        }
        String houseNumber = normalize(addressedPrimitive.get("addr:housenumber"));
        ParsedHouseNumber parsedHouseNumber = parseHouseNumber(houseNumber);
        return new HouseNumberOverlayEntry(
                canonicalPrimitive,
                street,
                postcode,
                houseNumber,
                parsedHouseNumber.numberPart,
                parsedHouseNumber.suffixPart,
                labelPoint,
                stableIndex
        );
    }

    private OsmPrimitive resolveAddressedPrimitive(OsmPrimitive canonicalPrimitive, OsmPrimitive sourcePrimitive) {
        if (AddressedBuildingMatcher.isAddressedBuilding(canonicalPrimitive)) {
            return canonicalPrimitive;
        }
        if (AddressedBuildingMatcher.isAddressedBuilding(sourcePrimitive)) {
            return sourcePrimitive;
        }
        return canonicalPrimitive;
    }

    private OsmPrimitive resolveCanonicalPrimitive(OsmPrimitive primitive) {
        if (!(primitive instanceof Way) || !primitive.isUsable()) {
            return primitive;
        }
        Way way = (Way) primitive;
        Relation canonicalRelation = findBuildingOuterMultipolygonRelation(way);
        return canonicalRelation != null ? canonicalRelation : primitive;
    }

    private Relation findBuildingOuterMultipolygonRelation(Way way) {
        if (way == null || !way.isUsable()) {
            return null;
        }
        Relation best = null;
        for (OsmPrimitive referrer : way.getReferrers()) {
            if (!(referrer instanceof Relation)) {
                continue;
            }
            Relation relation = (Relation) referrer;
            if (!isBuildingOuterMultipolygonRelationForWay(relation, way)) {
                continue;
            }
            if (best == null || relation.getUniqueId() < best.getUniqueId()) {
                best = relation;
            }
        }
        return best;
    }

    private boolean isBuildingOuterMultipolygonRelationForWay(Relation relation, Way way) {
        if (relation == null || way == null || !relation.isUsable()) {
            return false;
        }
        if (!relation.hasTag("type", "multipolygon") || !relation.hasTag("building")) {
            return false;
        }
        for (RelationMember member : relation.getMembers()) {
            if (member == null || !member.isWay() || member.getWay() != way) {
                continue;
            }
            String role = normalize(member.getRole());
            if (role.isEmpty() || "outer".equals(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNearSelectedStreet(EastNorth labelPoint, List<Way> streetWays) {
        if (labelPoint == null || streetWays == null || streetWays.isEmpty()) {
            return false;
        }
        double limitSquared = MAX_BUILDING_DISTANCE_TO_SELECTED_STREET_METERS * MAX_BUILDING_DISTANCE_TO_SELECTED_STREET_METERS;
        double bestSquared = Double.POSITIVE_INFINITY;
        for (Way way : streetWays) {
            if (way == null || !way.isUsable()) {
                continue;
            }
            List<Node> nodes = way.getNodes();
            for (int i = 1; i < nodes.size(); i++) {
                Node first = nodes.get(i - 1);
                Node second = nodes.get(i);
                if (first == null || second == null || first.getEastNorth() == null || second.getEastNorth() == null) {
                    continue;
                }
                double distanceSquared = distanceSquaredToSegment(labelPoint, first.getEastNorth(), second.getEastNorth());
                if (distanceSquared < bestSquared) {
                    bestSquared = distanceSquared;
                    if (bestSquared <= limitSquared) {
                        return true;
                    }
                }
            }
        }
        return bestSquared <= limitSquared;
    }

    private double distanceSquaredToSegment(EastNorth point, EastNorth segmentStart, EastNorth segmentEnd) {
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
            double ex = px - ax;
            double ey = py - ay;
            return (ex * ex) + (ey * ey);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0.0, Math.min(1.0, t));
        double projectionX = ax + (t * dx);
        double projectionY = ay + (t * dy);
        double ex = px - projectionX;
        double ey = py - projectionY;
        return (ex * ex) + (ey * ey);
    }

    private void logCollectionResult(StreetOption selectedStreet, CollectionStats stats, int collected) {
        if (selectedStreet == null) {
            return;
        }
        Logging.debug(LOG_PREFIX + ": collect result base='" + normalize(selectedStreet.getBaseStreetName())
                + "', display='" + normalize(selectedStreet.getDisplayStreetName())
                + "', cluster='" + normalize(selectedStreet.getClusterId()) + "', scanned=" + stats.scannedPrimitives
                + ", canonicalized=" + stats.canonicalizedPrimitives
                + ", selectedStreetWays=" + stats.selectedStreetWays
                + ", collected=" + collected
                + ", rejectedNotAddressedForStreet=" + stats.rejectedNotAddressedForStreet
                + ", rejectedByDistance=" + stats.rejectedByDistance
                + ", rejectedMissingLabelPoint=" + stats.rejectedMissingLabelPoint
                + ", rejectedCanonicalDuplicate=" + stats.rejectedCanonicalDuplicate
                + ", acceptedMissingPostcode=" + stats.acceptedMissingPostcode + ".");
    }


    private EastNorth resolveLabelPoint(OsmPrimitive primitive) {
        Area area = Geometry.getAreaEastNorth(primitive);
        if (area == null || area.isEmpty()) {
            return null;
        }

        Rectangle2D bounds = area.getBounds2D();
        if (bounds == null || bounds.isEmpty()) {
            return null;
        }

        List<EastNorth> candidates = new ArrayList<>();
        addBoundingBoxCandidates(candidates, primitive, bounds);
        addGeometryCandidates(candidates, primitive);

        EastNorth bestInside = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (EastNorth candidate : candidates) {
            if (candidate == null || !area.contains(candidate.east(), candidate.north())) {
                continue;
            }
            double score = interiorScore(candidate, bounds);
            if (score > bestScore) {
                bestScore = score;
                bestInside = candidate;
            }
        }

        return bestInside;
    }

    private void addBoundingBoxCandidates(List<EastNorth> candidates, OsmPrimitive primitive, Rectangle2D areaBounds) {
        BBox primitiveBox = primitive.getBBox();
        Projection projection = ProjectionRegistry.getProjection();
        if (primitiveBox != null && primitiveBox.isValid() && projection != null) {
            LatLon centerLatLon = primitiveBox.getCenter();
            candidates.add(projection.latlon2eastNorth(centerLatLon));
        }

        double cx = areaBounds.getCenterX();
        double cy = areaBounds.getCenterY();
        double dx = areaBounds.getWidth() * 0.25;
        double dy = areaBounds.getHeight() * 0.25;

        candidates.add(new EastNorth(cx, cy));
        candidates.add(new EastNorth(cx + dx, cy));
        candidates.add(new EastNorth(cx - dx, cy));
        candidates.add(new EastNorth(cx, cy + dy));
        candidates.add(new EastNorth(cx, cy - dy));
        candidates.add(new EastNorth(cx + dx, cy + dy));
        candidates.add(new EastNorth(cx + dx, cy - dy));
        candidates.add(new EastNorth(cx - dx, cy + dy));
        candidates.add(new EastNorth(cx - dx, cy - dy));
    }

    private void addGeometryCandidates(List<EastNorth> candidates, OsmPrimitive primitive) {
        if (primitive instanceof Way) {
            addWayGeometryCandidates(candidates, (Way) primitive);
            return;
        }

        if (!(primitive instanceof Relation)) {
            return;
        }

        Relation relation = (Relation) primitive;
        for (RelationMember member : relation.getMembers()) {
            String role = member.getRole();
            boolean isOuter = role == null || role.isEmpty() || "outer".equals(role);
            if (!isOuter || !member.isWay()) {
                continue;
            }
            Way way = member.getWay();
            if (way == null || !way.isUsable()) {
                continue;
            }
            addWayGeometryCandidates(candidates, way);
        }
    }

    private void addWayGeometryCandidates(List<EastNorth> candidates, Way way) {
        if (way == null || !way.isClosed()) {
            return;
        }

        List<Node> nodes = way.getNodes();
        if (nodes.size() < 3) {
            return;
        }

        EastNorth centroid = Geometry.getCentroid(nodes);
        if (centroid != null) {
            candidates.add(centroid);
        }

        EastNorth center = Geometry.getCenter(nodes);
        if (center != null) {
            candidates.add(center);
        }
    }

    private double interiorScore(EastNorth point, Rectangle2D bounds) {
        double left = point.east() - bounds.getMinX();
        double right = bounds.getMaxX() - point.east();
        double top = bounds.getMaxY() - point.north();
        double bottom = point.north() - bounds.getMinY();
        return Math.min(Math.min(left, right), Math.min(top, bottom));
    }

    private ParsedHouseNumber parseHouseNumber(String houseNumber) {
        String normalized = normalize(houseNumber);
        Matcher matcher = HOUSE_NUMBER_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return new ParsedHouseNumber(Integer.MAX_VALUE, normalized.toLowerCase(Locale.ROOT));
        }

        int numberPart;
        try {
            numberPart = Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            numberPart = Integer.MAX_VALUE;
        }

        String suffix = normalize(matcher.group(2)).toLowerCase(Locale.ROOT);
        return new ParsedHouseNumber(numberPart, suffix);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Parsed representation of a house number split into sortable numeric and suffix parts.
     */
    private static final class ParsedHouseNumber {
        private final int numberPart;
        private final String suffixPart;

        ParsedHouseNumber(int numberPart, String suffixPart) {
            this.numberPart = numberPart;
            this.suffixPart = suffixPart;
        }
    }

    /**
     * Aggregated rejection counters used for overlay collection diagnostics.
     */
    private static final class CollectionStats {
        private int selectedStreetWays;
        private int scannedPrimitives;
        private int canonicalizedPrimitives;
        private int rejectedNotAddressedForStreet;
        private int rejectedByDistance;
        private int rejectedMissingLabelPoint;
        private int rejectedCanonicalDuplicate;
        private int acceptedMissingPostcode;
    }
}
