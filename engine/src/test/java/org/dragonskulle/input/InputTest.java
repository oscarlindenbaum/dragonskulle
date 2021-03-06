/* (C) 2021 DragonSkulle */
package org.dragonskulle.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.dragonskulle.input.test_bindings.TestActions;
import org.dragonskulle.input.test_bindings.TestBindings;
import org.joml.Vector2f;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link Input}.
 *
 * @author Craig Wilbourne
 */
public class InputTest {

    /** An arbitrary key codes used for testing key presses. */
    public static final int TEST_KEY_1 = -12345;

    public static final int TEST_KEY_2 = -12300;

    /** Before every test, reinitialise the Input. */
    @Before
    public void createWindowInput() {
        Input.initialise(null, new TestBindings());
    }

    @Test
    public void bindingsNotNull() {
        Bindings bindings = Input.getBindings();
        assertNotNull(bindings);
    }

    @Test
    public void buttonsNotNull() {
        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);
    }

    @Test
    public void cursorNotNull() {
        Cursor cursor = TestActions.getCursor();
        assertNotNull(cursor);
    }

    @Test
    public void scrollNotNull() {
        Scroll scroll = TestActions.getScroll();
        assertNotNull(scroll);
    }

    /** Ensure button presses can be triggered and stored. */
    @Test
    public void buttonShouldStorePress() {
        boolean activated;

        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);

        buttons.setPressed(TEST_KEY_1, true);
        activated = buttons.isPressed(TEST_KEY_1);
        assertTrue("Button TEST_KEY_1 should be activated (true).", activated);

        buttons.setPressed(TEST_KEY_1, false);
        activated = buttons.isPressed(TEST_KEY_1);
        assertFalse("Button TEST_KEY_1 should be deactivated (false).", activated);
    }

    /** Ensure that pressing a button activates and deactivates an action. */
    @Test
    public void buttonShouldActivateAction() {
        // Parameters:
        int button = TEST_KEY_1;
        Action action = TestActions.TEST_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String buttonName = String.format("[Button Code: %d]", button);
        String actionName = action.toString();

        // Run the test:
        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);

        buttons.press(button);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s has been pressed.",
                        actionName, buttonName),
                activated);

        buttons.release(button);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s has been released.",
                        actionName, buttonName),
                activated);
    }

    /** Ensure that pressing a multiple buttons activates and deactivates an action. */
    @Test
    public void multipleButtonsShouldActivateAction() {
        // Parameters:
        int button1 = TEST_KEY_1;
        int button2 = TEST_KEY_2;
        Action action = TestActions.TEST_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String button1Name = String.format("[Button Code: %d]", button1);
        String button2Name = String.format("[Button Code: %d]", button2);
        String actionName = action.toString();

        // Run the test:
        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);

        buttons.press(button1);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s has been pressed.",
                        actionName, button1Name),
                activated);

        buttons.press(button2);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s and %s has been pressed.",
                        actionName, button1Name, button2Name),
                activated);

        buttons.release(button1);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s is still being pressed.",
                        actionName, button1Name),
                activated);

        buttons.release(button2);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s and %s have been released.",
                        actionName, button1Name, button2Name),
                activated);
    }

    /** Ensure that actions that do not have any triggers remain deactivated. */
    @Test
    public void actionWithoutTrigger() {
        // Parameters:
        Action action = TestActions.UNLINKED_ACTION;

        // For logic:
        boolean activated;

        // For error messages:
        String actionName = action.toString();

        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as nothing can activate it.", actionName),
                activated);
    }

    /** Ensure the bindings can be modified. */
    @Test
    public void modifyBindings() {
        // Parameters:
        int button = TEST_KEY_1;
        Action action = TestActions.TEST_ACTION_2;

        // For logic:
        boolean activated;

        // For error messages:
        String buttonName = String.format("[Button Code: %d]", button);
        String actionName = action.toString();

        // Run the test:
        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);
        Bindings bindings = Input.getBindings();
        assertNotNull(bindings);

        // Press the button, but this should not activate the action.
        buttons.press(button);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s is not currently binded to the action.",
                        actionName, buttonName),
                activated);
        buttons.release(button);

        // Bind the action to the button.
        bindings.addBinding(button, action);

        // Press the button again.
        buttons.press(button);
        activated = action.isActivated();
        assertTrue(
                String.format(
                        "%s should be activated (true) as %s is now binded to the action.",
                        actionName, buttonName),
                activated);
        buttons.release(button);

        // Unbind the button.
        bindings.removeBinding(button);

        // Press the button, but this should not activate the action.
        buttons.press(button);
        activated = action.isActivated();
        assertFalse(
                String.format(
                        "%s should be deactivated (false) as %s should have been unbinded for all actions.",
                        actionName, buttonName),
                activated);
        buttons.release(button);
    }

    /** Ensure {@link Scroll} is storing the amount scrolled (since last {@link Scroll#reset()}). */
    @Test
    public void scrollShouldStoreAmount() {
        Scroll scroll = TestActions.getScroll();
        assertNotNull(scroll);

        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), 0, 0);

        scroll.add(10);
        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), 10, 0);

        scroll.add(-999);
        assertEquals("Scroll amount incorrect. ", scroll.getAmount(), -989, 0);
    }

    /** Ensure calling {@link Scroll#reset()} resets the scroll values. */
    @Test
    public void scrollResetClearsValues() {
        // For logic:
        boolean activated;

        Buttons buttons = Input.getButtons();
        assertNotNull(buttons);

        Scroll scroll = Actions.getScroll();
        assertNotNull(scroll);

        // Manually simulate scrolling.
        buttons.press(Scroll.UP);
        buttons.press(Scroll.DOWN);
        scroll.add(-100d);

        // Reset the scrolling.
        scroll.reset();

        // Ensure all the button presses and the scroll amount has been reset.
        activated = buttons.isPressed(Scroll.UP);
        assertFalse(
                "Scroll.UP should be deactivated (false) as scrolling has been reset.", activated);

        activated = buttons.isPressed(Scroll.DOWN);
        assertFalse(
                "Scroll.DOWN should be deactivated (false) as scrolling has been reset.",
                activated);

        assertEquals("Scroll amount has been reset and should be 0.", scroll.getAmount(), 0, 0);
    }

    /** Ensure the raw cursor position is correctly stored. */
    @Test
    public void cursorPositionShouldBeStored() {
        Cursor cursor = Actions.getCursor();
        assertNotNull(cursor);

        cursor.setPosition(123f, 456f);
        Vector2f desired = new Vector2f(123f, 456f);

        assertEquals(
                "Raw cursor position should equal Vector2d(123d, 456d).",
                cursor.getRawPosition(),
                desired);
    }

    /**
     * Ensure that when no dragging is taking place, the drag start location is null and the angle
     * and distance is 0.
     */
    @Test
    public void noDragShouldCauseNullOrZero() {
        Cursor cursor = Actions.getCursor();
        assertNotNull(cursor);

        assertNull("No drag has begun, so DragStart should be null.", cursor.getDragStart());

        assertEquals(
                "No drag has begun, so the drag distance should be 0.",
                cursor.getDragDistance(),
                0,
                0);

        assertEquals(
                "No drag has begun, so the drag angle should be 0.", cursor.getDragAngle(), 0, 0);
    }

    /** Ensure the angle calculated between the drag start and current position is correct. */
    @Test
    public void cursorAngleCorrect() {
        Cursor cursor = Actions.getCursor();
        assertNotNull(cursor);

        Vector2f desiredStart = new Vector2f(123f, 456f);
        Vector2f desiredEnd1 = new Vector2f(6f, 110f);
        Vector2f desiredEnd2 = new Vector2f(777f, 0f);
        double desiredAngle1 = desiredEnd1.angle(desiredStart);
        double desiredAngle2 = desiredEnd2.angle(desiredStart);

        cursor.setPosition(123f, 456f);
        cursor.startDrag();

        cursor.setPosition(6f, 110f);
        assertEquals(
                "The angle between the drag start point and current position is not correct.",
                cursor.getDragAngle(),
                desiredAngle1,
                1e-15);

        cursor.setPosition(777f, 0f);
        assertEquals(
                "The angle between the drag start point and current position is not correct.",
                cursor.getDragAngle(),
                desiredAngle2,
                1e-15);

        cursor.endDrag();
    }

    /** Ensure the cursor can detect when it is being dragged. */
    @Test
    public void cursorDetectInDrag() {
        Cursor cursor = Actions.getCursor();
        assertNotNull(cursor);

        assertFalse("Cursor is in drag, but it should not be.", cursor.inDrag());

        cursor.startDrag();
        assertTrue("Cursor is not drag, but it should be.", cursor.inDrag());

        cursor.endDrag();
        assertFalse("Cursor is in drag, but it should not be.", cursor.inDrag());
    }
}
