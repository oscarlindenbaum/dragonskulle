/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai.algorithms.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.extern.java.Log;
import org.dragonskulle.core.Reference;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.game.player.ai.AimerAi;

/**
 * Will implement a directed Graph data structure. This has been adapted from Nathaniel Lowis's (one
 * of our group members) repository: https://github.com/low101043/aiProjectComputer
 *
 * @author Dragonskulle
 */
@Log
public class Graph {

    /** The hash map which will hold the node id to the Node. */
    protected HashMap<Integer, Node> mGraph;

    /** The {@code HexagonMap} which is used. */
    private Reference<HexagonMap> mMap;

    /**
     * Constructor to create the whole map.
     *
     * @param map The {@code HexagonMap} to convert to a {@code Graph}.
     * @param target The {@code HexagonTile} to aim for.
     */
    public Graph(HexagonMap map, HexagonTile target) {

        initialise(map, target);

        mGraph = new HashMap<Integer, Node>();
        map.getAllTiles().forEach((tile) -> convertToNode(tile, target));
        map.getAllTiles().forEach(this::addConnections);
    }

    /**
     * Constructor to create only part of the map.
     *
     * @param map The {@code HexagonMap} to convert to a {@code Graph}.
     * @param target The {@code HexagonTile} to aim for.
     * @param aimer The {@code AimerAi} to use to create the map.
     */
    public Graph(HexagonMap map, HexagonTile target, AimerAi aimer) {

        initialise(map, target);

        aimer.getViewableTiles().forEach((tile) -> convertToNode(tile, target));
        aimer.getViewableTiles().forEach(this::addConnections);
    }

    /**
     * This is the variables set up in both constructors.
     *
     * @param map The {@code HexagonMap} to be used.
     * @param tileAiming The {@code HexagonTile} being aimed at.
     */
    private void initialise(HexagonMap map, HexagonTile tileAiming) {

        mMap = map.getReference(HexagonMap.class);
        mGraph = new HashMap<Integer, Node>();
    }

    /**
     * This will convert a {@code HexagonTile} to a {@code Node} which can be used by a {@code
     * Graph}.
     *
     * @param tile The {@code HexagonTile} to be made a node.
     * @param target The {@code HexagonTile} to aim for
     */
    private void convertToNode(HexagonTile tile, HexagonTile target) {

        if (tile.getTileType() != TileType.LAND) {
            return;
        }

        int nodeNum = getHash(tile);
        // log.info("Node num: " + nodeNum);
        log.severe("Node number: " + nodeNum + " Q: " + tile.getQ() + " R: " + tile.getR());

        addNode(nodeNum, tile);
        int heuristic = tile.distTo(target.getQ(), target.getR());
        setNodeHeuristic(nodeNum, heuristic);
    }

    /**
     * This will add all the connections for a node.
     *
     * @param tile The {@code HexagonTile} to add connections for in the graph.
     */
    private void addConnections(HexagonTile tile) {
        if (!Reference.isValid(mMap)) {
            return;
        }

        if (tile.getTileType() != TileType.LAND) {
            return;
        }

        int nodeNum = getHash(tile);
        log.info("Node num: " + nodeNum);
        ArrayList<HexagonTile> neighbourTilesList = new ArrayList<HexagonTile>();

        List<HexagonTile> neighbourTiles =
                mMap.get().getTilesInRadius(tile, 1, false, neighbourTilesList);

        for (HexagonTile tileNeighbour : neighbourTiles) {

            // TODO Check if in viewable tiles!
            if (tileNeighbour.getTileType() != TileType.LAND) {
                continue;
            }
            Node node = getNode(tileNeighbour);
            if (node == null) {
                // Assuming I cannot see the tile
                continue;
            }
            if (tileNeighbour.getQ() == node.getHexTile().get().getQ()
                    && tileNeighbour.getR() == node.getHexTile().get().getR()) {

                addConnection(nodeNum, node.getNodeId(), 1); // Weight set to 1
            }
        }
    }

    /**
     * This will add a node to a mGraph with no connections.
     *
     * @param nodeId The node number.
     * @param tile The {@code HexagonTile} it corresponds to.
     */
    public void addNode(int nodeId, HexagonTile tile) {

        Node newNode = new Node(nodeId, tile); // Makes a new node
        mGraph.put(nodeId, newNode); // Adds to mGraph
    }

    /**
     * Adds a connection between two nodes.
     *
     * @param originNode The origin node.
     * @param destinationNode the end nod.e
     * @param weight The weight between the nodes.
     */
    public void addConnection(int originNode, int destinationNode, int weight) {

        // Gets the connection if in the graph -- assumption is that all are added by now.
        Node node = mGraph.get(originNode);

        node.addConnection(destinationNode, weight); // Adds the connection to the node
    }

    /**
     * Returns the connections of that node.
     *
     * @param nodeNum The node to find.
     * @return The connections.
     */
    public ArrayList<Connection> getConnection(int nodeNum) {

        Node node = mGraph.get(nodeNum);
        if (node == null) {
            return new ArrayList<Connection>();
        } else {
            return mGraph.get(nodeNum).getConnections();
        }
    }

    /**
     * Returns the heuristic for that node.
     *
     * @param nodeToGet The node which has the special info to get.
     * @return the heuristic for that node or {@code Integer.MAX_VALUE} if the Node does not exist.
     */
    public int getNodeHeuristic(int nodeToGet) {

        if (mGraph.get(nodeToGet) == null) {
            return Integer.MAX_VALUE;
        }
        return mGraph.get(nodeToGet).getHeuristic();
    }

    /**
     * A setter which sets the heuristic info.
     *
     * @param nodeToChange The node to change.
     * @param newInfo The extra info to change.
     */
    public void setNodeHeuristic(int nodeToChange, int newInfo) {

        Node node = mGraph.get(nodeToChange);

        node.setHeuristic(newInfo);
        mGraph.replace(nodeToChange, node);
    }

    /**
     * Returns a node.
     *
     * @param node The nodeID to find.
     * @return The actual {@code Node}.
     */
    public Node getNode(Integer node) {
        return mGraph.get(node);
    }

    /**
     * Will return a node when given a {@code HexagonTile}.
     *
     * @param tile The {@code HexagonTile} to find a {@code Node} for.
     * @return The {@code Node} which corresponds to the {@code HexagonTile} or {@code null}.
     */
    public Node getNode(HexagonTile tile) {
        int targetNodeHash = (tile.getQ() * mMap.get().getSize()) + tile.getR();
        return getNode(targetNodeHash);
    }

    /**
     * Gets the hash of a {@code HexagonTile}
     *
     * @param tile The {@code HexagonTile} to get a hash for
     * @return The hash of it
     */
    public int getHash(HexagonTile tile) {
        int nodeNum = (tile.getQ() * mMap.get().getSize()) + tile.getR();
        return nodeNum;
    }
}