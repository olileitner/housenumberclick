package org.openstreetmap.josm.plugins.housenumberclick;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates that all plugin classes have class Javadoc and that class-inventory.md stays in sync.
 */
public final class DocumentationConsistencyCheck {

    private static final Path SRC_DIR = Paths.get("src", "org", "openstreetmap", "josm", "plugins", "housenumberclick");
    private static final Path INVENTORY_FILE = Paths.get("docs", "class-inventory.md");

    private static final Set<String> CORE_CLASSES = Set.of(
            "HouseNumberClickPlugin",
            "HouseNumberClickAction",
            "StreetModeController",
            "HouseNumberClickStreetMapMode"
    );

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "^(\\s*)(?:public\\s+|protected\\s+|private\\s+)?(?:static\\s+)?(?:final\\s+|abstract\\s+)?(class|enum|record)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    );

    private static final Pattern INVENTORY_ROW = Pattern.compile(
            "^\\| `([^`]+)` \\| `([^`]+)` \\| `([^`]+)` \\| (Ja|Nein) \\| (.*) \\|$"
    );

    private DocumentationConsistencyCheck() {
        // Utility class
    }

    public static void main(String[] args) throws Exception {
        List<TypeInfo> sourceTypes = collectSourceTypes();
        List<String> errors = new ArrayList<>();

        for (TypeInfo type : sourceTypes) {
            if (type.classComment.isBlank()) {
                errors.add("Missing class Javadoc: " + type.qualifiedName + " (" + type.file + ":" + type.line + ")");
            }
        }

        Map<String, InventoryRow> inventoryRows = parseInventoryRows();
        validateInventorySync(sourceTypes, inventoryRows, errors);

        if (!errors.isEmpty()) {
            System.err.println("Documentation consistency check failed:");
            for (String error : errors) {
                System.err.println(" - " + error);
            }
            System.exit(1);
            return;
        }

        System.out.println("[PASS] Documentation consistency: class Javadocs and docs/class-inventory.md are in sync.");
    }

    private static List<TypeInfo> collectSourceTypes() throws IOException {
        if (!Files.isDirectory(SRC_DIR)) {
            throw new IOException("Source directory not found: " + SRC_DIR);
        }

        List<TypeInfo> result = new ArrayList<>();
        List<Path> javaFiles = new ArrayList<>();
        try (var stream = Files.list(SRC_DIR)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(javaFiles::add);
        }
        Collections.sort(javaFiles);

        for (Path javaFile : javaFiles) {
            result.addAll(collectTypesFromFile(javaFile));
        }

        result.sort((a, b) -> a.qualifiedName.compareToIgnoreCase(b.qualifiedName));
        return result;
    }

    private static List<TypeInfo> collectTypesFromFile(Path javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        List<TypeInfo> types = new ArrayList<>();
        List<ScopeEntry> scopeStack = new ArrayList<>();

        String relativeFile = javaFile.toString().replace('\\', '/');

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher declarationMatcher = TYPE_DECLARATION.matcher(line);
            if (!declarationMatcher.find()) {
                continue;
            }

            int indent = declarationMatcher.group(1).replace("\t", "    ").length();
            while (!scopeStack.isEmpty() && scopeStack.get(scopeStack.size() - 1).indent >= indent) {
                scopeStack.remove(scopeStack.size() - 1);
            }

            String kind = declarationMatcher.group(2);
            String simpleName = declarationMatcher.group(3);
            List<String> qualifiedParts = new ArrayList<>();
            for (ScopeEntry scope : scopeStack) {
                qualifiedParts.add(scope.name);
            }
            qualifiedParts.add(simpleName);
            String qualifiedName = String.join(".", qualifiedParts);

            String classComment = extractClassComment(lines, i);
            boolean core = CORE_CLASSES.contains(qualifiedName.split("\\.")[0]);

            types.add(new TypeInfo(qualifiedName, kind, relativeFile, classComment, core, i + 1));
            scopeStack.add(new ScopeEntry(indent, simpleName));
        }

