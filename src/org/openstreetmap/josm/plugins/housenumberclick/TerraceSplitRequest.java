package org.openstreetmap.josm.plugins.housenumberclick;

/**
 * Input object for row-house splitting that currently carries the requested part count.
 */
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
