/* (C) 2021 DragonSkulle */
package org.dragonskulle.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dragonskulle.components.Component;
import org.dragonskulle.components.Transform;
import org.joml.Matrix4fc;

/**
 * GameObject class
 *
 * @author Harry Stoltz
 * @author Aurimas Blažulionis
 *     <p>Each GameObject represents a single entity in the game, the object itself performs no
 *     actions but each component that is added to the GameObject will be able to interact with
 *     itself in the world and other GameObjects.
 */
public class GameObject implements Serializable {

    private final Reference<GameObject> mReference = new Reference<>(this);
    private final ArrayList<Component> mComponents = new ArrayList<>();
    private final ArrayList<GameObject> mChildren = new ArrayList<>();

    private GameObject mRoot;
    private GameObject mParent;
    private Transform mTransform = new Transform();
    private final String mName;
    // TODO: Make some sort of Tag class or enum and add here
    private boolean mActive;

    /**
     * A handler interface for building game objects
     *
     * <p>This handler interface is used in builder constructor, and {@link GameObject#buildChild}
     * method. It allows for a more structured way of constructing nested trees of game objects.
     */
    public interface IBuildHandler {
        /**
         * Handle building of object
         *
         * <p>This method will be called to allow initial setup of the object. It will be already
         * linked up with its parent, if there is any.
         */
        void handleBuild(GameObject go);
    }

    /**
     * Create a clone of a GameObject. The cloned GameObject's position is used
     *
     * @param object GameObject to be copied
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object) {
        GameObject instance = object.createClone();
        instance.mTransform.setGameObject(instance);
        return object.createClone();
    }

    /**
     * Create a clone of a GameObject, providing a new transform for the object
     *
     * @param object GameObject to be copied
     * @param transform New transform for the object. Must be passed exclusively (i.e. only to this
     *     instance)
     * @return The new instance of the GameObject
     */
    public static GameObject instantiate(GameObject object, Transform transform) {
        GameObject instance = object.createClone();
        transform.setGameObject(instance);
        instance.mTransform = transform;
        return instance;
    }

    /**
     * Find an instance of a GameObject with a given name in the currently active scene. This is
     * very slow and should not be used in any update loops. Instead, you should get all references
     * to necessary GameObject's in onAwake or onStart and save them for future use.
     *
     * @param name Name of the object to search for
     * @return A reference to the first GameObject found, or null if nothing is found
     */
    public static Reference<GameObject> FindObjectByName(String name) {
        Scene activeScene = Engine.getInstance().getActiveScene();

        for (GameObject root : activeScene.getRootObjects()) {

            ArrayList<GameObject> children = new ArrayList<>();
            root.getAllChildren(children);

            for (GameObject obj : children) {

                if (obj.mName.equals(name)) {
                    return obj.getReference();
                }
            }
        }
        return null;
    }

    // TODO: GetComponentsIn(Parent/Children)

    /**
     * Constructor for GameObject, defaults mActive to true
     *
     * @param name The name of the object
     */
    public GameObject(String name) {
        this(name, true);
    }

    /**
     * Constructor for GameObject
     *
     * @param name The name of the object
     * @param active Whether the object is active or not
     */
    public GameObject(String name, boolean active) {
        mRoot = null;
        mParent = null;
        mName = name;
        mActive = active;
        mTransform.setGameObject(this);
    }

    /**
     * Constructor for game object, allows initial setup
     *
     * @param name name of the object
     * @param active controls whether the object is active by default
     * @param handler handler callback that allows to do initial setup
     */
    public GameObject(String name, boolean active, Transform transform, IBuildHandler handler) {
        this(name, active, transform);
        handler.handleBuild(this);
    }

    /**
     * Constructor for game object, allows initial setup
     *
     * @param name name of the object
     * @param handler handler callback that allows to do initial setup
     */
    public GameObject(String name, Transform transform, IBuildHandler handler) {
        this(name, true, transform, handler);
    }

    /**
     * Constructor for game object, allows initial setup
     *
     * @param name name of the object
     * @param active controls whether the object is active by default
     * @param handler handler callback that allows to do initial setup
     */
    public GameObject(String name, boolean active, IBuildHandler handler) {
        this(name, active);
        handler.handleBuild(this);
    }

    /**
     * Constructor for game object, allows initial setup
     *
     * @param name name of the object
     * @param handler handler callback that allows to do initial setup
     */
    public GameObject(String name, IBuildHandler handler) {
        this(name, true, handler);
    }

    /**
     * Constructor for GameObject, defaults mActive to true
     *
     * @param name The name of the object
     * @param transform Transformation properties to apply
     */
    public GameObject(String name, Transform transform) {
        this(name, true, transform);
    }

