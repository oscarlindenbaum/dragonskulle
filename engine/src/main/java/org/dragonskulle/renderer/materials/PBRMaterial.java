/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import static org.lwjgl.vulkan.VK10.*;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.Texture;
import org.dragonskulle.renderer.TextureMapping;
import org.dragonskulle.renderer.TextureMapping.*;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Reference unlit material
 *
 * <p>This material provides basic unlit rendering of objects with textures. One texture is
 * settable, alongside per-object colour value.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class PBRMaterial
        implements IMaterial, IColouredMaterial, IRefCountedMaterial, Serializable {
    public static class StandardShaderSet extends ShaderSet {
        public StandardShaderSet() {
            mVertexShader = ShaderBuf.getResource("standard", ShaderKind.VERTEX_SHADER);
            mFragmentShader = ShaderBuf.getResource("standard", ShaderKind.FRAGMENT_SHADER);

            mVertexBindingDescription = BindingDescription.instancedWithMatrix(4 * 4);
            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, 0));
            mNumFragmentTextures = 1;
        }

        public StandardShaderSet enableAlpha() {
            // TODO: add order independent transparency
            mRenderOrder = ShaderSet.RenderOrder.TRANSPARENT.getValue();
            mAlphaBlend = true;
            mPreSort = true;
            return this;
        }
    }

    private static final StandardShaderSet OPAQUE_SET = new StandardShaderSet();
    private static final StandardShaderSet TRANSPARENT_SET = new StandardShaderSet().enableAlpha();

    protected SampledTexture[] mFragmentTextures = {
        new SampledTexture(
                Texture.getResource("white.bmp"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT)),
        new SampledTexture(
                Texture.getResource("white.bmp"),
                new TextureMapping(TextureFiltering.LINEAR, TextureWrapping.REPEAT)),
    };

    /** Base colour of the surface. It will multiply the texture's colour */
    @Getter private final Vector4f mColour = new Vector4f(1.f);
    /** Controls which alpha values are cut off */
    @Getter @Setter private float mAlphaCutoff = 0f;
    /** Metalicness multiplier */
    @Getter @Setter private float mMetallic = 1f;
    /** Roughness multiplier */
    @Getter @Setter private float mRoughness = 1f;
    /** Normal map multiplier */
    @Getter @Setter private float mNormal = 1f;

    private int mRefCount = 0;

    /** Constructor for StandardMaterial */
    public PBRMaterial() {}

    /**
     * Constructor for StandardMaterial
     *
     * @param texture initial texture of the object
     */
    public PBRMaterial(SampledTexture texture) {
        mFragmentTextures[0] = texture;
    }

    /**
     * Constructor for StandardMaterial
     *
     * @param texture initial texture of the object
     * @param colour colour of the material
     */
    public PBRMaterial(SampledTexture texture, Vector4f colour) {
        mFragmentTextures[0] = texture;
        mColour.set(colour);
    }

    /**
     * Constructor for StandardMaterial
     *
     * @param colour colour of the material
     */
    public PBRMaterial(Vector4f colour) {
        mColour.set(colour);
    }

    public ShaderSet getShaderSet() {
        if (mColour.w < 1f) return TRANSPARENT_SET;
        return OPAQUE_SET;
    }

    public void writeVertexInstanceData(int offset, ByteBuffer buffer, Matrix4fc matrix) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        mColour.get(offset, buffer);
    }

    public SampledTexture[] getFragmentTextures() {
        return mFragmentTextures;
    }

    public IRefCountedMaterial incRefCount() {
        mRefCount++;
        return this;
    }

    public void free() {
        if (--mRefCount < 0) for (SampledTexture tex : mFragmentTextures) tex.free();
    }
}