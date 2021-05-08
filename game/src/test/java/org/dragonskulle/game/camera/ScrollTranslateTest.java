/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.dragonskulle.core.GameObject;
import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class ScrollTranslateTest {
    @Test
    public void testConstructor() {
        ScrollTranslate actualScrollTranslate = new ScrollTranslate();
        assertFalse(actualScrollTranslate.isStarted());
        assertEquals(0.01f, actualScrollTranslate.getMinSpeed(), 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getTargetLerpTime(), 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getZoomLevel(), 0.0f);
        assertEquals(1.5f, actualScrollTranslate.getPowFactor(), 0.0f);
        assertFalse(actualScrollTranslate.isAwake());
        assertTrue(actualScrollTranslate.isEnabled());
        assertEquals(0.1f, actualScrollTranslate.getMaxSpeed(), 0.0f);
        assertSame(actualScrollTranslate, actualScrollTranslate.getReference().get());
        assertEquals(0.0f, actualScrollTranslate.getStartPos().z, 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getEndPos().y, 0.0f);
    }

    @Test
    public void testConstructor2() {
        HeightByMap heightByMap = new HeightByMap();
        HeightByMap heightByMap1 = new HeightByMap();
        ScrollTranslate actualScrollTranslate =
                new ScrollTranslate(heightByMap, heightByMap1, new HeightByMap());
        assertFalse(actualScrollTranslate.isStarted());
        assertEquals(0.01f, actualScrollTranslate.getMinSpeed(), 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getTargetLerpTime(), 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getZoomLevel(), 0.0f);
        assertEquals(1.5f, actualScrollTranslate.getPowFactor(), 0.0f);
        assertFalse(actualScrollTranslate.isAwake());
        assertTrue(actualScrollTranslate.isEnabled());
        assertEquals(0.1f, actualScrollTranslate.getMaxSpeed(), 0.0f);
        assertSame(actualScrollTranslate, actualScrollTranslate.getReference().get());
        assertEquals(0.0f, actualScrollTranslate.getStartPos().z, 0.0f);
        assertEquals(0.0f, actualScrollTranslate.getEndPos().y, 0.0f);
    }

    @Test
    public void testOnAwake() {
        ScrollTranslate scrollTranslate = new ScrollTranslate();
        scrollTranslate.setGameObject(new GameObject("Name"));
        scrollTranslate.onAwake();
        assertEquals(0.0f, scrollTranslate.getTargetLerpTime(), 0.0f);
    }

    @Test
    public void testFrameUpdate() {
        ScrollTranslate scrollTranslate = new ScrollTranslate();
        scrollTranslate.frameUpdate(0.5f);
        assertFalse(scrollTranslate.isStarted());
        assertEquals(0.01f, scrollTranslate.getMinSpeed(), 0.0f);
        assertEquals(0.0f, scrollTranslate.getTargetLerpTime(), 0.0f);
        assertEquals(0.0f, scrollTranslate.getZoomLevel(), 0.0f);
        assertEquals(1.5f, scrollTranslate.getPowFactor(), 0.0f);
        assertFalse(scrollTranslate.isAwake());
        assertTrue(scrollTranslate.isEnabled());
        assertEquals(0.1f, scrollTranslate.getMaxSpeed(), 0.0f);
    }

    @Test
    public void testOnDestroy() {
        ScrollTranslate scrollTranslate = new ScrollTranslate();
        scrollTranslate.onDestroy();
        assertFalse(scrollTranslate.isStarted());
        assertEquals(0.01f, scrollTranslate.getMinSpeed(), 0.0f);
        assertEquals(0.0f, scrollTranslate.getTargetLerpTime(), 0.0f);
        assertEquals(0.0f, scrollTranslate.getZoomLevel(), 0.0f);
        assertEquals(1.5f, scrollTranslate.getPowFactor(), 0.0f);
        assertFalse(scrollTranslate.isAwake());
        assertTrue(scrollTranslate.isEnabled());
        assertEquals(0.1f, scrollTranslate.getMaxSpeed(), 0.0f);
    }
}
