/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ai;

import java.util.Random;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.player.Player;
import org.dragonskulle.network.components.NetworkManager;

/**
 * This {@code abstract} class contains all the needed methods and variables which are needed by all
 * AI Player to actually play.
 *
 * @author DragonSkulle
 */
@Log
public abstract class AiPlayer extends Component implements IFixedUpdate, IOnStart {

    /** The time since the last check if the AI player can play. (Start at 0). */
    protected float mTimeSinceStart;
    /** The lower bound for the random number to choose a time. */
    protected int mLowerBoundTime = 1;
    /** The upper bound for the random number to choose a time. */
    protected int mUpperBoundTime = 2;
    /** Will hold how long the AI player has to wait until playing. */
    protected int mTimeToWait;

    /** This is whether the AiPlayer is being ran on the server. */
    private boolean mServerSide = false;

    /** The Random Number Generator. */
    protected Random mRandom = new Random();

    /** This is the {@link Reference} to the {@link Player} which is used by this AI Player. */
    protected Reference<Player> mPlayer;

    /** Basic Constructor. */
    public AiPlayer() {}

    @Override
    public void onStart() {

        NetworkManager manager = Scene.getActiveScene().getSingleton(NetworkManager.class);
        if (manager != null && manager.isServer()) {
            mServerSide = true;
        }

        // Sets up all unitialised variables
        mPlayer = getGameObject().getComponent(Player.class);
        mTimeSinceStart = 0;
        createNewRandomTime();
    }

    /**
     * This will check to see whether the AI Player can actually play or not.
     *
     * @param deltaTime The time since the last fixed update
     * @return A boolean to say whether the AI player can play
     */
    protected boolean shouldPlayGame(float deltaTime) {
        mTimeSinceStart += deltaTime;

        // Checks to see how long since last time AI player played and if longer than how long they
        // have to wait.
        if (mTimeSinceStart >= mTimeToWait) {
            mTimeSinceStart = 0;
            createNewRandomTime(); // Creates new Random Number until next move
            return true;
        }

        return false;
    }

    /** This will set how long the AI player has to wait until they can play. */
    protected void createNewRandomTime() {
        mTimeToWait = mRandom.nextInt() % (mUpperBoundTime + 1 - mLowerBoundTime) + mLowerBoundTime;
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void fixedUpdate(float deltaTime) {
        // If you can play simulate the input
        if (shouldPlayGame(deltaTime)
                && mServerSide
                && getPlayer() != null
                && !getPlayer().gameEnd()
                && getPlayer().getNumberOfOwnedBuildings() != 0) {
            log.info("Playing game");
            simulateInput();
        }
    }

    /**
     * This will simulate the action to be done by the AI player. This will only be called when we
     * have not lost. For the base class this will be done using probability
     */
    protected abstract void simulateInput();

    /**
     * Gets the player.
     *
     * @return The player.
     */
    protected Player getPlayer() {
        Player player = mPlayer.get();
        if (player == null) {
            log.severe("Reference to mPlayer is null!");
        }
        return player;
    }
}
