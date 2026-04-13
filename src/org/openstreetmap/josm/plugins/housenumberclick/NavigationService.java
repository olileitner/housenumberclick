package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores and updates current street navigation state shared between dialog and map interactions.
 */
final class NavigationService {

    private String currentStreet = "";
    private String currentPostcode = "";
    private List<String> streetNavigationOrder = List.of();

    void updateFromSelection(StreetModeController.AddressSelection selection) {
        if (selection == null) {
            currentStreet = "";
            currentPostcode = "";
            return;
        }
        currentStreet = normalize(selection.getStreetName());
        currentPostcode = normalize(selection.getPostcode());
    }

    String getCurrentStreet() {
        return currentStreet;
    }

    String getCurrentPostcode() {
        return currentPostcode;
    }

    void setCurrentStreet(String streetName) {
        currentStreet = normalize(streetName);
    }

    void setStreetNavigationOrder(List<String> order) {
        streetNavigationOrder = order == null ? List.of() : List.copyOf(order);
    }

    List<String> getStreetNavigationOrder() {
        return new ArrayList<>(streetNavigationOrder);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
