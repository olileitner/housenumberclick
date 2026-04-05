package org.openstreetmap.josm.plugins.housenumberclick;

final class StreetHouseNumberCountRow {

    private final String streetName;
    private final int count;

    StreetHouseNumberCountRow(String streetName, int count) {
        this.streetName = normalize(streetName);
        this.count = Math.max(0, count);
    }

    String getStreetName() {
        return streetName;
    }

    int getCount() {
        return count;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