        return types;
    }

    private static String extractClassComment(List<String> lines, int declarationLineIndex) {
        int lineIndex = declarationLineIndex - 1;
        while (lineIndex >= 0 && lines.get(lineIndex).trim().isEmpty()) {
            lineIndex--;
        }
        if (lineIndex < 0 || !lines.get(lineIndex).trim().endsWith("*/")) {
            return "";
        }

        int start = lineIndex;
        while (start >= 0 && !lines.get(start).contains("/**")) {
            start--;
        }
        if (start < 0) {
            return "";
        }

        List<String> cleaned = new ArrayList<>();
        for (int i = start; i <= lineIndex; i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("/**")) {
                line = line.substring(3);
            }
            if (line.endsWith("*/")) {
                line = line.substring(0, line.length() - 2);
            }
            if (line.startsWith("*")) {
                line = line.substring(1);
            }
            line = line.trim();
            if (!line.isEmpty()) {
                cleaned.add(line);
            }
        }

        return normalizeWhitespace(String.join(" ", cleaned));
    }

    private static Map<String, InventoryRow> parseInventoryRows() throws IOException {
        if (!Files.isRegularFile(INVENTORY_FILE)) {
            throw new IOException("Inventory file not found: " + INVENTORY_FILE);
        }

        Map<String, InventoryRow> rows = new HashMap<>();
        List<String> lines = Files.readAllLines(INVENTORY_FILE, StandardCharsets.UTF_8);

        for (String line : lines) {
            Matcher matcher = INVENTORY_ROW.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String qualifiedName = matcher.group(1);
            String kind = matcher.group(2);
            String file = matcher.group(3).replace('\\', '/');
            boolean core = "Ja".equals(matcher.group(4));
            String comment = normalizeWhitespace(unescapeTableText(matcher.group(5)));

            rows.put(qualifiedName, new InventoryRow(qualifiedName, kind, file, core, comment));
        }

        return rows;
    }

    private static void validateInventorySync(
            List<TypeInfo> sourceTypes,
            Map<String, InventoryRow> inventoryRows,
            List<String> errors
    ) {
        Set<String> sourceNames = new HashSet<>();

        for (TypeInfo type : sourceTypes) {
            sourceNames.add(type.qualifiedName);
            InventoryRow row = inventoryRows.get(type.qualifiedName);
            if (row == null) {
                errors.add("Missing inventory row for " + type.qualifiedName);
                continue;
            }

            if (!row.kind.equals(type.kind)) {
                errors.add("Inventory kind mismatch for " + type.qualifiedName + ": expected "
                        + type.kind + ", found " + row.kind);
            }
            if (!row.file.equals(type.file)) {
                errors.add("Inventory file mismatch for " + type.qualifiedName + ": expected "
                        + type.file + ", found " + row.file);
            }
            if (row.core != type.core) {
                errors.add("Inventory core marker mismatch for " + type.qualifiedName + ": expected "
                        + (type.core ? "Ja" : "Nein") + ", found " + (row.core ? "Ja" : "Nein"));
            }
            if (!row.comment.equals(type.classComment)) {
                errors.add("Inventory comment mismatch for " + type.qualifiedName);
            }
        }

        for (String inventoryClassName : inventoryRows.keySet()) {
            if (!sourceNames.contains(inventoryClassName)) {
                errors.add("Inventory contains removed/unknown class: " + inventoryClassName);
            }
        }
    }

    private static String unescapeTableText(String value) {
        return value.replace("\\|", "|").trim();
    }

    private static String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static final class ScopeEntry {
        private final int indent;
        private final String name;

        private ScopeEntry(int indent, String name) {
            this.indent = indent;
            this.name = name;
        }
    }

    private static final class TypeInfo {
        private final String qualifiedName;
        private final String kind;
        private final String file;
        private final String classComment;
        private final boolean core;
        private final int line;

        private TypeInfo(String qualifiedName, String kind, String file, String classComment, boolean core, int line) {
            this.qualifiedName = qualifiedName;
            this.kind = kind;
            this.file = file;
            this.classComment = classComment;
            this.core = core;
            this.line = line;
        }
    }

    private static final class InventoryRow {
        private final String kind;
        private final String file;
        private final boolean core;
        private final String comment;

        private InventoryRow(String qualifiedName, String kind, String file, boolean core, String comment) {
            this.kind = kind.toLowerCase(Locale.ROOT);
            this.file = file;
            this.core = core;
            this.comment = comment;
        }
    }
}

