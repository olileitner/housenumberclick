package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class IntersectionScanResult {

    private final boolean success;
    private final String message;
    private final List<IntersectionPoint> intersections;

    private IntersectionScanResult(boolean success, String message, List<IntersectionPoint> intersections) {
        this.success = success;
        this.message = message;
        this.intersections = Collections.unmodifiableList(new ArrayList<>(intersections));
    }

    static IntersectionScanResult success(List<IntersectionPoint> intersections) {
        return new IntersectionScanResult(true, "Intersections computed", intersections == null ? List.of() : intersections);
    }

    static IntersectionScanResult failure(String message) {
        return new IntersectionScanResult(false, message, List.of());
    }

    boolean isSuccess() {
        return success;
    }

    String getMessage() {
        return message;
    }

    List<IntersectionPoint> getIntersections() {
        return intersections;
    }
}

