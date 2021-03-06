/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer.materials;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Scene;
import org.dragonskulle.renderer.AttributeDescription;
import org.dragonskulle.renderer.BindingDescription;
import org.dragonskulle.renderer.SampledTexture;
import org.dragonskulle.renderer.ShaderBuf;
import org.dragonskulle.renderer.ShaderBuf.MacroDefinition;
import org.dragonskulle.renderer.ShaderKind;
import org.dragonskulle.renderer.ShaderSet;
import org.dragonskulle.renderer.components.Camera;
import org.dragonskulle.renderer.components.Light;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Reference PBR material
 *
 * <p>This material provides physically based rendering of objects with textures.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
public class PBRMaterial implements IColouredMaterial, IRefCountedMaterial {

    /** Cached shadersets for particular standard material configuration. */
    private static final Map<Integer, ShaderSet> sShaderSets = new TreeMap<Integer, ShaderSet>();

    /**
     * Standard shader set used by PBR materials.
     *
     * <p>This shader set is extensible, and allows to override the shader used, as well as add
     * custom properties on top.
     */
    public static class StandardShaderSet extends ShaderSet {

        /**
         * Construct a {@link StandardShaderSet} for {@link PBRMaterial}.
         *
         * <p>This will construct the standard shader set with no additional options and flags set.
         *
         * @param mat material to create shaderset for.
         */
        public StandardShaderSet(PBRMaterial mat) {
            this(mat, "standard", new ArrayList<>(), new ArrayList<>(), 0);
        }

        /**
         * Construct a {@link StandardShaderSet}.
         *
         * @param mat material to construct the shaderset for.
         * @param shaderName shader file to use.
         * @param fragMacroDefs list of custom macro definitions in the fragment shader.
         * @param vertMacroDefs list of custom macro definitions in the vertex shader.
         * @param extraInstanceDataSize additional per-instance data that is passed on top of the
         *     standard data.
         * @param extraDescriptions {@link AttributeDescription}s for the additional data.
         */
        public StandardShaderSet(
                PBRMaterial mat,
                String shaderName,
                List<MacroDefinition> fragMacroDefs,
                List<MacroDefinition> vertMacroDefs,
                int extraInstanceDataSize,
                AttributeDescription... extraDescriptions) {

            int textureCount = 0;
            if (mat.mAlbedoMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition("ALBEDO_BINDING", Integer.toString(textureCount++)));
            }
            if (mat.mNormalMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition("NORMAL_BINDING", Integer.toString(textureCount++)));
            }
            if (mat.mMetalnessRoughnessMap != null) {
                fragMacroDefs.add(
                        new MacroDefinition(
                                "METALNESS_ROUGHNESS_BINDING", Integer.toString(textureCount++)));
            }

            mNumFragmentTextures = textureCount;

            mLightCount = 2;

            MacroDefinition lights =
                    new MacroDefinition("NUM_LIGHTS", Integer.toString(mLightCount));
            fragMacroDefs.add(lights);
            vertMacroDefs.add(lights);

            if (mat.mAlphaBlend) {
                mRenderOrder = ShaderSet.RenderOrder.TRANSPARENT.getValue();
                mAlphaBlend = true;
                mPreSort = true;
                fragMacroDefs.add(new MacroDefinition("ALPHA_BLEND", "1"));
            }

            mVertexShader =
                    ShaderBuf.getResource(
                            shaderName,
                            ShaderKind.VERTEX_SHADER,
                            vertMacroDefs.stream().toArray(MacroDefinition[]::new));
            mFragmentShader =
                    ShaderBuf.getResource(
                            shaderName,
                            ShaderKind.FRAGMENT_SHADER,
                            fragMacroDefs.stream().toArray(MacroDefinition[]::new));

            mVertexBindingDescription =
                    BindingDescription.instancedWithMatrixAndLights(
                            NORMAL_OFFSET + 4 + extraInstanceDataSize, mLightCount);

            ArrayList<AttributeDescription> descriptions = new ArrayList<>();

            AttributeDescription[] regAttributes = {
                new AttributeDescription(1, 0, VK_FORMAT_R32G32B32A32_SFLOAT, COL_OFFSET),
                new AttributeDescription(1, 1, VK_FORMAT_R32G32B32_SFLOAT, EMISSION_COL_OFFSET),
                new AttributeDescription(1, 2, VK_FORMAT_R32G32B32_SFLOAT, CAM_OFFSET),
                new AttributeDescription(1, 3, VK_FORMAT_R32_SFLOAT, ALPHA_CUTOFF_OFFSET),
                new AttributeDescription(1, 4, VK_FORMAT_R32_SFLOAT, METALLIC_OFFSET),
                new AttributeDescription(1, 5, VK_FORMAT_R32_SFLOAT, ROUGHNESS_OFFSET),
                new AttributeDescription(1, 6, VK_FORMAT_R32_SFLOAT, NORMAL_OFFSET),
            };

