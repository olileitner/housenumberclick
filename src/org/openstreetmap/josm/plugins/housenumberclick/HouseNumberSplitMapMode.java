package org.openstreetmap.josm.plugins.housenumberclick;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.I18n;

final class HouseNumberSplitMapMode extends MapMode {

    private final StreetModeController controller;
    private LatLon dragStart;
    private boolean flowCompleted;

    HouseNumberSplitMapMode(StreetModeController controller) {
        super(
                I18n.tr("HouseNumberClick Split Mode"),
                "housenumberclick",
                I18n.tr("Drag a line across one selected building to split it"),
                Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        );
        this.controller = controller;
    }

    @Override
    public void enterMode() {
        super.enterMode();
        flowCompleted = false;
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapView != null) {
            map.mapView.addMouseListener(this);
            map.mapView.addMouseMotionListener(this);
        }
    }

    @Override
    public void exitMode() {
        MapFrame map = MainApplication.getMap();
        if (map != null && map.mapView != null) {
            map.mapView.removeMouseListener(this);
            map.mapView.removeMouseMotionListener(this);
            map.mapView.setCursor(Cursor.getDefaultCursor());
        }
        if (!flowCompleted) {
            flowCompleted = true;
            controller.onInternalSplitFlowFinished(StreetModeController.SplitFlowOutcome.CANCELLED);
        }
        dragStart = null;
        super.exitMode();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (!isLeftButton(e)) {
            return;
        }
        dragStart = toLatLon(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isLeftButton(e)) {
            completeWithOutcome(StreetModeController.SplitFlowOutcome.CANCELLED);
            dragStart = null;
            return;
        }
        if (dragStart == null) {
            dragStart = null;
            return;
        }

        LatLon dragEnd = toLatLon(e);
        if (dragEnd != null) {
            SingleSplitResult result = controller.executeInternalSingleSplit(dragStart, dragEnd);
            completeWithOutcome(result.isSuccess()
                    ? StreetModeController.SplitFlowOutcome.SUCCESS
                    : StreetModeController.SplitFlowOutcome.FAILED);
        } else {
            completeWithOutcome(StreetModeController.SplitFlowOutcome.CANCELLED);
        }
        dragStart = null;
    }

    private void completeWithOutcome(StreetModeController.SplitFlowOutcome outcome) {
        if (flowCompleted) {
            return;
        }
        flowCompleted = true;
        controller.onInternalSplitFlowFinished(outcome);
    }

    private boolean isLeftButton(MouseEvent event) {
        return event != null && event.getButton() == MouseEvent.BUTTON1;
    }

    private LatLon toLatLon(MouseEvent event) {
        MapFrame map = MainApplication.getMap();
        if (map == null || map.mapView == null || event == null) {
            return null;
        }
        return map.mapView.getLatLon(event.getX(), event.getY());
    }
}


