package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Converts conflict analysis into table-oriented dialog rows for overwrite confirmation UI.
 */
final class ConflictDialogModelBuilder {

    /**
     * One row in the conflict confirmation table (field, existing value, proposed value).
     */
    static final class DialogRow {
        private final String field;
        private final String existing;
        private final String proposed;

        DialogRow(String field, String existing, String proposed) {
            this.field = field;
            this.existing = existing;
            this.proposed = proposed;
        }

        String getField() {
            return field;
        }

        String getExisting() {
            return existing;
        }

        String getProposed() {
            return proposed;
        }
    }

    /**
     * Immutable dialog data model containing all rows shown in overwrite confirmation.
     */
    static final class DialogModel {
        private final List<DialogRow> rows;

        DialogModel(List<DialogRow> rows) {
            this.rows = rows == null ? List.of() : Collections.unmodifiableList(rows);
        }

        List<DialogRow> getRows() {
            return rows;
        }

        boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    DialogModel build(AddressConflictService.ConflictAnalysis analysis, Function<String, String> valueFormatter) {
        if (analysis == null || analysis.getDifferingFields().isEmpty()) {
            return new DialogModel(List.of());
        }

        Function<String, String> formatter = valueFormatter == null ? this::nullSafe : valueFormatter;
        List<DialogRow> rows = new ArrayList<>();
        for (AddressConflictService.ConflictField field : analysis.getDifferingFields()) {
            rows.add(new DialogRow(
                    field.getKey(),
                    formatter.apply(field.getExistingValue()),
                    formatter.apply(field.getProposedValue())
            ));
        }
        return new DialogModel(rows);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
