/* (C) 2021 DragonSkulle */
package org.dragonskulle.game;

import java.util.Scanner;
import lombok.extern.java.Log;
import org.dragonskulle.assets.GLTF;
import org.dragonskulle.audio.AudioManager;
import org.dragonskulle.audio.components.AudioListener;
import org.dragonskulle.audio.components.AudioSource;
import org.dragonskulle.components.*;
import org.dragonskulle.core.Engine;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Resource;
import org.dragonskulle.core.Scene;
import org.dragonskulle.core.TemplateManager;
import org.dragonskulle.game.camera.KeyboardMovement;
import org.dragonskulle.game.camera.ScrollTranslate;
import org.dragonskulle.game.camera.TargetMovement;
import org.dragonskulle.game.camera.ZoomTilt;
import org.dragonskulle.game.input.GameBindings;
import org.dragonskulle.game.map.FogOfWar;
import org.dragonskulle.game.map.HexagonMap;
import org.dragonskulle.game.map.MapEffects;
import org.dragonskulle.game.player.HumanPlayer;
import org.dragonskulle.network.ServerClient;
import org.dragonskulle.network.components.NetworkManager;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.renderer.components.*;
import org.dragonskulle.ui.*;
import org.joml.*;
import org.lwjgl.system.NativeResource;

@Log
public class App implements NativeResource {

    private static final int BGM_ID = AudioManager.getInstance().loadSound("game_background.wav");
    private static final int BGM2_ID =
            AudioManager.getInstance().loadSound("country_background_short.wav");
    private static final int BUTTON_SFX_ID = AudioManager.getInstance().loadSound("button-10.wav");

    private static String sIP = "127.0.0.1";
    private static int sPort = 7000;
    private static boolean sReload = false;

    private final Resource<GLTF> mMainMenuGLTF = GLTF.getResource("main_menu");
    private final Resource<GLTF> mNetworkTemplatesGLTF = GLTF.getResource("network_templates");

    private static void addDebugUI(Scene scene) {
        GameObject debugUI =
                new GameObject(
                        "debugUI",
                        new TransformUI(true),
                        (handle) -> {
                            handle.getTransform(TransformUI.class)
                                    .setParentAnchor(0.0f, 1f, 0.5f, 1f);
                            handle.getTransform(TransformUI.class).setMargin(0f, -0.3f, 0f, 0f);
                            handle.getTransform(TransformUI.class).setPivotOffset(0f, 1f);
                            handle.addComponent(new org.dragonskulle.devtools.RenderDebug());
                        });

        scene.addRootObject(debugUI);
    }

    private static Scene createMainScene() {
        // Create a scene
        Scene mainScene = new Scene("game");

        addDebugUI(mainScene);

        mainScene.addRootObject(
                new GameObject(
                        "light",
                        (light) -> {
                            light.addComponent(new Light());
                            light.getTransform(Transform3D.class).setRotationDeg(-60f, 0f, 0f);
                        }));

        GameObject cameraRig =
                new GameObject(
                        "mainCamera",
                        (rig) -> {
                            KeyboardMovement keyboardMovement = new KeyboardMovement();
                            rig.addComponent(keyboardMovement);
                            rig.addComponent(new TargetMovement());

                            rig.getTransform(Transform3D.class).setPosition(0, -4, 1.5f);

                            rig.buildChild(
                                    "rotationRig",
                                    (pitchRig) -> {
                                        ZoomTilt zoomTilt = new ZoomTilt();
                                        pitchRig.addComponent(zoomTilt);
                                        pitchRig.buildChild(
                                                "camera",
                                                (camera) -> {
                                                    ScrollTranslate scroll =
                                                            new ScrollTranslate(
                                                                    keyboardMovement, zoomTilt);
                                                    scroll.getStartPos().set(0f, -5f, 0f);
                                                    scroll.getEndPos().set(0f, -100f, 0f);
                                                    camera.addComponent(scroll);

                                                    // Make sure it's an actual camera
                                                    Camera cam = new Camera();
                                                    cam.farPlane = 200;
                                                    camera.addComponent(cam);

                                                    camera.addComponent(new MapEffects());
                                                    camera.addComponent(new FogOfWar());

                                                    AudioListener listener = new AudioListener();
                                                    camera.addComponent(listener);

                                                    AudioSource bgm = new AudioSource();
                                                    bgm.setVolume(0.1f);
                                                    bgm.setLooping(true);
                                                    bgm.playSound(BGM_ID);
                                                    camera.addComponent(bgm);
                                                });
                                    });
                        });

        mainScene.addRootObject(GameObject.instantiate(cameraRig));

        GameObject hexagonMap =
                new GameObject(
                        "hexagon map",
                        new Transform3D(),
                        (map) -> {
                            map.addComponent(new HexagonMap(51));
                        });

        mainScene.addRootObject(hexagonMap);

        return mainScene;
    }

