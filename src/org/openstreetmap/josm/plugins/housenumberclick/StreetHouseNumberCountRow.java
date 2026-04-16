package org.openstreetmap.josm.plugins.housenumberclick;

/**
 * Row model for per-street house-number counts, including duplicate marker information.
 */
final class StreetHouseNumberCountRow {

    private final StreetOption streetOption;
    private final int count;
    private final boolean hasDuplicate;

    StreetHouseNumberCountRow(String streetName, int count, boolean hasDuplicate) {
        this(new StreetOption(streetName, streetName, normalizeStatic(streetName).toLowerCase(java.util.Locale.ROOT)), count, hasDuplicate);
    }

    StreetHouseNumberCountRow(StreetOption streetOption, int count, boolean hasDuplicate) {
        this.streetOption = streetOption;
        this.count = Math.max(0, count);
        this.hasDuplicate = hasDuplicate;
    }

    String getStreetName() {
        return getDisplayStreetName();
    }

    String getDisplayStreetName() {
        if (streetOption == null) {
            return "";
        }
        return normalize(streetOption.getDisplayStreetName());
    }

    String getBaseStreetName() {
        if (streetOption == null) {
            return "";
        }
        return normalize(streetOption.getBaseStreetName());
    }

    String getClusterId() {
        if (streetOption == null) {
            return "";
        }
        return normalize(streetOption.getClusterId());
    }

    StreetOption getStreetOption() {
        return streetOption;
    }

    int getCount() {
        return count;
    }

    boolean hasDuplicate() {
        return hasDuplicate;
    }

    String getDisplayCount() {
        return hasDuplicate ? count + " (dup)" : Integer.toString(count);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeStatic(String value) {
        return value == null ? "" : value.trim();
    }
}
