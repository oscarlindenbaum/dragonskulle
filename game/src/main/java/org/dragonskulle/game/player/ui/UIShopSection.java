/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.player.ui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.IOnStart;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIButton;
import org.dragonskulle.ui.UIText;

/**
 * This is a section of the shop which can contain a sub-screen to its super window. For example the
 * shop can show additional options like in {@code ShopState.MY_BUILDING_SELECTED}.
 *
 * @author Oscar L
 */
@Accessors(prefix = "m")
@Log
public class UIShopSection extends Component implements IOnStart {
    @Getter private ShopState mState = ShopState.MY_BUILDING_SELECTED;
    @Setter @Getter private ShopState mLastState = ShopState.CLOSED;
    private Reference<GameObject> mDefaultPanel;
    private Reference<GameObject> mNewBuildingPanel;
    private Reference<GameObject> mUpgradePanel;

    @Getter
    @Accessors(fluent = true, prefix = "m")
    private boolean mDidBuild = false;

    @Getter(AccessLevel.PROTECTED)
    private final UIMenuLeftDrawer mParent;

    @Setter @Getter private Reference<GameObject> mCurrentPanel;
    private Reference<UIText> mTitleRef;

    /**
     * Constructor.
     *
     * @param mParent the parent who's IParams we are reusing
     */
    public UIShopSection(UIMenuLeftDrawer mParent) {
        this.mParent = mParent;
    }

    /**
     * Sets the flag if a new building was placed. This will allow us to update the building until
     * updated via network.
     *
     * @param state the state
     */
    public void markDidBuild(boolean state) {
        mDidBuild = state;
    }

    /** The Shop state. This controls what can be seen at what time. */
    public enum ShopState {
        CLOSED,
        DEFAULT,
        BUILDING_NEW,
        ATTACK_SCREEN,
        MY_BUILDING_SELECTED
    }

    /** User-defined destroy method, this is what needs to be overridden instead of destroy. */
    @Override
    protected void onDestroy() {}

    /**
     * Sets the shop state.
     *
     * @param state the state
     */
    protected void setState(ShopState state) {
        if (state != getLastState()) {
            log.fine("Setting visible state to " + state);

            String newText = "";
            Reference<GameObject> newPanel;

            switch (state) {
                case ATTACK_SCREEN:
                    newPanel = null;
                    break;
                case DEFAULT:
                    newPanel = mDefaultPanel;
                    newText = "Choose Action";
                    break;
                case BUILDING_NEW:
                    newPanel = mNewBuildingPanel;
                    newText = "Create Building";
                    break;
                case MY_BUILDING_SELECTED:
                    newPanel = mUpgradePanel;
                    newText = "Upgrade Building";
                    break;
                case CLOSED:
                    newPanel = null;
                    newText = "";
                    show(mDefaultPanel, false);
                    show(mCurrentPanel, false);
                    show(mUpgradePanel, false);
                    show(mNewBuildingPanel, false);
                    break;
                default:
                    if (Reference.isValid(mTitleRef)) {
                        mTitleRef.get().setText("");
                    }
                    setState(ShopState.CLOSED);
                    return;
            }

            if (Reference.isValid(mTitleRef)) {
                mTitleRef.get().setText(newText);
            }
            setLastState(state);
            swapPanels(newPanel);
        }
    }
    /**
     * Show the component's GO.
     *
     * @param component the component
     * @param shouldShow true if should show
     */
    private void show(Reference<GameObject> component, boolean shouldShow) {
        if (Reference.isValid(component)) {
            component.get().setEnabled(shouldShow);
        }
    }

    /**
     * Swaps the visible panels.
     *
     * @param newPanel the new panel
     */
    private void swapPanels(Reference<GameObject> newPanel) {
        log.fine("Swapping panels.");
        // if there is a screen being shown
        // deactivate the panel
        if (Reference.isValid(mCurrentPanel)) {
            mCurrentPanel.get().setEnabled(false);
        }

        if (Reference.isValid(newPanel)) {
            newPanel.get().setEnabled(true);
        }
        mCurrentPanel = newPanel;
    }

    /**
     * Called when a component is first added to a scene, after onAwake and before the first
     * frameUpdate. Used for setup of references to necessary Components and GameObjects
     */
    @Override
    public void onStart() {

        mDefaultPanel =
                getGameObject()
                        .buildChild(
                                "default_object",
                                new TransformUI(false),
                                (self) -> {
                                    GameObject buttonObject =
                                            new GameObject("build_button", new TransformUI(true));
                                    UIButton button =
                                            new UIButton(
                                                    "Place Building",
                                                    (__, ___) -> {
                                                        getParent()
                                                                .mNotifyScreenChange
                                                                .call(Screen.PLACING_NEW_BUILDING);
                                                    });
                                    buttonObject.addComponent(button);
                                    TransformUI transformUI =
                                            buttonObject.getTransform(TransformUI.class);
                                    transformUI.setParentAnchor(0, 0.1f, 1, 0.19f + 0.25f);
                                    transformUI.setPivotOffset(0.5f, 0);

                                    self.addChild(buttonObject);
                                });

        show(mDefaultPanel, true);
        mCurrentPanel = mDefaultPanel;

        UIBuildingUpgrade uiBuildingUpgrade = new UIBuildingUpgrade(this);
        mUpgradePanel =
                getGameObject()
                        .buildChild(
                                "upgrade_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingUpgrade));
        show(mUpgradePanel, false);

        UIBuildingOptions uiBuildingOptions =
                new UIBuildingOptions(this); // TODO this is what needs improving now

        mNewBuildingPanel =
                getGameObject()
                        .buildChild(
                                "building_object",
                                new TransformUI(true),
                                (self) -> self.addComponent(uiBuildingOptions));
        show(mNewBuildingPanel, false);

        // Outer window stuff
        TransformUI tran = getGameObject().getTransform(TransformUI.class);
        //        0.08f, 0.86f, 1 - 0.08f, 1 - 0.03f when using transform
        tran.setParentAnchor(0.08f, 0.15f, 1 - 0.08f, 1 - 0.15f);
        Reference<GameObject> textObj =
                getGameObject()
                        .buildChild(
                                "main_shop_text",
                                new TransformUI(true),
                                (self) -> {
                                    UIText mWindowText = new UIText("Choose Action");
                                    self.addComponent(mWindowText);
                                    mTitleRef = mWindowText.getReference(UIText.class);
                                });

        TransformUI mTextTransform = textObj.get().getTransform(TransformUI.class);
        mTextTransform.setParentAnchor(0.05f, 0f);
        mTextTransform.translate(0, -0.65f); // remove if using transforms
    }
}
