/* (C) 2021 DragonSkulle */
package org.dragonskulle.renderer;

import static java.util.stream.Collectors.toSet;
import static org.dragonskulle.utils.Env.*;
import static org.dragonskulle.utils.Env.envString;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.Camera;
import org.dragonskulle.components.Renderable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

/**
 * Vulkan renderer
 *
 * @author Aurimas Blažulionis
 *     <p>This renderer allows to draw {@code Renderable} objects on screen. Application needs to
 *     call {@code onResized} when its window gets resized.
 *     <p>This renderer was originally based on<a href="https://vulkan-tutorial.com/">Vulkan
 *     Tutorial</a>, and was later rewritten with a much more manageable design.
 */
@Accessors(prefix = "m")
@Getter(AccessLevel.PACKAGE)
public class Renderer implements NativeResource {

    private long mWindow;

    private VkInstance mInstance;
    private long mDebugMessenger;
    private long mSurface;

    private PhysicalDevice mPhysicalDevice;

    private VkDevice mDevice;
    private VkQueue mGraphicsQueue;
    private VkQueue mPresentQueue;

    private long mCommandPool;

    private TextureSamplerFactory mSamplerFactory;
    private VulkanSampledTextureFactory mTextureFactory;
    private TextureSetLayoutFactory mTextureSetLayoutFactory;
    private TextureSetFactory mTextureSetFactory;
    private VertexConstants mVertexConstants = new VertexConstants();

    private VkSurfaceFormatKHR mSurfaceFormat;
    private VkExtent2D mExtent;
    private long mSwapchain;
    private long mRenderPass;

    private ImageContext[] mImageContexts;
    private FrameContext[] mFrameContexts;

    private VulkanImage mDepthImage;
    private long mDepthImageView;

    private int mFrameCounter = 0;

    private static final List<String> WANTED_VALIDATION_LAYERS_LIST =
            Arrays.asList("VK_LAYER_KHRONOS_validation");
    private static final Set<String> DEVICE_EXTENSIONS =
            Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(toSet());

    private static final Logger LOGGER = Logger.getLogger("render");
    static final boolean DEBUG_MODE = envBool("DEBUG_RENDERER", false);
    private static final String TARGET_GPU = envString("TARGET_GPU", null);

    private static final int INSTANCE_BUFFER_SIZE = envInt("INSTANCE_BUFFER_SIZE", 8);

    private static final long UINT64_MAX = -1L;
    private static final int FRAMES_IN_FLIGHT = 4;

    /** Synchronization objects for when multiple frames are rendered at a time */
    private class FrameContext {
        public long imageAvailableSemaphore;
        public long renderFinishedSemaphore;
        public long inFlightFence;
    }

    /** All state for a single frame */
    private static class ImageContext {
        public VkCommandBuffer commandBuffer;
        public long inFlightFence;
        public long framebuffer;
        public int imageIndex;

        public VulkanBuffer instanceBuffer;

        private long mImageView;
        private VkDevice mDevice;

        /** Create a image context */
        private ImageContext(
                Renderer renderer, VulkanImage image, int imageIndex, long commandBuffer) {
            this.commandBuffer = new VkCommandBuffer(commandBuffer, renderer.mDevice);
            this.mImageView = image.createImageView();
            this.framebuffer = createFramebuffer(renderer);
            this.imageIndex = imageIndex;

            this.mDevice = renderer.mDevice;

            instanceBuffer = renderer.createInstanceBuffer(INSTANCE_BUFFER_SIZE * 1024 * 1024);
        }

        /** Create a framebuffer from image view */
        private long createFramebuffer(Renderer renderer) {
            try (MemoryStack stack = stackPush()) {
                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.callocStack(stack);
                createInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);

                createInfo.renderPass(renderer.mRenderPass);
                createInfo.width(renderer.mExtent.width());
                createInfo.height(renderer.mExtent.height());
                createInfo.layers(1);

                LongBuffer attachment = stack.longs(mImageView, renderer.mDepthImageView);
                LongBuffer framebuffer = stack.longs(0);

                createInfo.pAttachments(attachment);

                int result = vkCreateFramebuffer(renderer.mDevice, createInfo, null, framebuffer);

                if (result != VK_SUCCESS) {
                    throw new RuntimeException(
                            String.format(
                                    "Failed to create framebuffer for %x! Error: %x",
                                    mImageView, -result));
                }

                return framebuffer.get(0);
            }
        }

