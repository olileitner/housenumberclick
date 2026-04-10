package org.openstreetmap.josm.plugins.housenumberclick;

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

    String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