    /**
     * Constructor for GameObject, defaults mActive to true
     *
     * @param name The name of the object
     * @param matrix Transformation matrix to use
     */
    public GameObject(String name, Matrix4fc matrix) {
        this(name, true, matrix);
    }

    /**
     * Constructur for GameObject
     *
     * @param name The name of the object
     * @param active Whether the object is active or not
     * @param transform Object transformation
     */
    public GameObject(String name, boolean active, Transform transform) {
        mRoot = null;
        mParent = null;
        mName = name;
        mActive = active;
        mTransform = transform;
        mTransform.setGameObject(this);
    }

    /**
     * Constructur for GameObject
     *
     * @param name The name of the object
     * @param active Whether the object is active or not
     * @param matrix Object transformation
     */
    public GameObject(String name, boolean active, Matrix4fc matrix) {
        mRoot = null;
        mParent = null;
        mName = name;
        mActive = active;
        mTransform.setGameObject(this);
        mTransform.setLocalMatrix(matrix);
    }

    /**
     * Get all components of a given type T
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @param ret List that will store the components found
     */
    public <T extends Component> void getComponents(Class<T> type, List<Reference<T>> ret) {
        mComponents.stream()
                .filter(type::isInstance)
                .map(component -> component.getReference(type))
                .filter(Reference::isValid)
                .collect(Collectors.toCollection(() -> ret));
    }

