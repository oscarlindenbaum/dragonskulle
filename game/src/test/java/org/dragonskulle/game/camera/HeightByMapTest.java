/* (C) 2021 DragonSkulle */
package org.dragonskulle.game.camera;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class HeightByMapTest {
    @Test
    public void testFrameUpdate() {
        HeightByMap heightByMap = new HeightByMap();
        heightByMap.frameUpdate(0.5f);
        assertFalse(heightByMap.isStarted());
        assertTrue(heightByMap.isEnabled());
        assertFalse(heightByMap.isAwake());
        assertEquals(0.0f, heightByMap.getZoomLevel(), 0.0f);
        assertEquals(0.0f, heightByMap.getMinHeightLerped(), 0.0f);
        assertEquals(1.0f, heightByMap.getHeightOffset(), 0.0f);
        assertEquals(0.1f, heightByMap.getMaxZoomValue(), 0.0f);
        assertEquals(5.0f, heightByMap.getLerpSpeed(), 0.0f);
    }

    @Test
    public void testOnDestroy() {
        HeightByMap heightByMap = new HeightByMap();
        heightByMap.onDestroy();
        assertFalse(heightByMap.isStarted());
        assertTrue(heightByMap.isEnabled());
        assertFalse(heightByMap.isAwake());
        assertEquals(0.0f, heightByMap.getZoomLevel(), 0.0f);
        assertEquals(0.0f, heightByMap.getMinHeightLerped(), 0.0f);
        assertEquals(1.0f, heightByMap.getHeightOffset(), 0.0f);
        assertEquals(0.1f, heightByMap.getMaxZoomValue(), 0.0f);
        assertEquals(5.0f, heightByMap.getLerpSpeed(), 0.0f);
    }
}