            for (AttributeDescription desc : regAttributes) {
                descriptions.add(desc);
            }

            int binding = 5;

            for (AttributeDescription desc : extraDescriptions) {
                descriptions.add(
                        new AttributeDescription(
                                1,
                                ++binding,
                                desc.getFormat(),
                                NORMAL_OFFSET + 4 + desc.getOffset()));
            }

            mVertexAttributeDescriptions =
                    AttributeDescription.withMatrix(
                            AttributeDescription.withLights(
                                    mLightCount,
                                    descriptions.stream().toArray(AttributeDescription[]::new)));
        }
    }

    /**
     * Hash the material's shaderset.
     *
     * <p>This hash is used to identify whether 2 material instances can use the same shader set.
     *
     * @return integer representing the hash of the target shaderset.
     */
    protected int hashShaderSet() {
        int ret = 0;
        ret |= mAlbedoMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mNormalMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mMetalnessRoughnessMap != null ? 1 : 0;
        ret <<= 1;
        ret |= mAlphaBlend ? 1 : 0;
        return ret;
    }

    /** Colour offset within the instance buffer. */
    private static final int COL_OFFSET = 0;
    /** Emissive colour offset within the instance buffer. */
    private static final int EMISSION_COL_OFFSET = COL_OFFSET + 4 * 4;
    /** Camera position offset within the instance buffer. */
    private static final int CAM_OFFSET = EMISSION_COL_OFFSET + 3 * 4;
    /** Alpha cutoff float offset within the instance buffer. */
    private static final int ALPHA_CUTOFF_OFFSET = CAM_OFFSET + 3 * 4;
    /** Metalness float offset within the instance buffer. */
    private static final int METALLIC_OFFSET = ALPHA_CUTOFF_OFFSET + 4;
    /** Roughness float offset within the instance buffer. */
    private static final int ROUGHNESS_OFFSET = METALLIC_OFFSET + 4;
    /** Normal float offset within the instance buffer. */
    private static final int NORMAL_OFFSET = ROUGHNESS_OFFSET + 4;

    /** Albedo texture applied to the material. */
    @Getter protected SampledTexture mAlbedoMap;
    /** Normalmap texture applied to the material. */
    @Getter protected SampledTexture mNormalMap;
    /** Combined metalness and roughness texture applied to the material. */
    @Getter protected SampledTexture mMetalnessRoughnessMap;

    /** Internal cached list of textures to pass to the rendering system. */
    protected SampledTexture[] mFragmentTextures;

    /** Base colour of the surface. It will multiply the texture's colour. */
    @Getter private final Vector4f mColour = new Vector4f(1.f);
    /** Emissive colour of the surface. It will add to the texture's colour. */
    @Getter private final Vector3f mEmissionColour = new Vector3f(0.f);
    /** Controls which alpha values are cut off. */
    @Getter @Setter private float mAlphaCutoff = 0f;
    /** Metalicness multiplier. */
    @Getter @Setter private float mMetallic = 1f;
    /** Roughness multiplier. */
    @Getter @Setter private float mRoughness = 1f;
    /** Normal map multiplier. */
    @Getter @Setter private float mNormal = 1f;
    /** Have transparency. */
    @Getter @Setter private boolean mAlphaBlend = false;

    /** Internal reference count of the material. */
    private int mRefCount = 0;

    /** Constructor for StandardMaterial. */
    public PBRMaterial() {}

    /**
     * Constructor for StandardMaterial.
     *
     * @param albedoMap initial albedo/diffuse texture of the object
     */
    public PBRMaterial(SampledTexture albedoMap) {
        mAlbedoMap = albedoMap;
    }

    /**
     * Constructor for StandardMaterial.
     *
     * @param albedoMap initial texture of the object
     * @param colour colour of the material
     */
    public PBRMaterial(SampledTexture albedoMap, Vector4f colour) {
        mAlbedoMap = albedoMap;
        mColour.set(colour);
    }

    /**
     * Constructor for StandardMaterial.
     *
     * @param colour colour of the material
     */
    public PBRMaterial(Vector4f colour) {
        mColour.set(colour);
    }

    /**
     * Set the albedo map texture on the material.
     *
     * <p>This material will clone the input texture, and free the old one. Thus, it is necessary to
     * close the input texture, if it is no longer used.
     *
     * @param tex new texture to set.
     */
    public void setAlbedoMap(SampledTexture tex) {
        if (equalTexs(tex, mAlbedoMap)) {
            return;
        }

        if (mAlbedoMap != null) {
            mAlbedoMap.free();
        }
        mAlbedoMap = tex != null ? tex.clone() : null;
        mFragmentTextures = null;
    }

    /**
     * Set the normal map texture on the material.
     *
     * <p>This material will clone the input texture, and free the old one. Thus, it is necessary to
     * close the input texture, if it is no longer used.
     *
     * @param tex new texture to set.
     */
    public void setNormalMap(SampledTexture tex) {
        if (equalTexs(tex, mNormalMap)) {
            return;
        }

        if (mNormalMap != null) {
            mNormalMap.free();
        }

        if (tex != null) {
            mNormalMap = tex.clone();
            mNormalMap.setLinear(true);
            mFragmentTextures = null;
        } else if (mNormalMap != null) {
            mNormalMap = null;
            mFragmentTextures = null;
        }
    }

    /**
     * Set the combined metalness roughness map texture on the material.
     *
     * <p>This material will clone the input texture, and free the old one. Thus, it is necessary to
     * close the input texture, if it is no longer used.
     *
     * @param tex new texture to set.
     */
    public void setMetalnessRoughnessMap(SampledTexture tex) {
        if (equalTexs(tex, mMetalnessRoughnessMap)) {
            return;
        }

        if (mMetalnessRoughnessMap != null) {
            mMetalnessRoughnessMap.free();
        }

        if (tex != null) {
            mMetalnessRoughnessMap = tex.clone();
            mMetalnessRoughnessMap.setLinear(true);
            mFragmentTextures = null;
        } else if (mMetalnessRoughnessMap != null) {
            mMetalnessRoughnessMap = null;
            mFragmentTextures = null;
        }
    }

    /**
     * Get the {@link ShaderSet} used by this material.
     *
     * @return the shaderset appropriate for the properties set on the material.
     */
    public ShaderSet getShaderSet() {
        Integer hash = hashShaderSet();

        ShaderSet ret = sShaderSets.get(hash);

        if (ret == null) {
            ret = new StandardShaderSet(this);
            sShaderSets.put(hash, ret);
        }

        return ret;
    }

    /**
     * Write per-instance data to the vertex buffer.
     *
     * @param offset input offset within instance buffer.
     * @param buffer actual vertex buffer.
     * @param matrix transformation matrix of the object.
     * @param lights list of lights that can be used.
     * @return offset within the buffer, after the written data.
     */
    public int writeVertexInstanceData(
            int offset, ByteBuffer buffer, Matrix4fc matrix, List<Light> lights) {
        offset = ShaderSet.writeMatrix(offset, buffer, matrix);
        offset = Light.writeLights(offset, buffer, lights, getShaderSet().getLightCount());
        Scene.getActiveScene()
                .getSingleton(Camera.class)
                .getGameObject()
                .getTransform()
                .getPosition()
                .get(offset + CAM_OFFSET, buffer);
        mColour.get(offset + COL_OFFSET, buffer);
        mEmissionColour.get(offset + EMISSION_COL_OFFSET, buffer);
        ByteBuffer buf = (ByteBuffer) buffer.position(offset + ALPHA_CUTOFF_OFFSET);
        buf.putFloat(mAlphaCutoff);
        buf.putFloat(mMetallic);
        buf.putFloat(mRoughness);
        buf.putFloat(mNormal);
        return buf.position();
    }

    /**
     * Get the list of fragment textures used by the material.
     *
     * @return array of fragment textures. It will stay valid until a texture is updated. In
     *     addition, it is not a copy, so modifications to the array will propagate to all users.
     */
    @Override
    public SampledTexture[] getFragmentTextures() {
        if (mFragmentTextures == null) {
            mFragmentTextures =
                    Stream.of(mAlbedoMap, mNormalMap, mMetalnessRoughnessMap)
                            .filter(e -> e != null)
                            .toArray(SampledTexture[]::new);
        }

        return mFragmentTextures;
    }

    /**
     * Increase the reference count of the material (clone it).
     *
     * @return this, cast to reference coutned material.
     */
    @Override
    public IRefCountedMaterial incRefCount() {
        mRefCount++;
        return this;
    }

    /** Decrease the reference count, and free the material if it drops to 0. */
    @Override
    public void free() {
        if (--mRefCount < 0) {
            if (mAlbedoMap != null) {
                mAlbedoMap.free();
            }
            if (mNormalMap != null) {
                mNormalMap.free();
            }
            if (mMetalnessRoughnessMap != null) {
                mMetalnessRoughnessMap.free();
            }
        }
    }

    /**
     * Compare 2 sampled textures.
     *
     * @param a the first texture.
     * @param b the second texture.
     * @return {@code true}, if the textures are equal. {@code false} otherwise.
     */
    private boolean equalTexs(SampledTexture a, SampledTexture b) {
        if ((a == null) != (b == null)) {
            return false;
        }
        if (a != null) {
            return a.equals(b);
        }
        return true;
    }
}
