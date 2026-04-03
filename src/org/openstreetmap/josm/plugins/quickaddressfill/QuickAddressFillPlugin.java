package org.openstreetmap.josm.plugins.quickaddressfill;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

public class QuickAddressFillPlugin extends Plugin {

    private static final String TOOLBAR_ID = "quickaddressfill";
    private static final String TOOLBAR_MIGRATION_PREF = "quickaddressfill.toolbar.button.added.v1";

    public QuickAddressFillPlugin(PluginInformation info) {
        super(info);
        QuickAddressFillAction action = new QuickAddressFillAction();
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