    private static Scene createMainScene(NetworkManager networkManager, boolean asServer) {

        log.warning("We have got here " + asServer);
        Scene mainScene = createMainScene();

        // asServer = true;
        if (asServer) {
            log.warning("I am the server");
            GameObject hostGameUI =
                    new GameObject(
                            "hostGameUI",
                            new TransformUI(false),
                            (root) -> {
                                // root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f,
                                // 0.1f)));
                                root.getTransform(TransformUI.class).setParentAnchor(0f);
                                root.buildChild(
                                        "populate_with_ai",
                                        new TransformUI(true),
                                        (box) -> {
                                            box.getTransform(TransformUI.class)
                                                    .setParentAnchor(0.3f, 0.93f, 1f, 0.93f);
                                            box.getTransform(TransformUI.class)
                                                    .setMargin(0f, 0f, 0f, 0.07f);
                                            box.addComponent(
                                                    new UIButton(
                                                            "Fill game with AI",
                                                            (a, b) -> {
                                                                log.info("should fill with ai");
                                                                networkManager
                                                                        .getServerManager()
                                                                        .spawnNetworkObject(
                                                                                -1,
                                                                                networkManager
                                                                                        .findTemplateByName(
                                                                                                "aiPlayer"));

                                                                log.warning("Created ai");
                                                            }));
                                        });
                            });
            mainScene.addRootObject(hostGameUI);
        }
        return mainScene;
    }

    private Scene createMainMenu() {
        Scene mainMenu = mMainMenuGLTF.get().getDefaultScene();

        addDebugUI(mainMenu);

        TemplateManager templates = new TemplateManager();

        templates.addAllObjects(
                mNetworkTemplatesGLTF.get().getDefaultScene().getGameObjects().stream()
                        .toArray(GameObject[]::new));

        Reference<NetworkManager> networkManager =
                new NetworkManager(templates, App::createMainScene)
                        .getReference(NetworkManager.class);

        GameObject networkManagerObject =
                new GameObject(
                        "network manager",
                        (handle) -> {
                            handle.addComponent(networkManager.get());
                        });

        Reference<AudioSource> effectSource = new Reference<>(new AudioSource());

        GameObject audio =
                new GameObject(
                        "audio",
                        (audioRoot) -> {
                            audioRoot.buildChild(
                                    "muteUI",
                                    new TransformUI(true),
                                    (muteUI) -> {
                                        TransformUI t = muteUI.getTransform(TransformUI.class);
                                        t.setParentAnchor(0.78f, 0.75f, 1f, 0.75f);
                                        t.setMargin(0f, 0.1f, 0f, 0.2f);

                                        muteUI.addComponent(
                                                new UIButton(
                                                        "Toggle Mute",
                                                        (uiButton, __) -> {
                                                            effectSource
                                                                    .get()
                                                                    .playSound(BUTTON_SFX_ID);
                                                            AudioManager.getInstance()
                                                                    .toggleMasterMute();
                                                        }));
                                    });

                            audioRoot.addComponent(effectSource.get());
                            effectSource.get().setVolume(0.1f);

                            AudioSource bgm = new AudioSource();
                            bgm.setVolume(0.1f);
                            bgm.setLooping(true);
                            bgm.playSound(BGM_ID);

                            audioRoot.addComponent(bgm);
                            audioRoot.addComponent(new AudioListener());
                        });

        mainMenu.addRootObject(audio);

        GameObject gameTitle =
                new GameObject(
                        "title",
                        new TransformUI(true),
                        (title) -> {
                            TransformUI t = title.getTransform(TransformUI.class);
                            t.setParentAnchor(0.4f, 0.05f, 0.8f, 0.05f);
                            t.setMargin(0f, 0f, 0f, 0.2f);

                            UIText txt = new UIText("Hex Wars");
                            txt.setSetMarginsOnAwake(false);

                            title.addComponent(txt);
                        });

        GameObject mainUI =
                new GameObject(
                        "mainUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                            root.addChild(gameTitle);
                        });

