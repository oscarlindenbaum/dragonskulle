/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.util.shaderc.Shaderc.*;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum ShaderKind {
    VERTEX_SHADER(shaderc_glsl_vertex_shader),
    GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
    FRAGMENT_SHADER(shaderc_glsl_fragment_shader);

    @Accessors(prefix = "m")
    @Getter
    private final int mKind;

    ShaderKind(int kind) {
        this.mKind = kind;
    }

    @Override
    public String toString() {
        if (this == VERTEX_SHADER) return "vert";
        else if (this == GEOMETRY_SHADER) return "geom";
        else if (this == FRAGMENT_SHADER) return "frag";
        else return "none";
    }
}