/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.vulkan.VK10.*;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.lwjgl.vulkan.*;

/**
 * Class abstracting a single render instance
 *
 * <p>This stores everything for a single instantiatable draw call
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Builder
class VulkanSampledTexture {
    @Getter private long mImageView;
    @Getter private long mSampler;
}
