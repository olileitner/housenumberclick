package org.openstreetmap.josm.plugins.housenumberclick;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Plugin entry point that wires the menu action and performs one-time toolbar migration.
 */
public class HouseNumberClickPlugin extends Plugin {

    private static final String TOOLBAR_ID = "housenumberclick";
    private static final String TOOLBAR_MIGRATION_PREF = "housenumberclick.toolbar.button.added.v1";

    public HouseNumberClickPlugin(PluginInformation info) {
        super(info);
        HouseNumberClickAction action = new HouseNumberClickAction();
        MainApplication.getMenu().dataMenu.add(action);
        ensureToolbarButtonAddedOnce();
    }

    private void ensureToolbarButtonAddedOnce() {
        if (Config.getPref().getBoolean(TOOLBAR_MIGRATION_PREF, false)) {
            return;
        }

        ToolbarPreferences toolbar = MainApplication.getToolbar();
        if (toolbar == null) {
            return;
        }

        toolbar.addCustomButton(TOOLBAR_ID, -1, false);
        Config.getPref().putBoolean(TOOLBAR_MIGRATION_PREF, true);
    }
}
