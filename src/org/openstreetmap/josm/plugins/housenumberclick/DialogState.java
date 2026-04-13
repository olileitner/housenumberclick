package org.openstreetmap.josm.plugins.housenumberclick;

/**
 * Immutable snapshot of the dialog input values used to compare and restore UI state.
 */
final class DialogState {

    private String rememberedStreet;
    private String rememberedPostcode;
    private String rememberedBuildingType;
    private String rememberedHouseNumber;
    private int rememberedIncrementStep;
    private boolean rememberedHouseNumberLayerEnabled;
    private boolean rememberedConnectionLinesEnabled;
    private boolean rememberedConnectionLinesPreference;
    private boolean rememberedSeparateEvenOddLinesEnabled;
    private boolean rememberedSeparateEvenOddLinesPreference;
    private boolean rememberedHouseNumberOverviewEnabled;
    private boolean rememberedStreetHouseNumberCountsEnabled;
    private boolean rememberedZoomToSelectedStreetEnabled;
    private boolean rememberedSplitMakeRectangular;

    DialogState(String defaultHouseNumber) {
        rememberedHouseNumber = defaultHouseNumber == null ? "" : defaultHouseNumber;
        rememberedIncrementStep = 1;
        rememberedConnectionLinesPreference = true;
        rememberedSeparateEvenOddLinesPreference = true;
    }

    String getRememberedStreet() {
        return rememberedStreet;
    }

    void setRememberedStreet(String rememberedStreet) {
        this.rememberedStreet = rememberedStreet;
    }

    String getRememberedPostcode() {
        return rememberedPostcode;
    }

    void setRememberedPostcode(String rememberedPostcode) {
        this.rememberedPostcode = rememberedPostcode;
    }

    String getRememberedBuildingType() {
        return rememberedBuildingType;
    }

    void setRememberedBuildingType(String rememberedBuildingType) {
        this.rememberedBuildingType = rememberedBuildingType;
    }

    String getRememberedHouseNumber() {
        return rememberedHouseNumber;
    }

    void setRememberedHouseNumber(String rememberedHouseNumber) {
        this.rememberedHouseNumber = rememberedHouseNumber;
    }

    int getRememberedIncrementStep() {
        return rememberedIncrementStep;
    }

    void setRememberedIncrementStep(int rememberedIncrementStep) {
        this.rememberedIncrementStep = rememberedIncrementStep;
    }

    boolean isRememberedHouseNumberLayerEnabled() {
        return rememberedHouseNumberLayerEnabled;
    }

    void setRememberedHouseNumberLayerEnabled(boolean rememberedHouseNumberLayerEnabled) {
        this.rememberedHouseNumberLayerEnabled = rememberedHouseNumberLayerEnabled;
    }

    boolean isRememberedConnectionLinesEnabled() {
        return rememberedConnectionLinesEnabled;
    }

    void setRememberedConnectionLinesEnabled(boolean rememberedConnectionLinesEnabled) {
        this.rememberedConnectionLinesEnabled = rememberedConnectionLinesEnabled;
    }

    boolean isRememberedConnectionLinesPreference() {
        return rememberedConnectionLinesPreference;
    }

    void setRememberedConnectionLinesPreference(boolean rememberedConnectionLinesPreference) {
        this.rememberedConnectionLinesPreference = rememberedConnectionLinesPreference;
    }

    boolean isRememberedSeparateEvenOddLinesEnabled() {
        return rememberedSeparateEvenOddLinesEnabled;
    }

    void setRememberedSeparateEvenOddLinesEnabled(boolean rememberedSeparateEvenOddLinesEnabled) {
        this.rememberedSeparateEvenOddLinesEnabled = rememberedSeparateEvenOddLinesEnabled;
    }

    boolean isRememberedSeparateEvenOddLinesPreference() {
        return rememberedSeparateEvenOddLinesPreference;
    }

    void setRememberedSeparateEvenOddLinesPreference(boolean rememberedSeparateEvenOddLinesPreference) {
        this.rememberedSeparateEvenOddLinesPreference = rememberedSeparateEvenOddLinesPreference;
    }

    boolean isRememberedHouseNumberOverviewEnabled() {
        return rememberedHouseNumberOverviewEnabled;
    }

    void setRememberedHouseNumberOverviewEnabled(boolean rememberedHouseNumberOverviewEnabled) {
        this.rememberedHouseNumberOverviewEnabled = rememberedHouseNumberOverviewEnabled;
    }

    boolean isRememberedStreetHouseNumberCountsEnabled() {
        return rememberedStreetHouseNumberCountsEnabled;
    }

    void setRememberedStreetHouseNumberCountsEnabled(boolean rememberedStreetHouseNumberCountsEnabled) {
        this.rememberedStreetHouseNumberCountsEnabled = rememberedStreetHouseNumberCountsEnabled;
    }

    boolean isRememberedZoomToSelectedStreetEnabled() {
        return rememberedZoomToSelectedStreetEnabled;
    }

    void setRememberedZoomToSelectedStreetEnabled(boolean rememberedZoomToSelectedStreetEnabled) {
        this.rememberedZoomToSelectedStreetEnabled = rememberedZoomToSelectedStreetEnabled;
    }

    boolean isRememberedSplitMakeRectangular() {
        return rememberedSplitMakeRectangular;
    }

    void setRememberedSplitMakeRectangular(boolean rememberedSplitMakeRectangular) {
        this.rememberedSplitMakeRectangular = rememberedSplitMakeRectangular;
    }
}
