package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.Way;

/**
 * Result object for row-house split execution with status message and resulting ways.
 */
final class TerraceSplitResult {

	private final boolean success;
	private final String message;
	private final List<Way> resultWays;

	private TerraceSplitResult(boolean success, String message, List<Way> resultWays) {
		this.success = success;
		this.message = message;
		this.resultWays = Collections.unmodifiableList(new ArrayList<>(resultWays));
	}

	static TerraceSplitResult success(String message, List<Way> resultWays) {
		return new TerraceSplitResult(true, message, resultWays == null ? List.of() : resultWays);
	}

	static TerraceSplitResult failure(String message) {
		return new TerraceSplitResult(false, message, List.of());
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
