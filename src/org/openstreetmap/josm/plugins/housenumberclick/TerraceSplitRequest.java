package org.openstreetmap.josm.plugins.housenumberclick;

final class TerraceSplitRequest {

    private final int parts;

    TerraceSplitRequest(int parts) {
        this.parts = parts;
    }

    int getParts() {
        return parts;
    }

    boolean hasValidParts() {
        return parts >= 2;
    }
}


