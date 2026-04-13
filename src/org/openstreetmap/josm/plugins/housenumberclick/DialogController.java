package org.openstreetmap.josm.plugins.housenumberclick;

/**
 * Keeps dialog-side configuration state normalized and synchronized with controller callbacks.
 */
final class DialogController {

    int normalizeIncrementStep(int step) {
        return step == -2 || step == -1 || step == 1 || step == 2 ? step : 1;
    }

    int enforcePlusOneForLetterHouseNumbers(String houseNumber, int currentStep) {
        if (!containsLetter(houseNumber)) {
            return currentStep;
        }
        return 1;
    }

    boolean containsLetter(String value) {
        return value != null && value.matches(".*[A-Za-z].*");
    }

}
