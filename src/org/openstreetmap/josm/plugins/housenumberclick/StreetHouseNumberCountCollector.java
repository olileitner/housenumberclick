package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Collects per-street address counts and completeness indicators across the current dataset,
 * using conditional city-aware duplicate detection for street-count duplicate markers.
 */
final class StreetHouseNumberCountCollector {

    List<StreetHouseNumberCountRow> collectRows(DataSet dataSet, StreetNameCollector.StreetIndex streetIndex) {
        if (dataSet == null) {
            return new ArrayList<>();
        }

        StreetNameCollector.StreetIndex effectiveStreetIndex = streetIndex != null
                ? streetIndex
                : StreetNameCollector.collectStreetIndex(dataSet);

        Map<String, StreetOption> optionByCluster = new HashMap<>();
        Map<String, Integer> countsByCluster = new HashMap<>();
        Map<String, Map<String, DuplicateAddressGroupStats>> duplicateAddressGroupsByCluster = new HashMap<>();
        Map<String, Boolean> hasDuplicateByCluster = new HashMap<>();

        for (StreetOption option : effectiveStreetIndex.getStreetOptions()) {
            if (option == null || !option.isValid()) {
                continue;
            }
            optionByCluster.put(option.getClusterId(), option);
            countsByCluster.putIfAbsent(option.getClusterId(), 0);
            hasDuplicateByCluster.putIfAbsent(option.getClusterId(), false);
        }

        for (Way way : dataSet.getWays()) {
            collectPrimitive(way, effectiveStreetIndex, countsByCluster, duplicateAddressGroupsByCluster, hasDuplicateByCluster);
        }
        for (Relation relation : dataSet.getRelations()) {
            collectPrimitive(relation, effectiveStreetIndex, countsByCluster, duplicateAddressGroupsByCluster, hasDuplicateByCluster);
        }

        List<StreetHouseNumberCountRow> rows = new ArrayList<>();
        for (StreetOption option : effectiveStreetIndex.getStreetOptions()) {
            if (option == null || !option.isValid()) {
                continue;
            }
            String clusterId = option.getClusterId();
            rows.add(new StreetHouseNumberCountRow(
                    option,
                    countsByCluster.getOrDefault(clusterId, 0),
                    Boolean.TRUE.equals(hasDuplicateByCluster.get(clusterId))
            ));
        }
        return rows;
    }

    private void collectPrimitive(OsmPrimitive primitive, StreetNameCollector.StreetIndex streetIndex,
            Map<String, Integer> countsByCluster,
            Map<String, Map<String, DuplicateAddressGroupStats>> duplicateAddressGroupsByCluster,
            Map<String, Boolean> hasDuplicateByCluster) {
        // Count view uses the same addressed-building filter as overlay/overview collectors.
        if (!AddressedBuildingMatcher.isAddressedBuilding(primitive)) {
            return;
        }

        StreetOption option = streetIndex.resolveForAddressPrimitive(primitive);
        if (option == null || !option.isValid()) {
            return;
        }

        String clusterId = option.getClusterId();
        countsByCluster.put(clusterId, countsByCluster.getOrDefault(clusterId, 0) + 1);

        String baseAddressKey = normalizeBaseAddressKey(primitive);
        if (baseAddressKey.isEmpty()) {
            return;
        }
        String cityKey = normalizeCityKey(primitive);

        Map<String, DuplicateAddressGroupStats> duplicateAddressGroups = duplicateAddressGroupsByCluster
                .computeIfAbsent(clusterId, key -> new HashMap<>());
        DuplicateAddressGroupStats groupStats = duplicateAddressGroups
                .computeIfAbsent(baseAddressKey, key -> new DuplicateAddressGroupStats());
        groupStats.addCandidate(cityKey);
        if (groupStats.hasDuplicateFor(cityKey)) {
            hasDuplicateByCluster.put(clusterId, true);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeBaseAddressKey(OsmPrimitive primitive) {
        String street = normalize(primitive.get("addr:street"));
        String postcode = normalize(primitive.get("addr:postcode"));
        String houseNumber = normalize(primitive.get("addr:housenumber"));
        if (street.isEmpty() || postcode.isEmpty() || houseNumber.isEmpty()) {
            return "";
        }
        return street.toLowerCase(Locale.ROOT)
                + "|" + postcode.toLowerCase(Locale.ROOT)
                + "|" + houseNumber.toLowerCase(Locale.ROOT);
    }

    private String normalizeCityKey(OsmPrimitive primitive) {
        return normalize(primitive.get("addr:city")).toLowerCase(Locale.ROOT);
    }

    /**
     * Aggregated duplicate-match statistics for one street+postcode+housenumber group in one street cluster.
     */
    private static final class DuplicateAddressGroupStats {
        private int totalCount;
        private int missingCityCount;
        private final Map<String, Integer> cityCounts = new HashMap<>();

        private void addCandidate(String cityKey) {
            totalCount++;
            if (cityKey == null || cityKey.isEmpty()) {
                missingCityCount++;
                return;
            }
            cityCounts.merge(cityKey, 1, Integer::sum);
        }

        private boolean hasDuplicateFor(String cityKey) {
            if (totalCount <= 1) {
                return false;
            }
            if (cityKey == null || cityKey.isEmpty()) {
                return true;
            }
            if (missingCityCount > 0) {
                return true;
            }
            return cityCounts.getOrDefault(cityKey, 0) > 1;
        }
    }
}
