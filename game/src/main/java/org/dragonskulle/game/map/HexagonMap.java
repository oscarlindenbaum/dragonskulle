/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.map.HexagonTile.TileType;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.renderer.components.Camera;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

/**
 * @author Leela Muppala and Craig Wilbourne
 *     <p>This class generates and stores a map of tiles with appropriate coordinates. Hexagon map
 *     objects are also created and stored.
 */
@Accessors(prefix = "m")
@Log
public class HexagonMap extends NetworkableComponent implements IOnAwake {

    public static interface IFloodFillVisitor {
        void onVisit(
                HexagonMap map,
                HexagonTile tile,
                List<HexagonTile> neighbours,
                Deque<HexagonTile> tilesToFill);
    }

    /** The size that is used to create the map. */
    @Getter @Setter private int mSize = 51;

    /** The map that is created which is made of a 2d array of HexagonTiles. */
    private HexagonTileStore mTiles;

    /** This will store what the largest landMass is */
    private int[] mLargestLandMass;

    /** This will store what the next land mass number is */
    private int mLandMass = 0;

    /** This will go through all the tiles and find all islands */
    private void checkIslands() {

        mLargestLandMass = new int[2];
        mLargestLandMass[0] = -1;
        mLargestLandMass[1] = -1;
        getAllTiles()
                .forEach(
                        tile -> {
                            if (tile.landMassNumber == -1) {
                                floodFillLand(tile);
                            }
                        });
    }

    /**
     * This will use flood fill to find all connected tiles on land from the given {@code
     * HexagonTile}
     *
     * @param tile The tile to start flooding from
     */
    private void floodFillLand(HexagonTile tile) {
        // Checks that we haven't already checked it
        int size[] = {0};
        if (tile.getTileType() != TileType.LAND || tile.landMassNumber != -1) {
            return;
        }

        Deque<HexagonTile> tiles = new ArrayDeque<HexagonTile>();
        tiles.add(tile);

        floodFill(
                tiles,
                (__, tileToUse, neighbours, tilesOut) -> {
                    if (tileToUse.getTileType() == TileType.LAND
                            && tileToUse.landMassNumber == -1) {
                        size[0]++;
                        tileToUse.landMassNumber = mLandMass;

                        for (HexagonTile neighbour : neighbours) {
                            if (neighbour.landMassNumber == -1
                                    && neighbour.getTileType() == TileType.LAND) {
                                tilesOut.add(neighbour);
                            }
                        }
                    }
                });

        if (size[0] > mLargestLandMass[1]) {
            mLargestLandMass[0] = mLandMass;
            mLargestLandMass[1] = size[0];
        }

        mLandMass++;
    }

    public void floodFill(Deque<HexagonTile> tiles, IFloodFillVisitor visitor) {
        ArrayList<HexagonTile> neighbours = new ArrayList<>();

        while (tiles.size() != 0) {
            HexagonTile tileToUse = tiles.removeFirst();
            getTilesInRadius(tileToUse, 1, false, neighbours);
            visitor.onVisit(this, tileToUse, neighbours, tiles);
        }
    }

    /**
     * Get the {@link HexagonTile} at the specified position, or {@code null} if it doesn't exist.
     *
     * @param q The q coordinate.
     * @param r The r coordinate.
     * @return The HexagonTile, or {@code null}.
     */
    public HexagonTile getTile(int q, int r) {
        return mTiles.getTile(q, r);
    }

    /**
     * Get a stream of all hexagon tiles.
     *
     * @return stream of all non-null hexagon tiles in the map
     */
    public Stream<HexagonTile> getAllTiles() {
        return mTiles.getAllTiles();
    }

    /**
     * Get all of the {@link HexagonTile}s in a radius around the selected tile. If {@code
     * includeTile} is {@code true}, the selected tile will be included in the list.
     *
     * @param tile The selected tile.
     * @param radius The radius around the selected tile.
     * @param includeTile Whether or not to include the selected tile in the resultant {@link
     *     ArrayList}.
     * @return A list of tiles within a radius of the selected tile, otherwise an empty ArrayList.
     */
    public List<HexagonTile> getTilesInRadius(
            HexagonTile tile, int radius, boolean includeTile, ArrayList<HexagonTile> tilesOut) {
        int minimum = includeTile ? 0 : 1;
        return getTilesInRadius(tile, minimum, radius, tilesOut);
    }

    /**
     * Get the tiles within a minimum and maximum radius of the target tile.
     *
     * <p>A min radius of 0 will include the target tile. <br>
     * A min radius of 1 will exclude the target tile.
     *
     * @param tile The target tile.
     * @param min The minimum radius of tiles to include.
     * @param max The maximum radius of tiles to include.
     * @param tilesOut A {@link List} where {@link HexagonTile}s in radius will be filled into.
     * @return A {@link List} of {@link HexagonTile}s within the min and max radius. inclusive.
     */
    public List<HexagonTile> getTilesInRadius(
            HexagonTile tile, int min, int max, List<HexagonTile> tilesOut) {
        if (tile == null) return new ArrayList<HexagonTile>();

        return getTilesInRadius(tile.getQ(), tile.getR(), min, max, tilesOut);
    }

