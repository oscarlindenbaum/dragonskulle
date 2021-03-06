/* (C) 2021 DragonSkulle */
package org.dragonskulle.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class DiffBlueEnvTest {
    @Test
    public void testEnvString() {
        assertEquals("Default Val", Env.envString("Key", "Default Val"));
        assertEquals("Default Val", Env.envString("42", "Default Val"));
    }

    @Test
    public void testEnvInt() {
        assertEquals(42, Env.envInt("Key", 42));
        assertEquals(42, Env.envInt("42", 42));
    }

    @Test
    public void testEnvBool() {
        assertTrue(Env.envBool("Key", true));
        assertTrue(Env.envBool("42", true));
    }
}
