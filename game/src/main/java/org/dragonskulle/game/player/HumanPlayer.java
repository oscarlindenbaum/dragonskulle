/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.IFrameUpdate;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.game.building.Building;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.input.GameActions;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.map.MapEffects.StandardHighlightType;
import org.dragonskulle.game.player.ui.Screen;
import org.dragonskulle.game.player.ui.UILinkedScrollBar;
import org.dragonskulle.game.player.ui.UIMenuLeftDrawer;
import org.dragonskulle.game.player.ui.UITokenCounter;
import org.dragonskulle.input.Actions;
import org.dragonskulle.input.Cursor;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIManager;
import org.joml.Vector3f;

/**
 * This class will allow a user to interact with game.
 *
 * @author DragonSkulle
 */
@Accessors(prefix = "m")
@Log
public class HumanPlayer extends Component implements IFrameUpdate, IFixedUpdate, IOnStart {

    // All screens to be used
    private Screen mScreenOn = Screen.DEFAULT_SCREEN;

    private Reference<UIMenuLeftDrawer> mMenuDrawer;

    // Data which is needed on different screens
    @Getter @Setter private HexagonTile mHexChosen;

    @Getter @Setter private Reference<Building> mBuildingChosen = new Reference<>(null);

    // The player
    private Reference<Player> mPlayer;
    private int mLocalTokens = 0;

    private final int mNetId;
    private final Reference<NetworkManager> mNetworkManager;

    // Visual effects
    private Reference<MapEffects> mMapEffects;
    private boolean mVisualsNeedUpdate;
    private Reference<UITokenCounter> mTokenCounter;
    private Reference<GameObject> mTokenCounterObject;
    private HexagonTile mLastHexChosen;

    private boolean mMovedCameraToCapital = false;

    /**
     * Create a {@link HumanPlayer}.
     *
     * @param networkManager The network manager.
     * @param netId The human player's network ID.
     */
    public HumanPlayer(Reference<NetworkManager> networkManager, int netId) {
        mNetworkManager = networkManager;
        mNetId = netId;
    }

    @Override
    public void onStart() {

        getGameObject()
                .buildChild(
                        "zoom_slider",
                        new TransformUI(true),
                        (go) -> go.addComponent(new UILinkedScrollBar()));

        Reference<GameObject> tmpRef =
                getGameObject()
                        .buildChild(
                                "menu_drawer",
                                new TransformUI(true),
                                (go) -> {
                                    mTokenCounterObject =
                                            go.buildChild(
                                                    "token_counter",
                                                    new TransformUI(true),
                                                    (self) -> {
                                                        UITokenCounter tokenCounter =
                                                                new UITokenCounter();
                                                        self.addComponent(tokenCounter);
                                                    });
                                    go.addComponent(
                                            new UIMenuLeftDrawer(
                                                    this::getBuildingChosen,
                                                    this::setBuildingChosen,
                                                    this::getHexChosen,
                                                    this::setHexChosen,
                                                    this::setScreenOn,
                                                    this::getPlayer));
                                });

        mTokenCounter = mTokenCounterObject.get().getComponent(UITokenCounter.class);
        mMenuDrawer = tmpRef.get().getComponent(UIMenuLeftDrawer.class);

        mVisualsNeedUpdate = true;
    }

    @Override
    public void fixedUpdate(float deltaTime) {
    	
    	Player player = getPlayer();
    	if(player == null) return;

        if (!mMovedCameraToCapital) {
            TargetMovement targetRig = Scene.getActiveScene().getSingleton(TargetMovement.class);

            Building capital = player.getCapital();

            if (targetRig != null && capital != null) {
                targetRig.setTarget(capital.getGameObject().getTransform());
                mMovedCameraToCapital = true;
            }
        }

        if (player.hasLost()) {
            log.warning("You've lost your capital");
            setEnabled(false);
            return;
        }

        if (player.getNumberOfOwnedBuildings() == 0) {
            log.warning("You have 0 buildings -- should be sorted in mo");
            return;
        }
        
        // Update token
        updateVisibleTokens();
    }

    private void updateVisibleTokens() {
        mLocalTokens = getPlayer().getTokens().get();
        if (Reference.isValid(mTokenCounter)) {
            mTokenCounter.get().setLabelReference(mLocalTokens);
        } else {
            mTokenCounter = getGameObject().getComponent(UITokenCounter.class);
        }
    }

    @Override
    public void frameUpdate(float deltaTime) {
        // Choose which screen to show

        if (Reference.isValid(mMenuDrawer)) {
            mMenuDrawer.get().setVisibleScreen(mScreenOn);
        }

        mapScreen();

        if (mVisualsNeedUpdate) {
            updateVisuals();
        }
    }

