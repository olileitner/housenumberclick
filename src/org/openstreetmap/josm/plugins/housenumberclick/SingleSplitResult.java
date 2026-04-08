package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;

final class SingleSplitResult {

    private final boolean success;
    private final String message;
    private final List<Way> resultWays;

    private SingleSplitResult(boolean success, String message, List<Way> resultWays) {
        this.success = success;
        this.message = message;
        this.resultWays = Collections.unmodifiableList(new ArrayList<>(resultWays));
    }

    static SingleSplitResult success(String message, List<Way> resultWays) {
        return new SingleSplitResult(true, message, resultWays == null ? List.of() : resultWays);
    }

    static SingleSplitResult failure(String message) {
        return new SingleSplitResult(false, message, List.of());
    }

    boolean isSuccess() {
        return success;
    }

    String getMessage() {
        return message;
    }

    List<Way> getResultWays() {
        return resultWays;
    }
}

