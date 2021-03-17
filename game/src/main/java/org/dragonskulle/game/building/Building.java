/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building;

import java.util.ArrayList;
import java.util.Random;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.IOnAwake;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.stat.SyncAttackDistanceStat;
import org.dragonskulle.game.building.stat.SyncAttackStat;
import org.dragonskulle.game.building.stat.SyncDefenceStat;
import org.dragonskulle.game.building.stat.SyncStat;
import org.dragonskulle.game.building.stat.SyncTokenGenerationStat;
import org.dragonskulle.game.building.stat.SyncViewDistanceStat;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncInt;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * A Building component.
 *
 * @author Craig Wilbourne
 */
@Accessors(prefix = "m")
@Log
public class Building extends NetworkableComponent implements IOnAwake {

    /** Stores the attack strength of the building. */
    @Getter private SyncAttackStat mAttack;
    /** Stores the defence strength of the building. */
    @Getter private SyncDefenceStat mDefence;
    /** Stores how many tokens the building can generate in one go. */
    @Getter private SyncTokenGenerationStat mTokenGeneration;
    /** Stores the view range of the building. */
    @Getter private SyncViewDistanceStat mViewDistance;
    /** Stores the attack range of the building. */
    @Getter private SyncAttackDistanceStat mAttackDistance;

    /** ID of the owner of the building. */
    private final SyncInt mOwnerID = new SyncInt(-1);
    /** Whether the building is a capital. */
    private final SyncBool mIsCapital = new SyncBool(false);

    /**
     * Create a new {@link Building}. This should be added to a {@link HexagonTile}. {@link
     * HexagonTile}.
     */
    public Building() {}

    @Override
    public void onAwake() {
        // Create the Stats.
        mAttack = new SyncAttackStat();
        mDefence = new SyncDefenceStat();
        mTokenGeneration = new SyncTokenGenerationStat();
        mViewDistance = new SyncViewDistanceStat();
        mAttackDistance = new SyncAttackDistanceStat();

        // For debugging, set all stat levels to 5.
        // TODO: Remove.
        mAttack.setLevel(5);
        mDefence.setLevel(5);
        mTokenGeneration.setLevel(5);
        mViewDistance.setLevel(5);
        mAttackDistance.setLevel(5);
    }

    /**
     * Attack an opponent building.
     *
     * <p><b> Currently has no effect other than the basic calculations (i.e. it does not transfer
     * ownership of Buildings). <b>
     *
     * <p>There is a chance this will either fail or succeed, influenced by the attack stat of the
     * attacking building and the defence stats of the opponent building.
     *
     * @param opponent The building to attack.
     * @return Whether the attack was successful or not.
     */
    public boolean attack(Building opponent) {
        // TODO: Add Lelaa's code here.

        Random random = new Random();
        double successChance = random.nextDouble();
        // Set a 50% chance of success.
        double target = 0.5;

        if (successChance >= target) {
            log.info(
                    String.format(
                            "Successful attack: random number %f was greater or equal to target %f.",
                            successChance, target));

            // Claim the opponent building.
            // TODO: Allow the Players to update their lists of buildings they own.
            return true;
        }

        log.info(
                String.format(
                        "Failed attack: random number %f was not greater or equal to target %f.",
                        successChance, target));

        return false;
    }

