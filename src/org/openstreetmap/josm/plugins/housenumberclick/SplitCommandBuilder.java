package org.openstreetmap.josm.plugins.housenumberclick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.command.SplitWayCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Builds command sequences for split operations, including node preparation and tag preservation.
 */
final class SplitCommandBuilder {

    PreparedNodeCommands prepareNodes(DataSet dataSet, Way way, List<IntersectionPoint> intersections) {
        List<Command> commands = new ArrayList<>();
        List<Node> splitNodes = new ArrayList<>();
        boolean changedWay = false;

        Way updatedWay = new Way(way);
        List<Node> updatedNodes = new ArrayList<>(updatedWay.getNodes());

        List<IntersectionPoint> ordered = new ArrayList<>(intersections);
        ordered.sort(Comparator.comparingInt(IntersectionPoint::getSegmentIndex).reversed());

        for (IntersectionPoint intersection : ordered) {
            if (intersection.isExistingNode() && intersection.getExistingNode() != null) {
                Node existingNode = intersection.getExistingNode();
                if (!splitNodes.contains(existingNode)) {
                    splitNodes.add(existingNode);
                }
                continue;
            }

            Node newNode = new Node(intersection.getCoordinate());
            commands.add(new AddCommand(dataSet, newNode));
            int insertIndex = calculateInsertIndex(updatedNodes, intersection.getSegmentIndex());
            updatedNodes.add(insertIndex, newNode);
            changedWay = true;
            if (!splitNodes.contains(newNode)) {
                splitNodes.add(newNode);
            }
        }

        if (changedWay) {
            updatedWay.setNodes(updatedNodes);
            commands.add(new ChangeCommand(dataSet, way, updatedWay));
        }

        splitNodes.sort(Comparator.comparingInt(updatedNodes::indexOf));
        return new PreparedNodeCommands(splitNodes, updatedNodes, commands);
    }

    Optional<SplitWayCommand> createSplitWayCommand(Way sourceWay, List<List<Node>> splitChunks, List<OsmPrimitive> splitSelection) {
        return SplitWayCommand.splitWay(
                sourceWay,
                splitChunks,
                splitSelection,
                SplitWayCommand.Strategy.keepFirstChunk(),
                SplitWayCommand.WhenRelationOrderUncertain.SPLIT_ANYWAY
        );
    }

    SequenceCommand buildSequenceCommand(String name, List<Command> commands) {
        return new SequenceCommand(name, commands);
    }


    private int calculateInsertIndex(List<Node> closedWayNodes, int segmentIndex) {
        int min = 1;
        int max = closedWayNodes.size() - 1;
        int insertIndex = segmentIndex + 1;
        if (insertIndex < min) {
            return min;
        }
        if (insertIndex > max) {
            return max;
        }
        return insertIndex;
    }

    /**
     * Bundle returned from node preparation containing new split nodes, updated ring, and commands.
     */
    static final class PreparedNodeCommands {
        private final List<Node> splitNodes;
        private final List<Node> updatedWayNodes;
        private final List<Command> commands;

        private PreparedNodeCommands(List<Node> splitNodes, List<Node> updatedWayNodes, List<Command> commands) {
            this.splitNodes = splitNodes;
            this.updatedWayNodes = updatedWayNodes;
            this.commands = commands;
        }

        List<Node> getSplitNodes() {
            return splitNodes;
        }

        List<Node> getUpdatedWayNodes() {
            return updatedWayNodes;
        }

        List<Command> getCommands() {
            return commands;
        }
    }
}