    /**
     * Get the tiles within a minimum and maximum radius of the target tile position.
     *
     * <p>A min radius of 0 will include the target tile. <br>
     * A min radius of 1 will exclude the target tile.
     *
     * <p>Based off pseudocode found here: https://www.redblobgames.com/grids/hexagons/#range
     *
     * @param tileQ The target tile Q position.
     * @param tileR The target tile R position.
     * @param min The minimum radius of tiles to include.
     * @param max The maximum radius of tiles to include.
     * @param tilesOut A {@link List} where {@link HexagonTile}s in radius will be filled into.
     * @return An {@link List} of {@link HexagonTile}s within the min and max radius, inclusive.
     */
    private List<HexagonTile> getTilesInRadius(
            int tileQ, int tileR, int min, int max, List<HexagonTile> tilesOut) {
        tilesOut.clear();
        for (int q = -max; q <= max; q++) {
            // Only generate valid tile coordinates.
            int lower = Math.max(-max, -q - max);
            int upper = Math.min(max, -q + max);
            for (int r = lower; r <= upper; r++) {
                int s = -q - r;

                // Ensure tile isn't within the minimum.
                int distance = getDistance(q, r, s);
                if (distance < min) {
                    continue;
                }

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = getTile(tileQ + q, tileR + r);
                if (selectedTile == null) {
                    continue;
                }

                // Add the tile to the list.
                tilesOut.add(selectedTile);
            }
        }

        return tilesOut;
    }

    /**
     * Calculate the distance from the centre (0, 0, 0).
     *
     * @param q the q coordinate
     * @param r the r coordinate
     * @param s the s coordinate
     * @return distance
     */
    private int getDistance(int q, int r, int s) {
        return Math.max(Math.max(Math.abs(q), Math.abs(r)), Math.abs(s));
    }

    public HexagonTile cursorToTile() {
        Camera mainCam = Scene.getActiveScene().getSingleton(Camera.class);

        if (mainCam == null) {
            return null;
        }

        // Retrieve scaled screen coordinates
        Cursor cursor = Actions.getCursor();

        if (cursor == null) {
            return null;
        }

        Vector2fc screenPos = cursor.getPosition();

        // Convert those coordinates to local coordinates within the map
        Vector3f pos =
                mainCam.screenToPlane(
                        getGameObject().getTransform(),
                        0,
                        screenPos.x(),
                        screenPos.y(),
                        new Vector3f());

        Vector2f axial = new Vector2f();

        // Convert those coordinates to axial
        TransformHex.cartesianToAxial(pos, axial);
        // And round them
        TransformHex.roundAxial(axial, pos);

        HexagonTile closestTile = null;
        float closestDistance = 1e30f;

        List<HexagonTile> tiles =
                getTilesInRadius((int) axial.x, (int) axial.y, 0, 4, new ArrayList<>());

        Vector3f va = new Vector3f();
        Vector3f vb = new Vector3f();
        Vector3f camPos = mainCam.getGameObject().getTransform().getPosition();

        tiles.sort(
                (a, b) ->
                        Float.compare(
                                a.getGameObject()
                                        .getTransform()
                                        .getPosition(va)
                                        .distanceSquared(camPos),
                                b.getGameObject()
                                        .getTransform()
                                        .getPosition(vb)
                                        .distanceSquared(camPos)));

        for (HexagonTile tile : tiles) {
            float dist = cursorDistanceFromCenter(tile, mainCam, screenPos, pos);
            if (dist < closestDistance) {
                closestDistance = dist;
                closestTile = tile;

                if (dist <= TransformHex.HEX_SIZE) {
                    return closestTile;
                }
            }
        }

        return closestTile;
    }

    private float cursorDistanceFromCenter(
            HexagonTile tile, Camera cam, Vector2fc screenPos, Vector3f pos) {
        pos =
                cam.screenToPlane(
                        tile.getGameObject().getTransform(), 0, screenPos.x(), screenPos.y(), pos);

        Vector2f axial = new Vector2f();

        // Convert those coordinates to axial
        TransformHex.cartesianToAxial(pos, axial);

        return (Math.abs(axial.x) + Math.abs(axial.y) + Math.abs(-axial.x - axial.y));
    }

    /**
     * This checks if the given {@code HexagonTile} is an island (An island is defined as a land
     * mass which is disconnected completely from the largest land mass)
     *
     * @param tile The {@code HexagonTile} to check if its in an island
     * @return Returns {@code true} if it is an island, {@code false} if not
     */
    public boolean isIsland(HexagonTile tile) {
        if (mLargestLandMass == null) {
            log.severe("ERROR");
        }

        return tile.landMassNumber != mLargestLandMass[0];
    }

    @Override
    protected void onNetworkInitialize() {
        Random rand = new Random();
        mTiles = new HexagonTileStore(mSize, rand.nextInt(), this);
        checkIslands();
    }

    @Override
    public void onDestroy() {}

    @Override
    public void onAwake() {

        if (mSize <= 0) {
            log.severe("Map size must be greater than 0!");
            getGameObject().destroy();
            return;
        }

        Scene.getActiveScene().registerSingleton(this);
    }

    void updateTileGameObject(HexagonTile tile) {
        getGameObject().addChild(tile.getGameObject());
    }
}
