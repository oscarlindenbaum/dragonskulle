/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import static org.dragonskulle.utils.Env.envBool;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.Configuration.DEBUG;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.input.Bindings;
import org.dragonskulle.input.Input;
import org.dragonskulle.renderer.Renderer;
import org.dragonskulle.renderer.RendererSettings;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

/**
 * Handles all GLFW state, like input, and window, and stores renderer.
 *
 * @author Aurimas Blažulionis
 */
@Accessors(prefix = "m")
@Log
public class GLFWState implements NativeResource {
    private long mWindow;
    /** Window size in screen coordinates. */
    @Getter private final Vector2i mWindowSize = new Vector2i();

    @Getter private Renderer mRenderer;
    private boolean mFramebufferResized = false;

    private final String mAppName;
    private final Bindings mBindings;

    private boolean mFullscreen = false;
    private int mOldX = 0;
    private int mOldY = 0;
    private int mOldWidth = 1600;
    private int mOldHeight = 900;

    public static final boolean DEBUG_MODE = envBool("DEBUG_GLFW", false);

    public static final boolean LOAD_RENDERDOC = envBool("LOAD_RENDERDOC", false);

    /**
     * Entrypoint of the app instance.
     *
     * @param width initial window width
     * @param height initial window height
     * @param appName name of the app
     * @param bindings input bindings
     * @throws RuntimeException if initialization fails
     */
    public GLFWState(int width, int height, String appName, Bindings bindings)
            throws RuntimeException {
        DEBUG.set(DEBUG_MODE);

        if (LOAD_RENDERDOC) {
            System.loadLibrary("renderdoc");
        }

        mAppName = appName;
        mBindings = bindings;

        initialise(width, height, new RendererSettings());
    }

    /** Initialise the GLFW state. */
    private void initialise(int width, int height, RendererSettings settings) {
        initWindow(width, height, mAppName);
        mRenderer = new Renderer(mAppName, mWindow, settings);

        // Start detecting user input from the specified window, based on the bindings.
        Input.initialise(mWindow, mBindings);
    }

    /**
     * Set renderer graphics settings.
     *
     * <p>Depending on the settings changed, there may be a longer, or shorter time rendering
     * freeze.
     *
     * <p>If target GPU is being changed, make sure you call this method, not the one on the {@link
     * Renderer}, because it should be more stable!
     *
     * @param newSettings new graphics settings to choose
     */
    public void setRendererSettings(RendererSettings newSettings) {
        RendererSettings oldSettings = mRenderer.getSettings();

        if (newSettings.equals(oldSettings)) {
            return;
        }

        boolean changeGPU =
                newSettings.getTargetGPU() != null
                        && !mRenderer.getPhysicalDeviceName().equals(newSettings.getTargetGPU());

        if (changeGPU) {
            free();
            try {
                initialise(mOldWidth, mOldHeight, newSettings);
            } catch (Exception e) {
                log.warning("Failed to change settings, attempting to revert!");
                e.printStackTrace();
                free();
                initialise(mOldWidth, mOldHeight, oldSettings);
            }
            setFullscreen(mFullscreen);
        } else {
            mRenderer.setSettings(newSettings);
        }
    }

    /**
     * Set fullscreen mode for the current window
     *
     * @param fullscreen whether to make the window fullscreen or not. If set to true, the window
     *     will be made full screen, if set to false, the window will no longer be fullscreen. An
     *     attempt will be made to restore the window to the same position and size it was before
     *     entering full screen mode.
     */
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            long primaryMonitor = glfwGetPrimaryMonitor();
            GLFWVidMode mode = glfwGetVideoMode(primaryMonitor);

            int[] xpos = {0};
            int[] ypos = {0};
            glfwGetWindowPos(mWindow, xpos, ypos);
            mOldX = xpos[0];
            mOldY = ypos[0];

            int[] width = {0};
            int[] height = {0};
            glfwGetWindowSize(mWindow, width, height);
            mOldWidth = width[0];
            mOldHeight = height[0];

            glfwSetWindowMonitor(
                    mWindow, primaryMonitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
            mFullscreen = true;
        } else if (mFullscreen) {
            glfwSetWindowMonitor(mWindow, 0, mOldX, mOldY, mOldWidth, mOldHeight, GLFW_DONT_CARE);
            mFullscreen = false;
        }
    }

    /**
     * Process GLFW events and check if the app should close.
     *
     * @return {@code true} if the app should stay running, {@code false} if the app should close.
     */
    public boolean processEvents() {
        Input.beforePoll();
        glfwPollEvents();

        if (mFramebufferResized) {
            mFramebufferResized = false;
            mRenderer.onResize();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);
            glfwGetWindowSize(mWindow, width, height);
            mWindowSize.set(width.get(0), height.get(0));
        }

        return !glfwWindowShouldClose(mWindow);
    }

    @Override
    public void free() {
        mRenderer.free();
        glfwDestroyWindow(mWindow);
    }

    /** Creates a GLFW window. */
    private void initWindow(int width, int height, String appName) {
        log.info("Initialize GLFW window");

        if (!glfwInit()) {
            throw new RuntimeException("Cannot initialize GLFW");
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        mWindow = glfwCreateWindow(width, height, appName, NULL, NULL);

        if (mWindow == NULL) {
            throw new RuntimeException("Cannot create window");
        }

        glfwSetFramebufferSizeCallback(mWindow, this::onFramebufferResize);
    }

    private void onFramebufferResize(long window, int width, int height) {
        mFramebufferResized = true;
    }
}
