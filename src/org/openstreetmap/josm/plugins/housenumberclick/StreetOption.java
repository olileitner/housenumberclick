package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.Objects;

/**
 * Immutable street descriptor used to separate OSM base street names from UI disambiguation labels.
 */
final class StreetOption {

    private final String baseStreetName;
    private final String displayStreetName;
    private final String clusterId;

    StreetOption(String baseStreetName, String displayStreetName, String clusterId) {
        this.baseStreetName = normalize(baseStreetName);
        this.displayStreetName = normalize(displayStreetName);
        this.clusterId = normalize(clusterId);
    }

    String getBaseStreetName() {
        return baseStreetName;
    }

    String getDisplayStreetName() {
        return displayStreetName;
    }

    String getClusterId() {
        return clusterId;
    }

    boolean isValid() {
        return !baseStreetName.isEmpty() && !displayStreetName.isEmpty() && !clusterId.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StreetOption)) {
            return false;
        }
        StreetOption that = (StreetOption) other;
        return baseStreetName.equals(that.baseStreetName)
                && displayStreetName.equals(that.displayStreetName)
                && clusterId.equals(that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseStreetName, displayStreetName, clusterId);
    }

    @Override
    public String toString() {
        return displayStreetName;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

