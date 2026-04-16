package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores and updates current street navigation state shared between dialog and map interactions.
 */
final class NavigationService {

    private StreetOption currentStreetOption;
    private String currentStreetBase = "";
    private String currentStreetDisplay = "";
    private String currentStreetClusterId = "";
    private String currentPostcode = "";
    private List<StreetOption> streetNavigationOrder = List.of();

    void updateFromSelection(StreetModeController.AddressSelection selection) {
        if (selection == null) {
            currentStreetOption = null;
            currentStreetBase = "";
            currentStreetDisplay = "";
            currentStreetClusterId = "";
            currentPostcode = "";
            return;
        }
        setCurrentStreetOption(new StreetOption(
                selection.getStreetName(),
                selection.getDisplayStreetName(),
                selection.getStreetClusterId()
        ));
        currentPostcode = normalize(selection.getPostcode());
    }

    StreetOption getCurrentStreetOption() {
        return currentStreetOption;
    }

    String getCurrentStreet() {
        return currentStreetBase;
    }

    String getCurrentStreetDisplay() {
        return currentStreetDisplay;
    }

    String getCurrentStreetClusterId() {
        return currentStreetClusterId;
    }

    String getCurrentPostcode() {
        return currentPostcode;
    }

    void setCurrentStreet(String streetName) {
        String normalizedStreet = normalize(streetName);
        currentStreetOption = normalizedStreet.isEmpty()
                ? null
                : new StreetOption(normalizedStreet, normalizedStreet, "");
        currentStreetBase = normalizedStreet;
        currentStreetDisplay = normalizedStreet;
        currentStreetClusterId = "";
    }

    void setCurrentStreetOption(StreetOption option) {
        if (option == null) {
            setCurrentStreet("");
            return;
        }
        currentStreetOption = option;
        currentStreetBase = normalize(option.getBaseStreetName());
        currentStreetDisplay = normalize(option.getDisplayStreetName());
        currentStreetClusterId = normalize(option.getClusterId());
    }

    void setStreetNavigationOrder(List<StreetOption> order) {
        streetNavigationOrder = order == null ? List.of() : List.copyOf(order);
    }

    List<StreetOption> getStreetNavigationOrder() {
        return new ArrayList<>(streetNavigationOrder);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
