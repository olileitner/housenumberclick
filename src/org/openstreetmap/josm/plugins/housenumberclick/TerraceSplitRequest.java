package org.openstreetmap.josm.plugins.housenumberclick;

final class TerraceSplitRequest {

    private static final int MIN_PARTS = 2;

    private final int parts;

    TerraceSplitRequest(int parts) {
        this.parts = parts;
    }

    int getParts() {
        return parts;
    }

    boolean hasValidParts() {
        return parts >= MIN_PARTS;
    }
}


