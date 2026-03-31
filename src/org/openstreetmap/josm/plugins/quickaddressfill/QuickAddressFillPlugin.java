package org.openstreetmap.josm.plugins.quickaddressfill;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

public class QuickAddressFillPlugin extends Plugin {

    public QuickAddressFillPlugin(PluginInformation info) {
        super(info);
        MainApplication.getMenu().dataMenu.add(new QuickAddressFillAction());
    }
}