    /** This will choose what to do when the user can see the full map. */
    private void mapScreen() {

    	Player player = getPlayer();
    	if(player == null) return;
    	
        Cursor cursor = Actions.getCursor();

        // Checks that its clicking something
        if (GameActions.LEFT_CLICK.isJustDeactivated()
                && (cursor == null || !cursor.hadLittleDrag())) {
            if (UIManager.getInstance().getHoveredObject() == null) {
                // And then select the tile
                HexagonMap component = player.getMap();
                if (component != null) {
                    mLastHexChosen = mHexChosen;
                    mHexChosen = component.cursorToTile();
                    if (mHexChosen != null) {
                        Building building = mHexChosen.getBuilding();
                        if (building != null)
                            setBuildingChosen(building.getReference(Building.class));
                    }
                }

                if (mScreenOn == Screen.ATTACKING_SCREEN) {
                    return;
                }
                log.fine("Human:Got the Hexagon to enter");
                if (mHexChosen != null) {
                    if (mHexChosen.hasBuilding()) {
                        Building building = mHexChosen.getBuilding();

                        if (player.isBuildingOwner(building)) {
                            mBuildingChosen = building.getReference(Building.class);
                            setScreenOn(Screen.BUILDING_SELECTED_SCREEN);
                        }
                    } else {
                        if (mHexChosen.isBuildable(player)) { // If you can build
                            // If you can build
                            log.info("Human:Change Screen");
                            setScreenOn(Screen.PLACING_NEW_BUILDING);
                        } else {
                            log.info("Human:Cannot build");
                            mBuildingChosen = null;
                            setScreenOn(Screen.DEFAULT_SCREEN);
                        }
                    }
                }
            } else if (GameActions.RIGHT_CLICK.isJustDeactivated()) {
                HexagonTile tile = player.getMap().cursorToTile();
                Vector3f pos = new Vector3f(tile.getQ(), tile.getR(), tile.getS());
                log.info("[DEBUG] RCL Position From Camera : " + pos);
            }
        }
    }

    /* AURI!! This updates what the user can see */
    private void updateVisuals() {
        
    	Player player = getPlayer();
    	MapEffects effects = getMapEffects();
        if (player == null || effects == null) return;

        mVisualsNeedUpdate = false;
        
        if (!player.hasLost()) {
            if (Reference.isValid(mMenuDrawer)) {
                mMenuDrawer.get().setVisibleScreen(mScreenOn);
            }

            effects.setActivePlayer(mPlayer);

            switch (mScreenOn) {
                case DEFAULT_SCREEN:
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    break;
                case BUILDING_SELECTED_SCREEN:
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));
                    break;
                case UPGRADE_SCREEN:
                    break;
                case ATTACKING_SCREEN:
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.ATTACK_DARKER));
                    break;
                case SELLING_SCREEN:
                    break;
                case PLACING_NEW_BUILDING:
                    effects.setHighlightOverlay(
                            (fx) -> highlightSelectedTile(fx, StandardHighlightType.VALID));

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + mScreenOn);
            }
        }
    }

    private void highlightSelectedTile(MapEffects fx, StandardHighlightType highlight) {
        if (mHexChosen != null) {
            fx.highlightTile(mHexChosen, highlight.asSelection());
        }
    }

    /**
     * Sets screen and notifies that an update is needed for the visuals.
     *
     * @param newScreen the new screen
     */
    private void setScreenOn(Screen newScreen) {
        if (!newScreen.equals(mScreenOn) || (mLastHexChosen != mHexChosen)) {
            mVisualsNeedUpdate = true;
        }
        mScreenOn = newScreen;
    }
    
    /**
     * Get the {@link Player} associated with this HumanPlayer.
     * <p>
     * If {@link #mPlayer} is not valid, it will attempt to get a valid Player.
     * 
     * @return The {@link Player}, or {@code null} if there is no associated Player.
     */
    private Player getPlayer() {
    	// Try getting the player if haven't already
        if (!Reference.isValid(mPlayer)) {
            if(!Reference.isValid(mNetworkManager)) return null;
        	NetworkManager manager = mNetworkManager.get();
        	
            if(manager == null || manager.getClientManager() == null) return null;
            Reference<Player> player =  manager.getClientManager()
                                .getNetworkObjects()
                                .filter(Reference::isValid)
                                .map(Reference::get)
                                .filter(NetworkObject::isMine)
                                .map(NetworkObject::getGameObject)
                                .map(go -> go.getComponent(Player.class))
                                .filter(Objects::nonNull)
                                .findFirst()
                                .orElse(null);
            
            // Ensure player is not null (a valid Reference).
            if(!Reference.isValid(player)) return null;
            mPlayer = player;
        }
    	
    	return mPlayer.get();
    }

    /**
     *  Get the {@link MapEffects}.
     * <p>
     * If {@link #mMapEffects} is not valid, it will attempt to get a valid MapEffects.
     * 
     * @return The {@link MapEffects}; otherwise {@code null}.
     */
    private MapEffects getMapEffects() {
		if(!Reference.isValid(mMapEffects)) {
			MapEffects mapEffects = Scene.getActiveScene()
                    .getSingleton(MapEffects.class);
			
			if(mapEffects == null) return null;
			mMapEffects = mapEffects.getReference(MapEffects.class);
		}
    	
    	return mMapEffects.get();
    }
    
    @Override
    protected void onDestroy() {}
}
