package org.openstreetmap.josm.plugins.housenumberclick;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Value object describing one rendered house-number label entry in the overlay layer.
 */
final class HouseNumberOverlayEntry {

    private final OsmPrimitive primitive;
    private final String street;
    private final String postcode;
    private final String houseNumber;
    private final int numberPart;
    private final String suffixPart;
    private final EastNorth labelPoint;
    private final int stableIndex;

    HouseNumberOverlayEntry(OsmPrimitive primitive, String street, String postcode, String houseNumber, int numberPart, String suffixPart,
            EastNorth labelPoint, int stableIndex) {
        this.primitive = primitive;
        this.street = street;
        this.postcode = postcode;
        this.houseNumber = houseNumber;
        this.numberPart = numberPart;
        this.suffixPart = suffixPart;
        this.labelPoint = labelPoint;
        this.stableIndex = stableIndex;
    }

    OsmPrimitive getPrimitive() {
        return primitive;
    }

    String getStreet() {
        return street;
    }

    String getPostcode() {
        return postcode;
    }

    String getHouseNumber() {
        return houseNumber;
    }

    int getNumberPart() {
        return numberPart;
    }

    String getSuffixPart() {
        return suffixPart;
    }

    EastNorth getLabelPoint() {
        return labelPoint;
    }

    int getStableIndex() {
        return stableIndex;
    }
}
