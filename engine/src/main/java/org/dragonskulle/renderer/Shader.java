/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.LongBuffer;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.Resource;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.*;

/**
 * Allows to load shaders for GPU
 *
 * @author Aurimas Blažulionis
 *     <p>TODO: Turn this into a factory?
 */
public class Shader implements NativeResource {
    private VkDevice mDevice;

    @Accessors(prefix = "m")
    @Getter
    private long mModule;

    private static final Logger LOGGER = Logger.getLogger("render");

    public static Shader getShader(ShaderBuf shader, VkDevice device) {
        try (MemoryStack stack = stackPush()) {
            Shader ret = new Shader();
            ret.mDevice = device;

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(shader.getBuffer());

            LongBuffer pShaderModule = stack.longs(0);

            if (vkCreateShaderModule(device, createInfo, null, pShaderModule) == VK_SUCCESS) {
                ret.mModule = pShaderModule.get(0);
                return ret;
            } else {
                return null;
            }
        }
    }

    public static Shader getShader(String name, ShaderKind kind, VkDevice device) {
        LOGGER.fine("Get shader... " + name);

        try (Resource<ShaderBuf> resource = ShaderBuf.getResource(name, kind)) {
            return resource == null ? null : getShader(resource.get(), device);
        }
    }

    @Override
    public final void free() {
        vkDestroyShaderModule(mDevice, mModule, null);
    }
}