/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio.formats;

import static org.junit.Assert.assertNull;

import java.nio.file.Paths;
import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class DiffBlueWaveSoundTest {
    @Test
    public void testLoadWave() {
        assertNull(
                WaveSound.loadWave(
                        Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toFile()));
    }
}