    /**
     * Get the first component of type T found
     *
     * @param type Class object of T
     * @param <T> Type of component to be returned
     * @return The first component of type T found, or null if none were found
     */
    public <T extends Component> Reference<T> getComponent(Class<T> type) {
        return mComponents.stream()
                .filter(type::isInstance)
                .map(component -> component.getReference(type))
                .filter(Reference::isValid)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a list of all components that implement the interface I
     *
     * @param iface Class object of the interface I
     * @param <I> Interface to search by
     * @param ret List that will contain any components found
     */
    public <I> void getComponentsByIface(Class<I> iface, List<Reference<Component>> ret) {
        mComponents.stream()
                .filter(iface::isInstance)
                .map(Component::getReference)
                .collect(Collectors.toCollection(() -> ret));
    }

    /**
     * Get a list of all components of a specific type in all children of this GameObject
     *
     * @param type Class object of type T
     * @param ret List object to store the references to components found
     * @param <T> Type of component to search for
     */
    public <T extends Component> void getComponentsInChildren(
            Class<T> type, List<Reference<T>> ret) {
        for (GameObject child : mChildren) {
            child.getComponents(type, ret);
            child.getComponentsInChildren(type, ret);
        }
    }

    /**
     * Get every child with this GameObject acting as the root in a tree, adding to the list in a
     * depth-first order.
     *
     * <p>Doesn't return a list of references as this method should only be used by the engine which
     * is responsible for the destroying of objects and therefore won't keep any strong references
     * to destroyed objects.
     */
    protected void getAllChildren(List<GameObject> ret) {
        for (GameObject child : mChildren) {
            ret.add(child);
            child.getAllChildren(ret);
        }
    }

    /**
     * Add a component to the GameObject. If the component's GameObject is null, it is set to this
     * If the component's GameObject is another GameObject, the component is removed from that
     * GameObject and is set to this.
     *
     * @param component Component to be added
     */
    public void addComponent(Component component) {
        GameObject obj = component.getGameObject();

        if (obj == null) {
            // GameObject not set
            component.setGameObject(this);
        } else if (obj != this) {
            // GameObject is some other object
            obj.removeComponent(component);
            component.setGameObject(this);
        }

        // Add the component
        mComponents.add(component);
    }

    /**
     * Add a child to the GameObject. Setting the parent to this and the root to this.mRoot
     *
     * @param child GameObject to be added as a child
     */
    public void addChild(GameObject child) {
        // If this doesn't have a parent, it must be a root object so set the child's root to this
        if (mRoot == null) {
            child.mRoot = this;
        } else {
            child.mRoot = mRoot;
        }
        child.mParent = this;
        mChildren.add(child);
    }

    /**
     * Add a list of children to the GameObject, setting the parent to this and the root to mRoot,
     * or this if mRoot is null
     *
     * @param children List of GameObject to be added
     */
    public void addChildren(List<GameObject> children) {

        GameObject root = (mRoot == null) ? this : mRoot;

        for (GameObject child : children) {
            child.mRoot = root;
            child.mParent = this;
        }
        mChildren.addAll(children);
    }

    /**
     * Build a child for the GameObject. It will create an object of given name, attach to the
     * parent, and then call the build handler method.
     *
     * @param name name of the object
     * @param handler handler callback to do initial setup
     */
    public void buildChild(String name, IBuildHandler handler) {
        buildChild(name, true, handler);
    }

    /**
     * Build a child for the GameObject. It will create an object of given name, attach to the
     * parent, and then call the build handler method.
     *
     * @param name name of the object
     * @param handler handler callback to do initial setup
     */
    public void buildChild(String name, boolean active, IBuildHandler handler) {
        GameObject go = new GameObject(name, active);
        this.addChild(go);
        handler.handleBuild(go);
    }

    /**
     * Build a child for the GameObject. It will create an object of given name, attach to the
     * parent, and then call the build handler method.
     *
     * @param name name of the object
     * @param handler handler callback to do initial setup
     */
    public void buildChild(String name, Transform transform, IBuildHandler handler) {
        buildChild(name, true, transform, handler);
    }

    /**
     * Build a child for the GameObject. It will create an object of given name, attach to the
     * parent, and then call the build handler method.
     *
     * @param name name of the object
     * @param handler handler callback to do initial setup
     */
    public void buildChild(
            String name, boolean active, Transform transform, IBuildHandler handler) {
        GameObject go = new GameObject(name, active, transform);
        this.addChild(go);
        handler.handleBuild(go);
    }

    /**
     * Remove component from the GameObject. If a component is removed, the scene's updated flag is
     * set to true. Set's the component's GameObject to null
     *
     * @param component Component to be removed
     */
    public void removeComponent(Component component) {
        if (mComponents.remove(component)) {
            component.setGameObject(null);
        }
    }

    /**
     * Remove a child from the GameObject.
     *
     * @param child GameObject to be removed
     */
    public void removeChild(GameObject child) {
        mChildren.remove(child);
    }

    /** Handle the destruction of the object. */
    protected void engineDestroy() {
        // Create a copy of the list of children this object has
        ArrayList<GameObject> children = new ArrayList<>(mChildren);

        // Iterate through the children and destroy all of them
        for (GameObject child : children) {
            child.engineDestroy();
        }

        // Add all components to the set of destroyed components.
        // We do this instead of destroying them here to prevent double-destroys
        Engine.getInstance().mDestroyedComponents.addAll(mComponents);

        // Destroy the transform of this GameObject
        mTransform.destroy();
        mTransform = null;

        // After we have finished destroying we need to clear our reference so nothing attempts to
        // access this after being destroyed
        mReference.clear();

        // Then remove this GameObject from the parent and remove the link to the parent
        if (mParent != null) {
            mParent.removeChild(this);
            mParent = null;
        }
        mRoot = null;
    }

    /**
     * Add the GameObject to the list of objects that need to be destroyed in the Engine instance
     */
    public void destroy() {
        Engine.getInstance().mDestroyedObjects.add(this);
    }

    /**
     * Create a deep copy of the GameObject
     *
     * @return New GameObject with identical values as this
     */
    public GameObject createClone() {
        byte[] objectData = null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.flush();
            oos.close();
            baos.close();
            objectData = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (objectData != null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
                return (GameObject) new ObjectInputStream(bais).readObject();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Getter for mChildren
     *
     * @return mChildren
     */
    public ArrayList<GameObject> getChildren() {
        return new ArrayList<>(mChildren);
    }

    /**
     * Getter for mComponents, should only be used by the engine
     *
     * @return mComponents
     */
    protected ArrayList<Component> getComponents() {
        return mComponents;
    }

    /**
     * Getter for mActive
     *
     * @return mActive
     */
    public boolean isActive() {
        return mActive;
    }

    /**
     * Setter for mActive
     *
     * @param val New value for mActive
     */
    public void setActive(boolean val) {
        mActive = val;
    }

    /**
     * Getter for mTransform
     *
     * @return mTransform
     */
    public Transform getTransform() {
        return mTransform;
    }

    /**
     * Get the transform from the parent of this GameObject
     *
     * @return Parent Transform, or null if this is a root GameObject
     */
    public Transform getParentTransform() {
        return mParent != null ? mParent.mTransform : null;
    }

    /**
     * Getter for mReference
     *
     * @return mReference
     */
    public Reference<GameObject> getReference() {
        return mReference;
    }

    /**
     * Check whether a GameObject is a root object
     *
     * @return true if it's a root, false otherwise
     */
    public boolean isRootObject() {
        return mParent == null;
    }

    /**
     * Getter for mRoot, used for testing
     *
     * @return mRoot
     */
    protected GameObject getRoot() {
        return mRoot;
    }

    /**
     * Getter for mParent, used for testing
     *
     * @return mParent
     */
    protected GameObject getParent() {
        return mParent;
    }
}