    /**
     * Get an ArrayList of all {@link HexagonTile}s, excluding the building's HexagonTile, within a
     * set radius.
     *
     * @param radius The radius.
     * @return An ArrayList of HexgaonTiles, otherwise an empty ArrayList.
     */
    private ArrayList<HexagonTile> getTilesInRadius(int radius) {
        ArrayList<HexagonTile> tiles = new ArrayList<HexagonTile>();

        // Get the map.
        HexagonMap map = getMap();
        // Get the current position.
        Vector3i position = getPosition();

        // Get the current q and r coordinates.
        int qCentre = position.x();
        int rCentre = position.y();

        for (int rOffset = -radius; rOffset <= radius; rOffset++) {
            for (int qOffset = -radius; qOffset <= radius; qOffset++) {
                // Only get tiles whose s coordinates are within the desired range.
                int sOffset = -qOffset - rOffset;

                // Do not include tiles outside of the radius.
                if (sOffset > radius || sOffset < -radius) continue;
                // Do not include the building's HexagonTile.
                if (qOffset == 0 && rOffset == 0) continue;

                // log.info(String.format("qOffset = %d, rOffset = %d, s = %d ", qOffset, rOffset,
                // s));

                // Attempt to get the desired tile, and check if it exists.
                HexagonTile selectedTile = map.getTile(qCentre + qOffset, rCentre + rOffset);
                if (selectedTile == null) continue;

                // Add the tile to the list.
                tiles.add(selectedTile);
            }
        }

        // log.info("Number of tiles in range: " + tiles.size());

        return tiles;
    }

    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's view range, as
     * specified by {@link #mViewDistance}.
     *
     * @return All the HexagonTiles within the building's view range, excluding the Building's
     *     HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getViewableTiles() {
        // Get the current view distance.
        int distance = mViewDistance.getValue();
        // Get the tiles within the view distance.
        return getTilesInRadius(distance);
    }

    /**
     * Get an ArrayList of {@link HexagonTile}s that are within the Building's attack range, as
     * specified by {@link #mAttackDistance}.
     *
     * @return All the HexagonTiles within the building's attack range, excluding the Building's
     *     HexagonTile, otherwise an empty ArrayList.
     */
    public ArrayList<HexagonTile> getAttackableTiles() {
        // Get the current view distance.
        int distance = mAttackDistance.getValue();
        // Get the tiles within the view distance.
        return getTilesInRadius(distance);
    }

    /**
     * Get an ArrayList of opponent {@link Building}s within the range defined by {@link
     * #mAttackDistance}.
     *
     * @return An ArrayList of opponent Buildings that can be attacked.
     */
    public ArrayList<Building> getAttackableBuildings() {
        ArrayList<Building> buildings = new ArrayList<Building>();

        // Get the map.
        HexagonMap map = getMap();

        // Get all the tiles in attackable distance.
        ArrayList<HexagonTile> attackTiles = getAttackableTiles();
        for (HexagonTile tile : attackTiles) {
            // Get the building on an attackable tile, if it exists.
            Building building = map.getBuilding(tile.getQ(), tile.getR());
            if (building == null) continue;

            // Ensure the building is not owned by the owner of this building.
            if (getOwnerID() == building.getOwnerID()) {
                log.info("Building owned by same player.");
                continue;
            }

            // Add the opponent building to the list of attackable buildings.
            buildings.add(building);
        }

        return buildings;
    }

    /**
     * Get the current position of the building.
     *
     * @return A 3d-vector of integers containing the x, y and z position of the building.
     */
    private Vector3i getPosition() {
        Vector3f floatPosition = new Vector3f();
        TransformHex tranform = getGameObject().getComponent(TransformHex.class).get();
        tranform.getLocalPosition(floatPosition);

        Vector3i position = new Vector3i();
        position.set((int) floatPosition.x(), (int) floatPosition.y(), (int) floatPosition.z());

        return position;
    }

    /**
     * Get the {@link HexagonMap} being used.
     *
     * @return The map.
     */
    private HexagonMap getMap() {
        return Scene.getActiveScene()
                .getSingleton(HexagonMap.class)
                .getReference(HexagonMap.class)
                .get();
    }

    /**
     * Get an ArrayList of Stats that the Building has.
     *
     * @return An ArrayList of Stats.
     */
    public ArrayList<SyncStat<?>> getStats() {
        ArrayList<SyncStat<?>> stats = new ArrayList<SyncStat<?>>();
        stats.add(mAttack);
        stats.add(mDefence);
        stats.add(mTokenGeneration);
        stats.add(mViewDistance);
        stats.add(mAttackDistance);

        return stats;
    }

    /**
     * Store the owner's ID.
     *
     * @param id The ID of the owner.
     */
    public void setOwnerID(int id) {
        mOwnerID.set(id);
    }

    /**
     * Get the ID of the owner of the building.
     *
     * @return The ID of the owner.
     */
    public int getOwnerID() {
        return mOwnerID.get();
    }

    /**
     * Set the owner of the building.
     *
     * @param player The owner.
     */
    public void setOwner(TestPlayer player) {
        setOwnerID(player.getID());
    }

    /**
     * Get whether the building is a capital.
     *
     * @return Whether the building is a capital.
     */
    public boolean isCapital() {
        return mIsCapital.get();
    }

    /**
     * Set the building to be a capital.
     *
     * <p>By default, buildings are not capitals.
     *
     * @param isCapital Whether the building should be capital.
     */
    public void setCapital(boolean isCapital) {
        mIsCapital.set(isCapital);
    }

    @Override
    protected void onDestroy() {}
}