        private void free() {
            instanceBuffer.free();
            vkDestroyFramebuffer(mDevice, framebuffer, null);
            vkDestroyImageView(mDevice, mImageView, null);
        }
    }

    private VulkanMeshBuffer mCurrentMeshBuffer;
    private Map<Integer, VulkanMeshBuffer> mDiscardedMeshBuffers = new HashMap<>();
    private Map<DrawCallState.HashKey, DrawCallState> mDrawInstances = new HashMap<>();

    /**
     * Create a renderer
     *
     * <p>This constructor will create a Vulkan renderer instance, and set everything up so that
     * {@code render} method can be called.
     *
     * @param appName name of the rendered application.
     * @param window handle to GLFW window.
     * @throws RuntimeException when initialization fails.
     */
    public Renderer(String appName, long window) throws RuntimeException {
        LOGGER.info("Initialize renderer");
        this.mWindow = window;
        mInstance = createInstance(appName);
        if (DEBUG_MODE) mDebugMessenger = createDebugLogger();
        mSurface = createSurface();
        mPhysicalDevice = pickPhysicalDevice();
        mDevice = createLogicalDevice();
        mGraphicsQueue = createGraphicsQueue();
        mPresentQueue = createPresentQueue();
        mCommandPool = createCommandPool();
        mSamplerFactory = new TextureSamplerFactory(mDevice, mPhysicalDevice);
        mTextureFactory =
                new VulkanSampledTextureFactory(
                        mDevice, mPhysicalDevice, mCommandPool, mGraphicsQueue, mSamplerFactory);
        mTextureSetLayoutFactory = new TextureSetLayoutFactory(mDevice);
        mCurrentMeshBuffer = new VulkanMeshBuffer(mDevice, mPhysicalDevice);
        createSwapchainObjects();
        mFrameContexts = createFrameContexts(FRAMES_IN_FLIGHT);
    }

    /**
     * Render a frame
     *
     * <p>This method will take a list of renderable objects, and render them from the camera point
     * of view.
     *
     * @param camera object from where the renderer should render
     * @param objects list of objects that should be rendered
     */
    public void render(Camera camera, List<Renderable> objects) {
        if (mImageContexts == null) recreateSwapchain();

        camera.updateAspectRatio(mExtent.width(), mExtent.height());

        try (MemoryStack stack = stackPush()) {
            FrameContext ctx = mFrameContexts[mFrameCounter];
            mFrameCounter = (mFrameCounter + 1) % FRAMES_IN_FLIGHT;

            vkWaitForFences(mDevice, ctx.inFlightFence, true, UINT64_MAX);

            IntBuffer pImageIndex = stack.ints(0);
            int res =
                    vkAcquireNextImageKHR(
                            mDevice,
                            mSwapchain,
                            UINT64_MAX,
                            ctx.imageAvailableSemaphore,
                            VK_NULL_HANDLE,
                            pImageIndex);
            final int imageIndex = pImageIndex.get(0);
            final ImageContext image = mImageContexts[imageIndex];

            if (res == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapchain();
                return;
            }

            if (image.inFlightFence != 0)
                vkWaitForFences(mDevice, image.inFlightFence, true, UINT64_MAX);

            image.inFlightFence = ctx.inFlightFence;

            VulkanMeshBuffer discardedBuffer = mDiscardedMeshBuffers.remove(imageIndex);

            if (discardedBuffer != null) discardedBuffer.free();

            updateInstanceBuffer(image, objects);
            recordCommandBuffer(image, camera);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            LongBuffer waitSemaphores = stack.longs(ctx.imageAvailableSemaphore);
            IntBuffer waitStages = stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            PointerBuffer commandBuffer = stack.pointers(image.commandBuffer);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(waitSemaphores);
            submitInfo.pWaitDstStageMask(waitStages);
            submitInfo.pCommandBuffers(commandBuffer);

            LongBuffer signalSemaphores = stack.longs(ctx.renderFinishedSemaphore);
            submitInfo.pSignalSemaphores(signalSemaphores);

            vkResetFences(mDevice, ctx.inFlightFence);

            res = vkQueueSubmit(mGraphicsQueue, submitInfo, ctx.inFlightFence);

            if (res != VK_SUCCESS) {
                vkResetFences(mDevice, ctx.inFlightFence);
                throw new RuntimeException(
                        String.format("Failed to submit draw command buffer! Ret: %x", -res));
            }

            LongBuffer swapchains = stack.longs(mSwapchain);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(signalSemaphores);
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(swapchains);
            presentInfo.pImageIndices(pImageIndex);

            res = vkQueuePresentKHR(mPresentQueue, presentInfo);

            if (res == VK_ERROR_OUT_OF_DATE_KHR || res == VK_SUBOPTIMAL_KHR) {
                recreateSwapchain();
            } else if (res != VK_SUCCESS) {
                throw new RuntimeException(String.format("Failed to present image! Ret: %x", -res));
            }
        }
    }

    /**
     * Inform the renderer about window being resized
     *
     * <p>This method needs to be called by the app every time the window gets resized, so that the
     * renderer can change its render resolution.
     */
    public void onResize() {
        recreateSwapchain();
    }

    /**
     * Free all renderer resources
     *
     * <p>Call this method to shutdown the renderer and free all its resources.
     */
    @Override
    public void free() {
        vkDeviceWaitIdle(mDevice);
        for (FrameContext frame : mFrameContexts) {
            vkDestroySemaphore(mDevice, frame.renderFinishedSemaphore, null);
            vkDestroySemaphore(mDevice, frame.imageAvailableSemaphore, null);
            vkDestroyFence(mDevice, frame.inFlightFence, null);
        }
        cleanupSwapchain();
        mCurrentMeshBuffer.free();
        mTextureSetLayoutFactory.free();
        mTextureFactory.free();
        mSamplerFactory.free();
        vkDestroyCommandPool(mDevice, mCommandPool, null);
        vkDestroyDevice(mDevice, null);
        destroyDebugMessanger();
        vkDestroySurfaceKHR(mInstance, mSurface, null);
        vkDestroyInstance(mInstance, null);
        glfwDestroyWindow(mWindow);
        glfwTerminate();
    }

    /// Internal code

    /** Recreate swapchain when it becomes invalid */
    private void recreateSwapchain() {
        if (mImageContexts != null) {
            vkQueueWaitIdle(mPresentQueue);
            vkQueueWaitIdle(mGraphicsQueue);
            vkDeviceWaitIdle(mDevice);

            cleanupSwapchain();
        }

        try (MemoryStack stack = stackPush()) {
            IntBuffer x = stack.ints(0);
            IntBuffer y = stack.ints(0);
            glfwGetFramebufferSize(mWindow, x, y);
            LOGGER.finer(String.format("%d %d", x.get(0), y.get(0)));
            if (x.get(0) == 0 || y.get(0) == 0) return;
        }

        mPhysicalDevice.onRecreateSwapchain(mSurface);

        createSwapchainObjects();
    }

    /** Cleanup swapchain resources */
    private void cleanupSwapchain() {
        Arrays.stream(mImageContexts).forEach(ImageContext::free);

        try (MemoryStack stack = stackPush()) {
            vkFreeCommandBuffers(
                    mDevice,
                    mCommandPool,
                    toPointerBuffer(
                            Arrays.stream(mImageContexts)
                                    .map(d -> d.commandBuffer)
                                    .toArray(VkCommandBuffer[]::new),
                            stack));
        }

        mImageContexts = null;

        for (DrawCallState state : mDrawInstances.values()) state.free();
        mDrawInstances.clear();

        for (VulkanMeshBuffer meshBuffer : mDiscardedMeshBuffers.values()) meshBuffer.free();
        mDiscardedMeshBuffers.clear();

        mTextureSetFactory.free();
        mTextureSetFactory = null;

        vkDestroyRenderPass(mDevice, mRenderPass, null);

        vkDestroyImageView(mDevice, mDepthImageView, null);
        mDepthImageView = 0;

        mDepthImage.free();
        mDepthImage = null;

        vkDestroySwapchainKHR(mDevice, mSwapchain, null);
    }

    /// Internal setup code

    /**
     * Create swapchain objects
     *
     * <p>Create all objects that depend on the swapchain. Unlike initial setup, this method will be
     * called multiple times in situations like window resizes.
     */
    private void createSwapchainObjects() {
        mSurfaceFormat = mPhysicalDevice.getSwapchainSupport().chooseSurfaceFormat();
        mExtent = mPhysicalDevice.getSwapchainSupport().chooseExtent(mWindow);
        mSwapchain = createSwapchain();
        mRenderPass = createRenderPass();
        mDepthImage = createDepthImage();
        mDepthImageView = mDepthImage.createImageView();
        int imageCount = getImageCount();
        mImageContexts = createImageContexts(imageCount);
        mTextureSetFactory = new TextureSetFactory(mDevice, mTextureSetLayoutFactory, imageCount);
    }

    /// Instance setup

    /**
     * Create a Vulkan instance
     *
     * <p>Vulkan instance is needed for the duration of the renderer. If debug mode is on, the
     * instance will also enable debug validation layers, which allow to track down issues.
     */
    private VkInstance createInstance(String appName) {
        LOGGER.fine("Create instance");

        try (MemoryStack stack = stackPush()) {
            // Prepare basic Vulkan App information
            VkApplicationInfo appInfo = VkApplicationInfo.callocStack(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(appName));
            appInfo.applicationVersion(VK_MAKE_VERSION(0, 0, 1));
            appInfo.pEngineName(stack.UTF8Safe("DragonSkulle Engine"));
            appInfo.engineVersion(VK_MAKE_VERSION(0, 0, 1));
            appInfo.apiVersion(VK_API_VERSION_1_0);

            // Prepare a Vulkan instance information
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // set required GLFW extensions
            createInfo.ppEnabledExtensionNames(getExtensions(createInfo, stack));

            // use validation layers if enabled, and setup debugging
            if (DEBUG_MODE) {
                setupDebugValidationLayers(createInfo, stack);
                // setup logging for instance creation
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = createDebugLoggingInfo(stack);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer instancePtr = stack.mallocPointer(1);

            int result = vkCreateInstance(createInfo, null, instancePtr);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK instance. Error: %x", -result));
            }

            return new VkInstance(instancePtr.get(0), createInfo);
        }
    }

    /** Returns required extensions for the VK context */
    private PointerBuffer getExtensions(
            VkInstanceCreateInfo createInfoMemoryStack, MemoryStack stack) {
        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if (DEBUG_MODE) {
            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        } else {
            return glfwExtensions;
        }
    }

    /**
     * Returns validation layers used for debugging
     *
     * <p>Throws if the layers were not available, createInfo gets bound to the data on the stack
     * frame. Do not pop the stack before using up createInfo!!!
     */
    private void setupDebugValidationLayers(VkInstanceCreateInfo createInfo, MemoryStack stack) {
        LOGGER.fine("Setup VK validation layers");

        Set<String> wantedSet = new HashSet<>(WANTED_VALIDATION_LAYERS_LIST);

        VkLayerProperties.Buffer properties = getInstanceLayerProperties(stack);

        boolean containsAll =
                wantedSet.isEmpty()
                        || properties.stream()
                                .map(VkLayerProperties::layerNameString)
                                .filter(wantedSet::remove)
                                .anyMatch(__ -> wantedSet.isEmpty());

        if (containsAll) {
            createInfo.ppEnabledLayerNames(toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));
        } else {
            throw new RuntimeException("Some VK validation layers were not found!");
        }
    }

    /** Utility for converting collection to pointer buffer */
    private PointerBuffer toPointerBuffer(Collection<String> collection, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for converting a collection of pointer types to pointer buffer */
    private <T extends Pointer> PointerBuffer toPointerBuffer(T[] array, MemoryStack stack) {
        PointerBuffer buffer = stack.mallocPointer(array.length);
        Arrays.stream(array).forEach(buffer::put);
        return buffer.rewind();
    }

    /** Utility for retrieving instance VkLayerProperties list */
    private VkLayerProperties.Buffer getInstanceLayerProperties(MemoryStack stack) {
        IntBuffer propertyCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(propertyCount, null);
        VkLayerProperties.Buffer properties =
                VkLayerProperties.mallocStack(propertyCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(propertyCount, properties);
        return properties;
    }

    /** Creates default debug messenger info for logging */
    private VkDebugUtilsMessengerCreateInfoEXT createDebugLoggingInfo(MemoryStack stack) {
        VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo =
                VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);

        // Initialize debug callback parameters
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(
                VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT);
        debugCreateInfo.messageType(
                VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Renderer::debugCallback);

        return debugCreateInfo;
    }

    /// Setup debug logging

    /** VK logging entrypoint */
    private static int debugCallback(
            int messageSeverity, int messageType, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData =
                VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        Level level = Level.FINE;

        if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) {
            level = Level.SEVERE;
        } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) {
            level = Level.WARNING;
        } else if (messageSeverity == VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) {
            level = Level.INFO;
        }

        LOGGER.log(level, callbackData.pMessageString());

        return VK_FALSE;
    }

    /** Initializes debugMessenger to receive VK log messages */
    private long createDebugLogger() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pDebugMessenger = stack.longs(0);
            if (vkCreateDebugUtilsMessengerEXT(
                            mInstance, createDebugLoggingInfo(stack), null, pDebugMessenger)
                    != VK_SUCCESS) {
                throw new RuntimeException("Failed to initialize debug messenger");
            }
            return pDebugMessenger.get();
        }
    }

    /// Setup window surface

    /**
     * Create a window surface
     *
     * <p>This method uses window to get its surface that the renderer will draw to
     */
    private long createSurface() {
        LOGGER.fine("Create surface");

        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.callocLong(1);
            int result = glfwCreateWindowSurface(mInstance, mWindow, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create windows surface! %x", -result));
            }
            return pSurface.get(0);
        }
    }

    /// Physical device setup

    /** Sets up one physical device for use */
    private PhysicalDevice pickPhysicalDevice() {
        LOGGER.fine("Pick physical device");
        PhysicalDevice physicalDevice =
                PhysicalDevice.pickPhysicalDevice(
                        mInstance, mSurface, TARGET_GPU, DEVICE_EXTENSIONS);
        if (physicalDevice == null) {
            throw new RuntimeException("Failed to find compatible GPU!");
        }
        LOGGER.fine(String.format("Picked GPU: %s", physicalDevice.getDeviceName()));
        return physicalDevice;
    }

    /// Logical device setup

    /** Creates a logical device with required features */
    private VkDevice createLogicalDevice() {
        LOGGER.fine("Create logical device");

        try (MemoryStack stack = stackPush()) {
            FloatBuffer queuePriority = stack.floats(1.0f);

            int[] families = mPhysicalDevice.getIndices().uniqueFamilies();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfo =
                    VkDeviceQueueCreateInfo.callocStack(families.length, stack);

            IntStream.range(0, families.length)
                    .forEach(
                            i -> {
                                queueCreateInfo
                                        .get(i)
                                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                                queueCreateInfo.get(i).queueFamilyIndex(families[i]);
                                queueCreateInfo.get(i).pQueuePriorities(queuePriority);
                            });

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
            deviceFeatures.samplerAnisotropy(mPhysicalDevice.getFeatureSupport().anisotropyEnable);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfo);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(toPointerBuffer(DEVICE_EXTENSIONS, stack));

            if (DEBUG_MODE)
                createInfo.ppEnabledLayerNames(
                        toPointerBuffer(WANTED_VALIDATION_LAYERS_LIST, stack));

            PointerBuffer pDevice = stack.callocPointer(1);

            int result = vkCreateDevice(mPhysicalDevice.getDevice(), createInfo, null, pDevice);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create VK logical device! Err: %x", -result));
            }

            VkDevice device = new VkDevice(pDevice.get(0), mPhysicalDevice.getDevice(), createInfo);

            return device;
        }
    }

    /// Queue setup

    /**
     * Create a graphics queue
     *
     * <p>This queue is used to submit graphics commands every frame
     */
    private VkQueue createGraphicsQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.getIndices().graphicsFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), mDevice);
        }
    }

    /**
     * Create a presentation queue
     *
     * <p>This queue is used to display rendered frames on the screen
     */
    private VkQueue createPresentQueue() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pQueue = stack.callocPointer(1);
            vkGetDeviceQueue(mDevice, mPhysicalDevice.getIndices().presentFamily, 0, pQueue);
            return new VkQueue(pQueue.get(0), mDevice);
        }
    }

    /// Command pool setup

    /**
     * Create a command pool
     *
     * <p>This method creates a command pool which is used for creating command buffers.
     */
    private long createCommandPool() {
        LOGGER.fine("Create command pool");

        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(mPhysicalDevice.getIndices().graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.longs(0);

            int result = vkCreateCommandPool(mDevice, poolInfo, null, pCommandPool);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command pool! Err: %x", -result));
            }

            return pCommandPool.get(0);
        }
    }

    /// Swapchain setup

    /** Sets up the swapchain required for rendering */
    private long createSwapchain() {
        LOGGER.fine("Setup swapchain");

        try (MemoryStack stack = stackPush()) {
            int presentMode = mPhysicalDevice.getSwapchainSupport().choosePresentMode();
            int imageCount = mPhysicalDevice.getSwapchainSupport().chooseImageCount();

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(mSurface);
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(mSurfaceFormat.format());
            createInfo.imageColorSpace(mSurfaceFormat.colorSpace());
            createInfo.imageExtent(mExtent);
            createInfo.imageArrayLayers(1);
            // Render directly. For post-processing,
            // we may need VK_IMAGE_USAGE_TRANSFER_DST_BIT
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            // If we have separate queues, use concurrent mode which is easier to work with,
            // although slightly less efficient.
            if (mGraphicsQueue.address() != mPresentQueue.address()) {
                IntBuffer indices =
                        stack.ints(
                                mPhysicalDevice.getIndices().graphicsFamily,
                                mPhysicalDevice.getIndices().presentFamily);
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(indices);
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(
                    mPhysicalDevice.getSwapchainSupport().capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);

            LongBuffer pSwapchain = stack.longs(0);

            int result = vkCreateSwapchainKHR(mDevice, createInfo, null, pSwapchain);

            if (result != VK_SUCCESS)
                throw new RuntimeException(
                        String.format("Failed to create swapchain! Error: %x", -result));

            return pSwapchain.get(0);
        }
    }

    /// Per swapchain image setup

    /** Get the number of images swapchain was created with */
    private int getImageCount() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageCount = stack.ints(0);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, null);

            return pImageCount.get(0);
        }
    }

    /** Create a context for each swapchain image */
    private ImageContext[] createImageContexts(int imageCount) {
        try (MemoryStack stack = stackPush()) {
            // Get swapchain images
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount);

            IntBuffer pImageCount = stack.ints(imageCount);

            vkGetSwapchainImagesKHR(mDevice, mSwapchain, pImageCount, pSwapchainImages);

            // Allocate command buffers

            VkCommandBufferAllocateInfo cmdAllocInfo =
                    VkCommandBufferAllocateInfo.callocStack(stack);
            cmdAllocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            cmdAllocInfo.commandPool(mCommandPool);
            cmdAllocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            cmdAllocInfo.commandBufferCount(imageCount);

            PointerBuffer buffers = stack.mallocPointer(cmdAllocInfo.commandBufferCount());

            int result = vkAllocateCommandBuffers(mDevice, cmdAllocInfo, buffers);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create command buffers! Err: %x", -result));
            }

            return IntStream.range(0, imageCount)
                    .mapToObj(
                            i ->
                                    new ImageContext(
                                            this,
                                            new VulkanImage(
                                                    mDevice,
                                                    mSurfaceFormat.format(),
                                                    pSwapchainImages.get(i)),
                                            i,
                                            buffers.get(i)))
                    .toArray(ImageContext[]::new);
        }
    }

    /// Render pass setup

    /**
     * Create a render pass
     *
     * <p>This method will describe how a single render pass should behave.
     *
     * <p>TODO: move to material system? Move to render pass manager?
     */
    private long createRenderPass() {
        LOGGER.fine("Create render pass");

        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachments =
                    VkAttachmentDescription.callocStack(2, stack);
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(mSurfaceFormat.format());
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            // We don't use stencils yet
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            // We present the image after rendering, and don't care what it was,
            // since we clear it anyways.
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorAttachmentRef =
                    VkAttachmentReference.callocStack(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(mPhysicalDevice.findDepthFormat());
            depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = VkAttachmentReference.callocStack(stack);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);

            // Make render passes wait for COLOR_ATTACHMENT_OUTPUT stage
            VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(
                    VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.dstAccessMask(
                    VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.longs(0);

            int result = vkCreateRenderPass(mDevice, renderPassInfo, null, pRenderPass);

            if (result != VK_SUCCESS) {
                throw new RuntimeException(
                        String.format("Failed to create render pass! Err: %x", -result));
            }

            return pRenderPass.get(0);
        }
    }

    /// Depth texture setup

    private VulkanImage createDepthImage() {
        VkCommandBuffer tmpCommandBuffer = beginSingleUseCommandBuffer();
        VulkanImage depthImage =
                VulkanImage.createDepthImage(
                        tmpCommandBuffer,
                        mDevice,
                        mPhysicalDevice,
                        mExtent.width(),
                        mExtent.height());
        endSingleUseCommandBuffer(tmpCommandBuffer);
        return depthImage;
    }

    /// Vertex and index buffers

    /**
     * Create a instance buffer
     *
     * <p>As the name implies, this buffer holds base per-instance data
     */
    private VulkanBuffer createInstanceBuffer(int sizeOfBuffer) {
        LOGGER.fine("Create instance buffer");

        try (MemoryStack stack = stackPush()) {
            return new VulkanBuffer(
                    mDevice,
                    mPhysicalDevice,
                    sizeOfBuffer,
                    // VK_BUFFER_USAGE_TRANSFER_DST_BIVK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |T
                    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }
    }

    /**
     * Create a single use command buffer.
     *
     * <p>This command buffer can be flushed with {@code endSingleUseCommandBuffer}
     */
    static VkCommandBuffer beginSingleUseCommandBuffer(VkDevice device, long commandPool) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(commandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            return commandBuffer;
        }
    }

    private VkCommandBuffer beginSingleUseCommandBuffer() {
        return beginSingleUseCommandBuffer(mDevice, mCommandPool);
    }

    /** Ends and frees the single use command buffer */
    static void endSingleUseCommandBuffer(
            VkCommandBuffer commandBuffer,
            VkDevice device,
            VkQueue graphicsQueue,
            long commandPool) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            PointerBuffer pCommandBuffer = stack.pointers(commandBuffer.address());

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);

            vkQueueSubmit(graphicsQueue, submitInfo, NULL);
            vkQueueWaitIdle(graphicsQueue);

            vkFreeCommandBuffers(device, commandPool, pCommandBuffer);
        }
    }

    private void endSingleUseCommandBuffer(VkCommandBuffer commandBuffer) {
        endSingleUseCommandBuffer(commandBuffer, mDevice, mGraphicsQueue, mCommandPool);
    }

    /// Frame Context setup

    private FrameContext[] createFrameContexts(int framesInFlight) {
        LOGGER.fine("Setup sync objects");

        try (MemoryStack stack = stackPush()) {
            FrameContext[] frames = new FrameContext[framesInFlight];

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            IntStream.range(0, framesInFlight)
                    .forEach(
                            i -> {
                                FrameContext ctx = new FrameContext();

                                LongBuffer pSem1 = stack.longs(0);
                                LongBuffer pSem2 = stack.longs(0);
                                LongBuffer pFence = stack.longs(0);

                                int res1 = vkCreateSemaphore(mDevice, semaphoreInfo, null, pSem1);
                                int res2 = vkCreateSemaphore(mDevice, semaphoreInfo, null, pSem2);
                                int res3 = vkCreateFence(mDevice, fenceInfo, null, pFence);

                                if (res1 != VK_SUCCESS
                                        || res2 != VK_SUCCESS
                                        || res3 != VK_SUCCESS) {
                                    throw new RuntimeException(
                                            String.format(
                                                    "Failed to create semaphores! Err: %x %x",
                                                    -res1, -res2, -res2));
                                }

                                ctx.imageAvailableSemaphore = pSem1.get(0);
                                ctx.renderFinishedSemaphore = pSem2.get(0);
                                ctx.inFlightFence = pFence.get(0);

                                frames[i] = ctx;
                            });
            return frames;
        }
    }

    /// Record command buffer to temporary object

    void updateInstanceBuffer(ImageContext ctx, List<Renderable> renderables) {
        for (DrawCallState state : mDrawInstances.values()) state.startDrawData(mCurrentMeshBuffer);

        DrawCallState.HashKey tmpKey = new DrawCallState.HashKey();

        for (Renderable renderable : renderables) {
            tmpKey.setRenderable(renderable);
            DrawCallState state = mDrawInstances.get(tmpKey);
            if (state == null) {
                // We don't want to put the temp key in, becuase it changes values
                DrawCallState.HashKey newKey = new DrawCallState.HashKey(renderable);
                state = new DrawCallState(this, mImageContexts.length, newKey);
                state.startDrawData(mCurrentMeshBuffer);
                mDrawInstances.put(newKey, state);
            }
            state.addObject(renderable);
        }

        int instanceBufferSize = 0;

        for (DrawCallState state : mDrawInstances.values())
            instanceBufferSize = state.setInstanceBufferOffset(instanceBufferSize);

        // TODO: Resize instance buffer
        if (instanceBufferSize > INSTANCE_BUFFER_SIZE * 1024 * 1024)
            throw new RuntimeException("Would overflow instance buffer! Auri, Implement resizing!");

        if (mCurrentMeshBuffer.isDirty()) {
            mDiscardedMeshBuffers.put(ctx.imageIndex, mCurrentMeshBuffer);
            mCurrentMeshBuffer = mCurrentMeshBuffer.commitChanges(mGraphicsQueue, mCommandPool);
        }

        try (MemoryStack stack = stackPush()) {
            PointerBuffer pData = stack.pointers(0);
            vkMapMemory(mDevice, ctx.instanceBuffer.memory, 0, instanceBufferSize, 0, pData);

            ByteBuffer byteBuffer = pData.getByteBuffer(instanceBufferSize);

            for (DrawCallState state : mDrawInstances.values()) {
                state.updateInstanceBuffer(byteBuffer);
                state.endDrawData(ctx.imageIndex);
            }

            vkUnmapMemory(mDevice, ctx.instanceBuffer.memory);
        }
    }

    void recordCommandBuffer(ImageContext ctx, Camera camera) {

        mVertexConstants.proj = camera.getProj();
        mVertexConstants.view = camera.getView();

        try (MemoryStack stack = stackPush()) {
            // Record the command buffers
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(mRenderPass);

            renderPassInfo.renderArea().offset().clear();
            renderPassInfo.renderArea().extent(mExtent);

            VkClearValue.Buffer clearColor = VkClearValue.callocStack(2, stack);

            VkClearDepthStencilValue depthValue = clearColor.get(1).depthStencil();
            depthValue.depth(1.0f);
            depthValue.stencil(0);

            renderPassInfo.pClearValues(clearColor);

            int res = vkBeginCommandBuffer(ctx.commandBuffer, beginInfo);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to begin recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }

            renderPassInfo.framebuffer(ctx.framebuffer);

            // This is the beginning :)
            vkCmdBeginRenderPass(ctx.commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            ByteBuffer pConstants = stack.calloc(VertexConstants.SIZEOF);
            mVertexConstants.copyTo(pConstants);

            LongBuffer vertexBuffers =
                    stack.longs(mCurrentMeshBuffer.getVertexBuffer(), ctx.instanceBuffer.buffer);

            for (DrawCallState callState : mDrawInstances.values()) {
                VulkanPipeline pipeline = callState.getPipeline();
                VulkanMeshBuffer.MeshDescriptor meshDescriptor = callState.getMeshDescriptor();

                vkCmdBindPipeline(
                        ctx.commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);

                vkCmdPushConstants(
                        ctx.commandBuffer,
                        pipeline.layout,
                        VK_SHADER_STAGE_VERTEX_BIT,
                        0,
                        pConstants);

                for (DrawCallState.DrawData drawData : callState.getDrawData()) {
                    try (MemoryStack innerStack = stackPush()) {

                        LongBuffer offsets =
                                innerStack.longs(
                                        meshDescriptor.getVertexOffset(),
                                        drawData.getInstanceBufferOffset());
                        vkCmdBindVertexBuffers(ctx.commandBuffer, 0, vertexBuffers, offsets);
                        vkCmdBindIndexBuffer(
                                ctx.commandBuffer,
                                mCurrentMeshBuffer.getIndexBuffer(),
                                meshDescriptor.getIndexOffset(),
                                VK_INDEX_TYPE_UINT32);

                        long[] descriptorSets = drawData.getDescriptorSets();

                        if (descriptorSets != null && descriptorSets.length > 0) {
                            LongBuffer pDescriptorSets = innerStack.longs(descriptorSets);

                            vkCmdBindDescriptorSets(
                                    ctx.commandBuffer,
                                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                                    pipeline.layout,
                                    0,
                                    pDescriptorSets,
                                    null);
                        }

                        vkCmdDrawIndexed(
                                ctx.commandBuffer,
                                meshDescriptor.getIndexCount(),
                                drawData.getObjects().size(),
                                0,
                                0,
                                0);
                    }
                }
            }

            vkCmdEndRenderPass(ctx.commandBuffer);

            // And this is the end
            res = vkEndCommandBuffer(ctx.commandBuffer);

            if (res != VK_SUCCESS) {
                String format =
                        String.format("Failed to end recording command buffer! Err: %x", res);
                throw new RuntimeException(format);
            }
        }
    }

    /// Setup texture

    /// Cleanup code

    /** Destroys debugMessenger if exists */
    private void destroyDebugMessanger() {
        if (mDebugMessenger == 0) return;
        vkDestroyDebugUtilsMessengerEXT(mInstance, mDebugMessenger, null);
    }
}