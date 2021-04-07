/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.building.stat;

import org.dragonskulle.game.building.Building;

/**
 * Contains the value used to determine the range of view.
 *
 * @author Craig Wilbourne
 */
public class SyncViewDistanceStat extends SyncStat {

    public SyncViewDistanceStat(Building building) {
        super(building);
    }

    @Override
    public int getValue() {
        return 3;
    }
}
