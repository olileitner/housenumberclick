package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.Collections;

import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.I18n;

/**
 * Dockable JOSM sidebar dialog showing house numbers for the currently selected street.
 */
final class StreetNumbersToggleDialog extends ToggleDialog {

    private final StreetNumbersPanel panel;

    StreetNumbersToggleDialog(StreetNumbersPanel panel) {
        super(
                I18n.tr("House Numbers (Base Numbers only)"),
                "housenumberclick",
                I18n.tr("Show house numbers"),
                null,
                260
        );
        this.panel = panel;
        createLayout(panel, false, Collections.emptyList());
    }

    StreetNumbersPanel getPanel() {
        return panel;
    }
}


