package org.openstreetmap.josm.plugins.housenumberclick;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Centralized plugin preference access using the shared housenumberclick.* namespace.
 */
final class HouseNumberClickPreferences {

    static final String PREFIX = "housenumberclick.";

    static final BooleanProperty TOOLBAR_BUTTON_ADDED =
            new BooleanProperty(PREFIX + "toolbar.button.added.v1", false);

    static final IntegerProperty OVERLAY_MODE =
            new IntegerProperty(PREFIX + "overlay.mode", OverlayMode.CLASSIC.preferenceValue);
    private static final String LEGACY_SHOW_OVERLAY_KEY = PREFIX + "showOverlay";
    static final BooleanProperty OVERLAY_MODE_MIGRATION_DONE =
            new BooleanProperty(PREFIX + "overlay.mode.migrated.v1", false);

    static final BooleanProperty SHOW_CONNECTION_LINES =
            new BooleanProperty(PREFIX + "dialog.showConnectionLines", true);
    static final BooleanProperty SHOW_SEPARATE_EVEN_ODD_LINES =
            new BooleanProperty(PREFIX + "dialog.showSeparateEvenOddLines", true);
    static final BooleanProperty SHOW_HOUSE_NUMBER_OVERVIEW =
            new BooleanProperty(PREFIX + "dialog.showHouseNumberOverview", false);
    static final BooleanProperty SHOW_STREET_HOUSE_NUMBER_COUNTS =
            new BooleanProperty(PREFIX + "dialog.showStreetHouseNumberCounts", false);
    static final BooleanProperty ZOOM_TO_SELECTED_STREET =
            new BooleanProperty(PREFIX + "dialog.zoomToSelectedStreet", true);
    static final BooleanProperty ZOOM_TO_NUMBERED_BUILDINGS_ONLY =
            new BooleanProperty(PREFIX + "dialog.zoomToNumberedBuildingsOnly", true);
    static final BooleanProperty SPLIT_MAKE_RECTANGULAR =
            new BooleanProperty(PREFIX + "dialog.splitMakeRectangular", false);
    static final BooleanProperty APPLY_TYPE_TO_ALL =
            new BooleanProperty(PREFIX + "dialog.applyTypeToAll", false);

    static final IntegerProperty HOUSE_NUMBER_INCREMENT_STEP =
            new IntegerProperty(PREFIX + "dialog.houseNumberIncrementStep", 1);
    static final IntegerProperty TERRACE_PARTS =
            new IntegerProperty(PREFIX + "dialog.terraceParts", 2);
    static final IntegerProperty COMPLETENESS_MISSING_FIELD =
            new IntegerProperty(PREFIX + "dialog.completenessMissingField", BuildingOverviewLayer.MissingField.POSTCODE.ordinal());

    private HouseNumberClickPreferences() {
        // Utility class
    }

    enum OverlayMode {
        OFF(0),
        CLASSIC(1),
        CLUSTERED(2);

        private final int preferenceValue;

        OverlayMode(int preferenceValue) {
            this.preferenceValue = preferenceValue;
        }

        static OverlayMode fromPreferenceValue(int preferenceValue) {
            if (preferenceValue == OFF.preferenceValue) {
                return OFF;
            }
            if (preferenceValue == CLUSTERED.preferenceValue) {
                return CLUSTERED;
            }
            return CLASSIC;
        }
    }

    static OverlayMode getOverlayMode() {
        migrateLegacyOverlayPreferenceIfNeeded();
        return OverlayMode.fromPreferenceValue(OVERLAY_MODE.get());
    }

    static void setOverlayMode(OverlayMode mode) {
        OverlayMode normalized = mode != null ? mode : OverlayMode.CLASSIC;
        OVERLAY_MODE.put(normalized.preferenceValue);
    }

    static int normalizeIncrementStep(int step) {
        return step == -2 || step == -1 || step == 1 || step == 2 ? step : 1;
    }

    static int normalizeTerraceParts(int value) {
        return Math.max(2, value);
    }

    static BuildingOverviewLayer.MissingField getCompletenessMissingField() {
        int ordinal = COMPLETENESS_MISSING_FIELD.get();
        BuildingOverviewLayer.MissingField[] values = BuildingOverviewLayer.MissingField.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return BuildingOverviewLayer.MissingField.POSTCODE;
        }
        return values[ordinal];
    }

    static void setCompletenessMissingField(BuildingOverviewLayer.MissingField missingField) {
        BuildingOverviewLayer.MissingField normalized = missingField != null
                ? missingField
                : BuildingOverviewLayer.MissingField.POSTCODE;
        COMPLETENESS_MISSING_FIELD.put(normalized.ordinal());
    }

    private static void migrateLegacyOverlayPreferenceIfNeeded() {
        if (OVERLAY_MODE_MIGRATION_DONE.get()) {
            return;
        }

        if (Config.getPref() != null && Config.getPref().get(LEGACY_SHOW_OVERLAY_KEY, null) != null) {
            boolean legacyEnabled = Config.getPref().getBoolean(LEGACY_SHOW_OVERLAY_KEY, true);
            setOverlayMode(legacyEnabled ? OverlayMode.CLASSIC : OverlayMode.OFF);
            Config.getPref().put(LEGACY_SHOW_OVERLAY_KEY, null);
        }
        OVERLAY_MODE_MIGRATION_DONE.put(true);
    }
}


