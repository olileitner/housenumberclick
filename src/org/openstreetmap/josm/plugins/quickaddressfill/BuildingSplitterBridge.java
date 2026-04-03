package org.openstreetmap.josm.plugins.quickaddressfill;

import java.util.Locale;

import javax.swing.Action;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

final class BuildingSplitterBridge {

    private static final String TARGET_PLUGIN_NAME = "buildingsplitter";
    private static final String ADDRESS_CONTEXT_BRIDGE_CLASS =
        "org.openstreetmap.josm.plugins.buildingsplitter.AddressContextBridge";
    private static final String HANDOFF_STREET_KEY = "quickaddressfill.buildingsplitter.handoff.street";
    private static final String HANDOFF_POSTCODE_KEY = "quickaddressfill.buildingsplitter.handoff.postcode";
    private static final String HANDOFF_PENDING_KEY = "quickaddressfill.buildingsplitter.handoff.pending";
    private static final String FORCE_PREFERENCE_FALLBACK_KEY =
        "quickaddressfill.buildingsplitter.forcePreferenceFallback";

    private BuildingSplitterBridge() {
        // Utility class
    }

    static boolean activateBuildingSplitter() {
        return activateBuildingSplitter("", "");
    }

    static boolean activateBuildingSplitter(String street, String postcode) {
        try {
            if (!BuildingSplitterDetector.isBuildingSplitterAvailable()) {
                Logging.info("QuickAddressFill: BuildingSplitter activation skipped because plugin is not available.");
                return false;
            }

            publishAddressContext(street, postcode);

            MapFrame map = MainApplication.getMap();
            if (map == null || map.allMapModeButtons == null) {
                Logging.info("QuickAddressFill: BuildingSplitter activation failed because map UI is unavailable.");
                return false;
            }

            for (IconToggleButton button : map.allMapModeButtons) {
                if (button == null) {
                    continue;
                }
                Action action = button.getAction();
                if (!(action instanceof MapMode)) {
                    continue;
                }

                MapMode mapMode = (MapMode) action;
                if (!isBuildingSplitterMapMode(mapMode, button.getActionName())) {
                    continue;
                }

                if (map.selectMapMode(mapMode)) {
                    Logging.info("QuickAddressFill: BuildingSplitter map mode detected and activated.");
                    return true;
                }
            }
            Logging.info("QuickAddressFill: BuildingSplitter activation failed after scanning map modes.");
        } catch (RuntimeException ex) {
            Logging.info("QuickAddressFill: BuildingSplitter activation failed due to runtime error: {0}", ex.getMessage());
            return false;
        }

        return false;
    }

    private static void publishAddressContext(String street, String postcode) {
        String normalizedStreet = normalizeHandoffValue(street);
        String normalizedPostcode = normalizeHandoffValue(postcode);
        Logging.info("QuickAddressFill: Address context handoff attempt started.");

        if (Config.getPref().getBoolean(FORCE_PREFERENCE_FALLBACK_KEY, false)) {
            Logging.info("QuickAddressFill: Reflection handoff disabled by QA preference; using fallback.");
            writePreferenceFallback(normalizedStreet, normalizedPostcode);
            return;
        }

        try {
            Class<?> bridgeClass = Class.forName(ADDRESS_CONTEXT_BRIDGE_CLASS);
            bridgeClass
                .getMethod("setAddressContext", String.class, String.class)
                .invoke(null, normalizedStreet, normalizedPostcode);
            Logging.info("QuickAddressFill: Address context reflection handoff succeeded.");
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            Logging.info("QuickAddressFill: Address context reflection handoff unavailable.");
            writePreferenceFallback(normalizedStreet, normalizedPostcode);
        } catch (ReflectiveOperationException | LinkageError ex) {
            Logging.warn("QuickAddressFill: Address context reflection handoff failed.");
            Logging.debug(ex);
            writePreferenceFallback(normalizedStreet, normalizedPostcode);
        }
    }

    private static void writePreferenceFallback(String street, String postcode) {
        String normalizedStreet = normalizeHandoffValue(street);
        String normalizedPostcode = normalizeHandoffValue(postcode);
        if (normalizedStreet.isEmpty() && normalizedPostcode.isEmpty()) {
            Config.getPref().put(HANDOFF_STREET_KEY, null);
            Config.getPref().put(HANDOFF_POSTCODE_KEY, null);
            Config.getPref().put(HANDOFF_PENDING_KEY, null);
            Logging.info("QuickAddressFill: Preference fallback not written because context is empty.");
            return;
        }

        Config.getPref().put(HANDOFF_STREET_KEY, normalizedStreet);
        Config.getPref().put(HANDOFF_POSTCODE_KEY, normalizedPostcode);
        Config.getPref().putBoolean(HANDOFF_PENDING_KEY, true);
        Logging.info("QuickAddressFill: Address context preference fallback written.");
    }

    private static boolean isBuildingSplitterMapMode(MapMode mapMode, String actionName) {
        if (mapMode == null) {
            return false;
        }

        // Prioritize user-facing labels first to reduce accidental matches.
        String modeName = normalize((String) mapMode.getValue(Action.NAME));
        if (containsBuildingSplitter(modeName)) {
            return true;
        }

        String normalizedActionName = normalize(actionName);
        if (containsBuildingSplitter(normalizedActionName)) {
            return true;
        }

        String simpleName = normalize(mapMode.getClass().getSimpleName());
        if (containsBuildingSplitter(simpleName)) {
            return true;
        }

        String className = normalize(mapMode.getClass().getName());
        return containsBuildingSplitter(className);
    }

    private static boolean containsBuildingSplitter(String value) {
        if (value.isEmpty()) {
            return false;
        }

        String collapsed = collapseSeparators(value);

        if (isStrongBuildingSplitterMatch(value, collapsed)) {
            return true;
        }

        // Fallback only after stronger checks fail.
        return value.contains("building") && value.contains("split");
    }

    private static boolean isStrongBuildingSplitterMatch(String normalizedValue, String collapsedValue) {
        return TARGET_PLUGIN_NAME.equals(normalizedValue)
                || TARGET_PLUGIN_NAME.equals(collapsedValue)
                || normalizedValue.contains(TARGET_PLUGIN_NAME)
                || collapsedValue.contains(TARGET_PLUGIN_NAME);
    }

    private static String collapseSeparators(String value) {
        return value.replace("-", "").replace("_", "").replace(" ", "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeHandoffValue(String value) {
        return value == null ? "" : value.trim();
    }
}