        GameObject joinUI =
                new GameObject(
                        "joinUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        GameObject hostUI =
                new GameObject(
                        "hostUI",
                        new TransformUI(false),
                        (root) -> {
                            root.addComponent(new UIRenderable(new Vector4f(1f, 1f, 1f, 0.1f)));
                            root.getTransform(TransformUI.class).setParentAnchor(0f);
                        });

        mainUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Join Game",
                                                (uiButton, __) -> {
                                                    mainUI.setEnabled(false);
                                                    joinUI.setEnabled(true);
                                                    hostUI.setEnabled(false);
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "hostButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.15f, 0.5f, 0.15f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Host Game",
                                                (uiButton, __) -> {
                                                    mainUI.setEnabled(false);
                                                    hostUI.setEnabled(true);
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "settingsButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.25f, 0.5f, 0.25f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Settings",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "quitButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Quit",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    Engine.getInstance().stop();
                                                });

                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "reloadButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.45f, 0.5f, 0.45f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Quick Reload",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    sReload = true;
                                                    Engine.getInstance().stop();
                                                });

                                button.addComponent(newButton);
                            });
                });
        joinUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    final Reference<GameObject> connectingRef =
                            bg.buildChild(
                                    "connecting",
                                    false,
                                    new TransformUI(true),
                                    (text) -> {
                                        text.getTransform(TransformUI.class)
                                                .setParentAnchor(0f, 0.12f, 0.5f, 0.12f);
                                        text.getTransform(TransformUI.class)
                                                .setMargin(0f, 0f, 0f, 0.07f);
                                        text.addComponent(
                                                new UIText(
                                                        new Vector3f(0f),
                                                        Font.getFontResource("Rise of Kingdom.ttf"),
                                                        "Connecting..."));
                                    });
                    final Reference<UIText> connectingTextRef =
                            connectingRef.get().getComponent(UIText.class);

                    connectingTextRef.get().setEnabled(false);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Join (Temporary)",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    networkManager
                                                            .get()
                                                            .createClient(
                                                                    sIP,
                                                                    sPort,
                                                                    (gameScene, manager, netID) -> {
                                                                        if (netID >= 0) {
                                                                            onConnectedClient(
                                                                                    gameScene,
                                                                                    manager, netID);
                                                                        } else if (Reference
                                                                                .isValid(
                                                                                        connectingTextRef)) {
                                                                            connectingTextRef
                                                                                    .get()
                                                                                    .setEnabled(
                                                                                            false);
                                                                        }
                                                                    });
                                                    if (Reference.isValid(connectingTextRef))
                                                        connectingTextRef.get().setEnabled(true);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Cancel",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    joinUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        hostUI.buildChild(
                "bg",
                new TransformUI(false),
                (bg) -> {
                    bg.addComponent(new UIRenderable(new Vector4f(0.1f, 0.1f, 0.1f, 0f)));

                    bg.getTransform(TransformUI.class).setParentAnchor(0f, 0f, 0.5f, 1.f);

                    bg.buildChild(
                            "joinButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.05f, 0.5f, 0.05f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Host (Temporary)",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    networkManager
                                                            .get()
                                                            .createServer(
                                                                    sPort, this::onClientConnected);
                                                });
                                button.addComponent(newButton);
                            });

                    bg.buildChild(
                            "cancelButton",
                            new TransformUI(true),
                            (button) -> {
                                button.getTransform(TransformUI.class)
                                        .setParentAnchor(0f, 0.35f, 0.5f, 0.35f);
                                button.getTransform(TransformUI.class).setMargin(0f, 0f, 0f, 0.07f);

                                UIButton newButton =
                                        new UIButton(
                                                "Cancel",
                                                (uiButton, __) -> {
                                                    effectSource.get().playSound(BUTTON_SFX_ID);
                                                    hostUI.setEnabled(false);
                                                    mainUI.setEnabled(true);
                                                });

                                button.addComponent(newButton);
                            });
                });

        joinUI.setEnabled(false);
        hostUI.setEnabled(false);

        mainMenu.addRootObject(networkManagerObject);

        mainMenu.addRootObject(hostUI);
        mainMenu.addRootObject(joinUI);
        mainMenu.addRootObject(mainUI);

        return mainMenu;
    }

    /**
     * Entrypoint of the program. Creates and runs one app instance
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        UIAppearence appearence = UIManager.getInstance().getAppearence();
        appearence.getTextFont().free();
        appearence.setTextFont(Font.getFontResource("Rise of Kingdom.ttf"));

        do {
            sReload = false;
            Settings.getInstance().loadSettings();
            try (App app = new App()) {
                app.run();
            }
        } while (sReload);

        AudioManager.getInstance().cleanup();
        System.exit(0);
    }

    private void run() {
        // Create the main menu
        Scene mainMenu = createMainMenu();

        // Load the mainMenu as the presentation scene
        Engine.getInstance().loadPresentationScene(mainMenu);

        // Load dev console
        // TODO: actually make a fully fledged console
        // TODO: join it at the end
        new Thread(
                        () -> {
                            Scanner in = new Scanner(System.in);

                            String line;

                            while ((line = in.nextLine()) != null) {
                                try {
                                    sPort = in.nextInt();
                                    sIP = line.trim();
                                    log.info("Address set successfully!");
                                } catch (Exception e) {
                                    log.info("Failed to set IP and port!");
                                }
                            }
                        })
                .start();

        // Run the game
        Engine.getInstance().start("Hex Wars", new GameBindings());
    }

    private void onConnectedClient(Scene gameScene, NetworkManager manager, int netID) {
        log.info("CONNECTED ID " + netID);

        GameObject humanPlayer =
                new GameObject(
                        "human player",
                        (handle) -> {
                            handle.addComponent(
                                    new HumanPlayer(
                                            manager.getReference(NetworkManager.class), netID));
                        });

        gameScene.addRootObject(humanPlayer);
    }

    private void onClientConnected(
            Scene gameScene, NetworkManager manager, ServerClient networkClient) {
        int id = networkClient.getNetworkID();
        manager.getServerManager().spawnNetworkObject(id, manager.findTemplateByName("player"));
    }

    @Override
    public void free() {
        mMainMenuGLTF.free();
        mNetworkTemplatesGLTF.free();
    }
}
