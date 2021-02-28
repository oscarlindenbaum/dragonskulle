/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;

/**
 * Once attached to a window, this allows access to:
 *
 * <ul>
 *   <li>The cursor's current position.
 *   <li>The starting location of a cursor drag (with the drag being triggered by {@link
 *       Action#DRAG}).
 *   <li>The distance of a drag.
 *   <li>The angle of a drag.
 * </ul>
 *
 * @author Craig Wilbourne
 */
public class Cursor {

    /** This cursor's current position. */
    private Vector2d mPosition = new Vector2d(0, 0);
    /** The starting position of a drag, or {@code null} if no drag is taking place. */
    private Vector2d mDragStart;
    /** Allows {@link Action}s to be activated and deactivated. */
    private StoredActions mActions;

    /**
     * Create a new cursor manager.
     *
     * @param actions The actions to be triggered.
     */
    public Cursor(StoredActions actions) {
        mActions = actions;
    }

    /**
     * Attach this cursor to a window.
     *
     * <p>Required to allow this cursor to access to this window.
     *
     * @param window The window to attach to.
     * @param actions The user's {@link StoredActions}.
     */
    void attachToWindow(long window) {
        // Listen for cursor position events.
        GLFWCursorPosCallback listener =
                new GLFWCursorPosCallback() {
                    @Override
                    public void invoke(long window, double x, double y) {
                        setPosition(x, y);
                        detectDrag(mActions);
                    }
                };

        GLFW.glfwSetCursorPosCallback(window, listener);
    }

    /**
     * Set the current position of the mouse.
     *
     * @param x
     * @param y
     */
    void setPosition(double x, double y) {
        mPosition.set(x, y);
    }

    /**
     * Detects whether the cursor is being dragged.
     *
     * <p>Starts a new drag if {@link Action#DRAG} is active. Ends a drag in progress if {@link
     * Action#DRAG} is not active.
     *
     * @param actions The user's {@link StoredActions}.
     */
    private void detectDrag(StoredActions actions) {
        if (actions.isActivated(MyActions.DRAG)) {
            if (inDrag() == false) {
                startDrag();
            }
        } else {
            if (inDrag() == true) {
                endDrag();
            }
        }
    }

    /** Start a new drag. */
    void startDrag() {
        mDragStart = new Vector2d(mPosition);
    }

    /** End a drag in progress. */
    void endDrag() {
        mDragStart = null;
    }

    /**
     * Whether the user is currently dragging the cursor.
     *
     * @return {@code true} if the cursor is currently being dragged; otherwise {@code false}.
     */
    public boolean inDrag() {
        return !(mDragStart == null);
    }

    /**
     * Get the current location of this cursor in the window.
     *
     * @return The current cursor position.
     */
    public Vector2d getPosition() {
        return mPosition;
    }

    /**
     * Get the position of where the drag began.
     *
     * @return The start position of the cursor, or {@code null} if no dragging is taking place.
     */
    public Vector2d getDragStart() {
        return mDragStart;
    }

    /**
     * Get the direct distance between where this cursor started dragging and its current position.
     *
     * @return A {@code double} representing the distance from the starting point of the user's
     *     drag, or {@code 0} if no dragging is taking place.
     */
    public double getDragDistance() {
        if (!inDrag()) {
            return 0;
        }
        return mPosition.distance(mDragStart);
    }

    /**
     * Get the angle between where this cursor started dragging and its current position.
     *
     * @return The angle, in radians, between the drag start point and current position, or {@code
     *     0} if no dragging is taking place.
     */
    public double getDragAngle() {
        if (!inDrag()) {
            return 0;
        }
        return mPosition.angle(mDragStart);
    }
}
