package org.openstreetmap.josm.plugins.housenumberclick;

final class SplitContext {

    private final String street;
    private final String postcode;

    SplitContext(String street, String postcode) {
        this.street = normalize(street);
        this.postcode = normalize(postcode);
    }

    static SplitContext empty() {
        return new SplitContext("", "");
    }

    String getStreet() {
        return street;
    }

    String getPostcode() {
        return postcode;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

