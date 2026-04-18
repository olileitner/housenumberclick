package org.openstreetmap.josm.plugins.housenumberclick;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Map layer that visualizes postcode distribution for quick QA checks in building and
 * schematic dense-area rendering modes.
 */
final class PostcodeOverviewLayer extends Layer {

    enum OverviewMode {
        BUILDINGS,
        SCHEMATIC
    }

    private static final Color MISSING_POSTCODE_COLOR = BuildingOverviewLayer.NO_ADDRESS_DATA_COLOR;
    private static final Color LEGEND_BACKGROUND_COLOR = new Color(248, 248, 248, 215);
    private static final int LEGEND_PADDING = 8;
    private static final int LEGEND_ROW_HEIGHT = 16;
    private static final int LEGEND_SWATCH_SIZE = 11;
    static final int TOP_POSTCODE_LEGEND_LIMIT = 5;
    private static final int POSTCODE_FILL_ALPHA = 190;
    private static final double ISOLATION_DISTANCE_METERS = 500.0;
    private static final int MIN_SCHEMATIC_CLUSTER_SIZE = 4;
    private static final double SCHEMATIC_BUFFER_RADIUS_METERS = 120.0;
    private static final Color[] POSTCODE_PALETTE = new Color[] {
            new Color(86, 180, 233),
            new Color(230, 159, 0),
            new Color(204, 121, 167),
            new Color(0, 158, 115),
            new Color(0, 114, 178),
            new Color(213, 94, 0),
            new Color(240, 228, 66),
            new Color(152, 78, 163),
            new Color(255, 127, 0),
            new Color(77, 175, 74),
            new Color(55, 126, 184),
            new Color(228, 26, 28),
            new Color(166, 86, 40),
            new Color(247, 129, 191),
            new Color(102, 194, 165),
            new Color(27, 158, 119)
    };

    private final DataSet dataSet;
    private final BuildingOverviewCollector collector;
    private final OverviewMode overviewMode;

    PostcodeOverviewLayer(DataSet dataSet) {
        this(dataSet, OverviewMode.BUILDINGS);
    }

    PostcodeOverviewLayer(DataSet dataSet, OverviewMode overviewMode) {
        super(I18n.tr("Postcode overview"));
        this.dataSet = dataSet;
        this.collector = new BuildingOverviewCollector();
        this.overviewMode = overviewMode != null ? overviewMode : OverviewMode.BUILDINGS;
    }

    @Override
    public void paint(Graphics2D graphics, MapView mapView, Bounds bounds) {
        if (graphics == null || mapView == null || dataSet == null) {
            return;
        }

        List<BuildingOverviewCollector.BuildingOverviewEntry> entries = collector.collect(dataSet);
        if (entries.isEmpty()) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (overviewMode == OverviewMode.SCHEMATIC) {
            drawSchematicAreas(g, mapView, entries);
        } else {
            for (BuildingOverviewCollector.BuildingOverviewEntry entry : entries) {
                drawPrimitive(g, mapView, entry.getPrimitive());
            }
        }
        drawLegend(g, mapView, entries, overviewMode);
        g.dispose();
    }

    private void drawLegend(Graphics2D g, MapView mapView, List<BuildingOverviewCollector.BuildingOverviewEntry> entries,
            OverviewMode mode) {
        if (mapView.getWidth() < 180 || mapView.getHeight() < 120) {
            return;
        }

        Map<String, Integer> postcodeCounts = collectPostcodeCounts(entries);
        int missingCount = postcodeCounts.getOrDefault("", 0);
        List<String> topPostcodes = sortPostcodesForLegend(postcodeCounts, TOP_POSTCODE_LEGEND_LIMIT);
        boolean schematicLegend = mode == OverviewMode.SCHEMATIC;

        String title = I18n.tr("Postcode");
        int contentRows = 1 + topPostcodes.size() + (schematicLegend ? 1 : 0);
        int legendHeight = LEGEND_PADDING * 2 + LEGEND_ROW_HEIGHT + (contentRows * LEGEND_ROW_HEIGHT);
        int legendWidth = Math.max(210, g.getFontMetrics().stringWidth(title) + 80);
        int legendX = Math.max(8, mapView.getWidth() - legendWidth - 10);
        int legendY = 10;

        g.setColor(LEGEND_BACKGROUND_COLOR);
        g.fillRoundRect(legendX, legendY, legendWidth, legendHeight, 8, 8);

        int textBaseX = legendX + LEGEND_PADDING;
        int rowY = legendY + LEGEND_PADDING + 12;
        g.setColor(Color.BLACK);
        g.drawString(title, textBaseX, rowY);

        rowY += LEGEND_ROW_HEIGHT;
        drawLegendRow(g, textBaseX, rowY, resolveColorForPostcode(""),
                I18n.tr("No postcode ({0})", missingCount));

        for (String postcode : topPostcodes) {
            rowY += LEGEND_ROW_HEIGHT;
            int count = postcodeCounts.getOrDefault(postcode, 0);
            drawLegendRow(g, textBaseX, rowY, resolveColorForPostcode(postcode),
                    I18n.tr("{0} ({1})", postcode, count));
        }

        if (schematicLegend) {
            rowY += LEGEND_ROW_HEIGHT;
            g.setColor(Color.BLACK);
            g.drawString(I18n.tr("Schematic: dense postcode areas"), textBaseX, rowY);
        }
    }

