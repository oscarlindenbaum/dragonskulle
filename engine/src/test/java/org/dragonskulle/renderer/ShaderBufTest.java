/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.dragonskulle.core.Resource;
import org.junit.Test;

/** Unit test for simple App. */
public class ShaderBufTest {
    // The below 2 tests were generated by https://www.diffblue.com/

    @Test
    public void testGetResource() {
        assertNull(
                ShaderBuf.getResource(
                        "Name",
                        ShaderKind.VERTEX_SHADER,
                        new ShaderBuf.MacroDefinition("Name", "42")));
    }

    @Test
    public void testFree() {
        ShaderBuf shaderBuf = new ShaderBuf();
        shaderBuf.free();
        assertNull(shaderBuf.getBuffer());
    }

    @Test
    public void testLoadSimple() {
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("simple", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadWithIncludes() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("with_includes", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadUnlit() {
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("unlit", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
        try (Resource<ShaderBuf> res = ShaderBuf.getResource("unlit", ShaderKind.FRAGMENT_SHADER)) {
            assertNotNull(res);
        }
    }

    @Test
    public void testLoadPBR() {
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("standard", ShaderKind.VERTEX_SHADER)) {
            assertNotNull(res);
        }
        try (Resource<ShaderBuf> res =
                ShaderBuf.getResource("standard", ShaderKind.FRAGMENT_SHADER)) {
            assertNotNull(res);
        }
    }
}
