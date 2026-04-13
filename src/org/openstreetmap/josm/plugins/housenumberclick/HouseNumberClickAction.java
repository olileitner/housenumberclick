package org.openstreetmap.josm.plugins.housenumberclick;

import java.awt.event.ActionEvent;
import java.util.List;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.I18n;

/**
 * Main toolbar/menu action that opens the street selection dialog and activates street mode.
 */
public class HouseNumberClickAction extends JosmAction {

    private final StreetModeController streetModeController;
    private final StreetSelectionDialog streetSelectionDialog;

    public HouseNumberClickAction() {
        super(I18n.tr("HouseNumberClick"), "housenumberclick", I18n.tr("Open HouseNumberClick street dialog"), null, true);
        this.streetModeController = new StreetModeController();
        this.streetSelectionDialog = new StreetSelectionDialog(streetModeController);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        if (dataSet == null) {
            StreetSelectionDialog.showNoDataSetMessage();
            return;
        }

        List<String> streetNames = StreetNameCollector.collectStreetNames(dataSet);
        List<String> detectedPostcodes = PostcodeCollector.collectVisiblePostcodes(dataSet);
        streetSelectionDialog.showDialog(dataSet, streetNames, detectedPostcodes);
    }
}