    private void drawLegendRow(Graphics2D g, int textBaseX, int rowY, Color swatchColor, String label) {
        int swatchY = rowY - LEGEND_SWATCH_SIZE + 3;
        g.setColor(swatchColor);
        g.fillRect(textBaseX, swatchY, LEGEND_SWATCH_SIZE, LEGEND_SWATCH_SIZE);
        g.setColor(Color.BLACK);
        g.drawString(label, textBaseX + LEGEND_SWATCH_SIZE + 6, rowY);
    }

    private Map<String, Integer> collectPostcodeCounts(List<BuildingOverviewCollector.BuildingOverviewEntry> entries) {
        Map<String, Integer> counts = new HashMap<>();
        if (entries == null) {
            return counts;
        }
        for (BuildingOverviewCollector.BuildingOverviewEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String postcode = normalize(entry.getPrimitive() == null ? null : entry.getPrimitive().get("addr:postcode"));
            counts.merge(postcode, 1, Integer::sum);
        }
        return counts;
    }

    static List<String> sortPostcodesForLegend(Map<String, Integer> postcodeCounts, int limit) {
        if (postcodeCounts == null || postcodeCounts.isEmpty() || limit <= 0) {
            return List.of();
        }

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : postcodeCounts.entrySet()) {
            if (entry == null) {
                continue;
            }
            String postcode = entry.getKey() == null ? "" : entry.getKey().trim();
            if (postcode.isEmpty()) {
                continue;
            }
            ranked.add(Map.entry(postcode, entry.getValue() == null ? 0 : entry.getValue()));
        }

        ranked.sort(Comparator
                .<Map.Entry<String, Integer>>comparingInt(e -> e.getValue() == null ? 0 : e.getValue())
                .reversed()
                .thenComparing(e -> e.getKey().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(Map.Entry::getKey));

        int resultSize = Math.min(limit, ranked.size());
        List<String> result = new ArrayList<>(resultSize);
        for (int i = 0; i < resultSize; i++) {
            result.add(ranked.get(i).getKey());
        }
        return result;
    }

    private void drawPrimitive(Graphics2D g, MapView mapView, OsmPrimitive primitive) {
        if (primitive instanceof Way) {
            drawWay(g, mapView, (Way) primitive, resolveFillColor(primitive));
            return;
        }
        if (primitive instanceof Relation) {
            drawRelation(g, mapView, (Relation) primitive, resolveFillColor(primitive));
        }
    }

