package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

final class TerraceSplitService {

    private static final double LINE_MARGIN_FALLBACK = 1e-5;

    private final SingleBuildingSplitService singleBuildingSplitService;
    private final CornerSnapService cornerSnapService;

    TerraceSplitService() {
        this(new SingleBuildingSplitService(), new CornerSnapService());
    }

    TerraceSplitService(SingleBuildingSplitService singleBuildingSplitService, CornerSnapService cornerSnapService) {
        this.singleBuildingSplitService = singleBuildingSplitService;
        this.cornerSnapService = cornerSnapService;
    }

    TerraceSplitResult splitBuildingIntoTerrace(
            DataSet dataSet,
            Way buildingWay,
            TerraceSplitRequest request,
            SplitContext context
    ) {
        if (dataSet == null) {
            return TerraceSplitResult.failure("No editable dataset is available.");
        }
        if (buildingWay == null) {
            return TerraceSplitResult.failure("No building way selected.");
        }
        if (buildingWay.getDataSet() != dataSet) {
            return TerraceSplitResult.failure("Selected building is not part of the current dataset.");
        }
        if (!buildingWay.isClosed()) {
            return TerraceSplitResult.failure("The selected way must be closed.");
        }
        if (!buildingWay.hasKey("building")) {
            return TerraceSplitResult.failure("The selected way must have a building=* tag.");
        }
        if (request == null || !request.hasValidParts()) {
            return TerraceSplitResult.failure("Terrace split requires parts >= 2.");
        }

        Bounds bounds = computeBounds(buildingWay);
        if (!bounds.isValid()) {
            return TerraceSplitResult.failure("Building geometry is not suitable for terrace split.");
        }

        boolean splitAlongLongitude = bounds.width() >= bounds.height();
        double margin = Math.max(Math.max(bounds.width(), bounds.height()) * 0.25, LINE_MARGIN_FALLBACK);

        List<Way> pieces = new ArrayList<>();
        pieces.add(buildingWay);

        SplitContext splitContext = context == null ? SplitContext.empty() : context;

        for (int partIndex = 1; partIndex < request.getParts(); partIndex++) {
            double ratio = partIndex / (double) request.getParts();
            LatLon lineStart;
            LatLon lineEnd;
            if (splitAlongLongitude) {
                double lon = bounds.minLon + (bounds.width() * ratio);
                lineStart = new LatLon(bounds.minLat - margin, lon);
                lineEnd = new LatLon(bounds.maxLat + margin, lon);
            } else {
                double lat = bounds.minLat + (bounds.height() * ratio);
                lineStart = new LatLon(lat, bounds.minLon - margin);
                lineEnd = new LatLon(lat, bounds.maxLon + margin);
            }

            Way splitTarget = findSingleSplitTarget(pieces, lineStart, lineEnd);
            if (splitTarget == null) {
                return TerraceSplitResult.failure("Building geometry is not suitable for equal terrace splitting.");
            }

            SingleSplitResult splitResult = singleBuildingSplitService.splitBuilding(
                    dataSet,
                    splitTarget,
                    lineStart,
                    lineEnd,
                    splitContext
            );
            if (!splitResult.isSuccess() || splitResult.getResultWays().size() != 2) {
                return TerraceSplitResult.failure("Terrace split failed: " + splitResult.getMessage());
            }

            pieces.remove(splitTarget);
            pieces.addAll(splitResult.getResultWays());
        }

        if (pieces.size() != request.getParts()) {
            return TerraceSplitResult.failure("Terrace split produced an unexpected number of result ways.");
        }

        pieces.sort(buildDeterministicOrder(splitAlongLongitude));
        return TerraceSplitResult.success("Terrace split completed.", pieces);
    }

    private Way findSingleSplitTarget(List<Way> pieces, LatLon lineStart, LatLon lineEnd) {
        Way candidate = null;
        for (Way way : pieces) {
            if (way == null || !way.isUsable() || !way.isClosed()) {
                continue;
            }
            IntersectionScanResult scan = cornerSnapService.findSplitIntersections(way, lineStart, lineEnd);
            if (!scan.isSuccess() || scan.getIntersections().size() != 2) {
                continue;
            }
            if (candidate != null) {
                return null;
            }
            candidate = way;
        }
        return candidate;
    }

    private Comparator<Way> buildDeterministicOrder(boolean primaryLongitude) {
        return Comparator
                .comparingDouble((Way way) -> primaryLongitude ? centroidLon(way) : centroidLat(way))
                .thenComparingDouble(way -> primaryLongitude ? centroidLat(way) : centroidLon(way))
                .thenComparingInt(Way::getNodesCount);
    }

    private double centroidLat(Way way) {
        List<Node> ring = getOpenRingNodes(way);
        if (ring.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0.0;
        int count = 0;
        for (Node node : ring) {
            if (node == null || node.getCoor() == null) {
                continue;
            }
            sum += node.getCoor().lat();
            count++;
        }
        return count == 0 ? Double.POSITIVE_INFINITY : sum / count;
    }

    private double centroidLon(Way way) {
        List<Node> ring = getOpenRingNodes(way);
        if (ring.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0.0;
        int count = 0;
        for (Node node : ring) {
            if (node == null || node.getCoor() == null) {
                continue;
            }
            sum += node.getCoor().lon();
            count++;
        }
        return count == 0 ? Double.POSITIVE_INFINITY : sum / count;
    }

    private List<Node> getOpenRingNodes(Way way) {
        List<Node> ring = new ArrayList<>(way.getNodes());
        if (ring.size() > 1 && ring.get(0).equals(ring.get(ring.size() - 1))) {
            ring.remove(ring.size() - 1);
        }
        return ring;
    }

    private Bounds computeBounds(Way way) {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;

        for (Node node : getOpenRingNodes(way)) {
            if (node == null || node.getCoor() == null) {
                continue;
            }
            LatLon coor = node.getCoor();
            minLat = Math.min(minLat, coor.lat());
            maxLat = Math.max(maxLat, coor.lat());
            minLon = Math.min(minLon, coor.lon());
            maxLon = Math.max(maxLon, coor.lon());
        }
        return new Bounds(minLat, maxLat, minLon, maxLon);
    }

    private static final class Bounds {
        private final double minLat;
        private final double maxLat;
        private final double minLon;
        private final double maxLon;

        private Bounds(double minLat, double maxLat, double minLon, double maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }

        private boolean isValid() {
            return Double.isFinite(minLat)
                    && Double.isFinite(maxLat)
                    && Double.isFinite(minLon)
                    && Double.isFinite(maxLon)
                    && maxLat > minLat
                    && maxLon > minLon;
        }

        private double width() {
            return maxLon - minLon;
        }

        private double height() {
            return maxLat - minLat;
        }
    }
}