    private void drawSchematicAreas(Graphics2D g, MapView mapView,
            List<BuildingOverviewCollector.BuildingOverviewEntry> entries) {
        if (mapView == null || entries == null || entries.isEmpty()) {
            return;
        }

        Map<String, List<LatLon>> centersByPostcode = collectPostcodeCenters(entries);
        if (centersByPostcode.isEmpty()) {
            return;
        }

        double metersPer100Pixel = mapView.getDist100Pixel();
        if (metersPer100Pixel <= 0.0) {
            return;
        }
        double pixelsPerMeter = 100.0 / metersPer100Pixel;
        double radiusPixels = Math.max(5.0, SCHEMATIC_BUFFER_RADIUS_METERS * pixelsPerMeter);

        for (Map.Entry<String, List<LatLon>> entry : centersByPostcode.entrySet()) {
            String postcode = entry.getKey();
            List<List<LatLon>> clusters = clusterDensePoints(
                    entry.getValue(),
                    ISOLATION_DISTANCE_METERS,
                    MIN_SCHEMATIC_CLUSTER_SIZE
            );
            if (clusters.isEmpty()) {
                continue;
            }

            Color fillColor = resolveColorForPostcode(postcode);
            Color outlineColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 255);
            for (List<LatLon> cluster : clusters) {
                Area area = buildClusterArea(mapView, cluster, radiusPixels);
                if (area == null || area.isEmpty()) {
                    continue;
                }
                g.setColor(fillColor);
                g.fill(area);
                g.setColor(outlineColor);
                g.draw(area);
            }
        }
    }

    private Area buildClusterArea(MapView mapView, List<LatLon> cluster, double radiusPixels) {
        if (mapView == null || cluster == null || cluster.isEmpty() || radiusPixels <= 0.0) {
            return null;
        }
        Area merged = new Area();
        for (LatLon center : cluster) {
            if (center == null) {
                continue;
            }
            Point point = mapView.getPoint(center);
            if (point == null) {
                continue;
            }
            merged.add(new Area(new Ellipse2D.Double(
                    point.x - radiusPixels,
                    point.y - radiusPixels,
                    radiusPixels * 2.0,
                    radiusPixels * 2.0
            )));
        }
        return merged;
    }

    private Map<String, List<LatLon>> collectPostcodeCenters(List<BuildingOverviewCollector.BuildingOverviewEntry> entries) {
        Map<String, List<LatLon>> byPostcode = new HashMap<>();
        for (BuildingOverviewCollector.BuildingOverviewEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            OsmPrimitive primitive = entry.getPrimitive();
            String postcode = normalize(primitive == null ? null : primitive.get("addr:postcode"));
            if (postcode.isEmpty()) {
                continue;
            }
            LatLon center = computePrimitiveCenter(primitive);
            if (center == null) {
                continue;
            }
            byPostcode.computeIfAbsent(postcode, ignored -> new ArrayList<>()).add(center);
        }
        return byPostcode;
    }

    private LatLon computePrimitiveCenter(OsmPrimitive primitive) {
        List<LatLon> coordinates = new ArrayList<>();
        if (primitive instanceof Way) {
            collectWayCoordinates((Way) primitive, coordinates);
        } else if (primitive instanceof Relation) {
            collectRelationCoordinates((Relation) primitive, coordinates);
        }
        if (coordinates.isEmpty()) {
            return null;
        }

        double latSum = 0.0;
        double lonSum = 0.0;
        int count = 0;
        for (LatLon coordinate : coordinates) {
            if (coordinate == null) {
                continue;
            }
            latSum += coordinate.lat();
            lonSum += coordinate.lon();
            count++;
        }
        if (count == 0) {
            return null;
        }
        return new LatLon(latSum / count, lonSum / count);
    }

    private void collectRelationCoordinates(Relation relation, List<LatLon> coordinates) {
        if (relation == null || !relation.isUsable() || coordinates == null) {
            return;
        }
        for (RelationMember member : relation.getMembers()) {
            if (member == null || !member.isWay()) {
                continue;
            }
            String role = normalize(member.getRole());
            if (!role.isEmpty() && !"outer".equals(role)) {
                continue;
            }
            collectWayCoordinates(member.getWay(), coordinates);
        }
    }

    private void collectWayCoordinates(Way way, List<LatLon> coordinates) {
        if (way == null || !way.isUsable() || coordinates == null || !way.isClosed() || way.getNodesCount() < 4) {
            return;
        }
        for (Node node : way.getNodes()) {
            if (node == null || !node.isUsable() || node.getCoor() == null) {
                continue;
            }
            coordinates.add(node.getCoor());
        }
    }

    static List<List<LatLon>> clusterDensePoints(List<LatLon> points, double neighborDistanceMeters, int minClusterSize) {
        if (points == null || points.isEmpty() || neighborDistanceMeters <= 0.0 || minClusterSize <= 0) {
            return List.of();
        }

        List<LatLon> filtered = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            LatLon candidate = points.get(i);
            if (candidate == null || !hasNeighborWithin(points, i, neighborDistanceMeters)) {
                continue;
            }
            filtered.add(candidate);
        }
        if (filtered.isEmpty()) {
            return List.of();
        }

        List<List<LatLon>> clusters = new ArrayList<>();
        boolean[] visited = new boolean[filtered.size()];
        for (int start = 0; start < filtered.size(); start++) {
            if (visited[start]) {
                continue;
            }
            List<LatLon> cluster = new ArrayList<>();
            List<Integer> queue = new ArrayList<>();
            queue.add(start);
            visited[start] = true;

            for (int q = 0; q < queue.size(); q++) {
                int currentIndex = queue.get(q);
                LatLon current = filtered.get(currentIndex);
                cluster.add(current);
                for (int other = 0; other < filtered.size(); other++) {
                    if (visited[other]) {
                        continue;
                    }
                    LatLon candidate = filtered.get(other);
                    if (candidate == null || current == null) {
                        continue;
                    }
                    if (current.greatCircleDistance(candidate) <= neighborDistanceMeters) {
                        visited[other] = true;
                        queue.add(other);
                    }
                }
            }

            if (cluster.size() >= minClusterSize) {
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    private static boolean hasNeighborWithin(List<LatLon> points, int index, double maxDistanceMeters) {
        LatLon candidate = points.get(index);
        if (candidate == null) {
            return false;
        }
        for (int i = 0; i < points.size(); i++) {
            if (i == index) {
                continue;
            }
            LatLon other = points.get(i);
            if (other == null) {
                continue;
            }
            if (candidate.greatCircleDistance(other) <= maxDistanceMeters) {
                return true;
            }
        }
        return false;
    }

    private Color resolveFillColor(OsmPrimitive primitive) {
        String postcode = normalize(primitive == null ? null : primitive.get("addr:postcode"));
        return resolveColorForPostcode(postcode);
    }

    static Color resolveColorForPostcode(String postcode) {
        String normalizedPostcode = postcode == null ? "" : postcode.trim();
        if (normalizedPostcode.isEmpty()) {
            return MISSING_POSTCODE_COLOR;
        }
        long seed = Integer.toUnsignedLong(normalizedPostcode.toLowerCase(java.util.Locale.ROOT).hashCode());
        int index = (int) (seed % POSTCODE_PALETTE.length);
        Color base = POSTCODE_PALETTE[index];
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), POSTCODE_FILL_ALPHA);
    }

    private void drawRelation(Graphics2D g, MapView mapView, Relation relation, Color fillColor) {
        if (relation == null || !relation.isUsable()) {
            return;
        }

        for (RelationMember member : relation.getMembers()) {
            if (member == null || !member.isWay()) {
                continue;
            }
            String role = normalize(member.getRole());
            if (!role.isEmpty() && !"outer".equals(role)) {
                continue;
            }
            drawWay(g, mapView, member.getWay(), fillColor);
        }
    }

    private void drawWay(Graphics2D g, MapView mapView, Way way, Color fillColor) {
        Path2D polygon = buildScreenPolygon(mapView, way);
        if (polygon == null) {
            return;
        }

        g.setColor(fillColor);
        g.fill(polygon);
    }

    private Path2D buildScreenPolygon(MapView mapView, Way way) {
        if (way == null || !way.isUsable() || !way.isClosed() || way.getNodesCount() < 4) {
            return null;
        }

        Path2D path = new Path2D.Double();
        boolean hasStart = false;
        for (Node node : way.getNodes()) {
            if (node == null || !node.isUsable()) {
                return null;
            }

            Point point = mapView.getPoint(node);
            if (point == null) {
                return null;
            }

            if (!hasStart) {
                path.moveTo(point.x, point.y);
                hasStart = true;
            } else {
                path.lineTo(point.x, point.y);
            }
        }

        if (!hasStart) {
            return null;
        }
        path.closePath();
        return path;
    }

    @Override
    public Icon getIcon() {
        Icon icon = ImageProvider.get("dialogs", "search");
        return icon != null ? icon : ImageProvider.get("housenumberclick");
    }

    @Override
    public String getToolTipText() {
        return I18n.tr("Postcode overview (bright colors, same color: same postcode)");
    }

    @Override
    public void mergeFrom(Layer from) {
        // Display-only layer, no merge behavior.
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor visitor) {
        if (visitor == null || dataSet == null) {
            return;
        }
        visitor.visit(dataSet.getDataSourceBoundingBox());
    }

    @Override
    public Object getInfoComponent() {
        return I18n.tr("Postcode overview (bright colors)");
    }

    @Override
    public Action[] getMenuEntries() {
        LayerListDialog layerListDialog = LayerListDialog.getInstance();
        if (layerListDialog == null) {
            return new Action[0];
        }
        return new Action[] {
                layerListDialog.createShowHideLayerAction(),
                layerListDialog.createDeleteLayerAction()
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
