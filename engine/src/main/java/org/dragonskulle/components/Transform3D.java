/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * 3D Transform class
 *
 * @author Harry Stoltz
 *     <p>Transform3D object the position, rotation and scale of the GameObject (As right, up,
 *     forward and position in a 4x4 Matrix). The Transform3D can be used to get 3D position, scale
 *     and rotation.
 */
public class Transform3D extends Transform {
    protected final Matrix4f mLocalMatrix;
    protected Matrix4f mWorldMatrix;

    /** Default constructor. mLocalMatrix is just the identity matrix */
    public Transform3D() {
        mLocalMatrix = new Matrix4f();
        mWorldMatrix = new Matrix4f();
    }

    /**
     * Constructor with initial position
     *
     * @param position Starting position for the object
     */
    public Transform3D(Vector3fc position) {
        this(position.x(), position.y(), position.z());
    }

    /**
     * Constructor with initial position
     *
     * @param x X position axis
     * @param y Y position axis
     * @param z Z position axis
     */
    public Transform3D(float x, float y, float z) {
        mLocalMatrix = new Matrix4f().translate(x, y, z);
        mWorldMatrix = new Matrix4f();
    }

    /**
     * Constructor with initial transformation matrix
     *
     * @param matrix Matrix to be used for mLocalMatrix
     */
    public Transform3D(Matrix4fc matrix) {
        mLocalMatrix = new Matrix4f(matrix);
        mWorldMatrix = new Matrix4f();
    }

    /**
     * Set the position of the object relative to the parent
     *
     * @param position Vector3f containing the desired position
     */
    public void setPosition(Vector3fc position) {
        setPosition(position.x(), position.y(), position.z());
    }

    /**
     * Set the position of the object relative to the parent
     *
     * @param x X coordinate of position
     * @param y Y coordinate of position
     * @param z Z coordinate of position
     */
    public void setPosition(float x, float y, float z) {
        mLocalMatrix.setTranslation(x, y, z);
        setUpdateFlag();
    }

    /**
     * Set the rotation of the object relative to the parent
     *
     * @param rotation {@link Quaternionfc} containing the desired quaternion rotation
     */
    public void setRotation(Quaternionfc rotation) {
        mLocalMatrix.rotation(rotation);
        setUpdateFlag();
    }

    /**
     * Set the rotation of the object relative to the parent
     *
     * @param rotation Vector3f containing the desired rotation
     */
    public void setRotation(Vector3fc rotation) {
        setRotation(rotation.x(), rotation.y(), rotation.z());
    }

    /**
     * Set the rotation of the object relative to the parent
     *
     * @param rotation Vector3f containing the desired rotation
     */
    public void setRotationDeg(Vector3fc rotation) {
        setRotation(
                rotation.x() * DEG_TO_RAD, rotation.y() * DEG_TO_RAD, rotation.z() * DEG_TO_RAD);
    }

    /**
     * Set the rotation of the object relative to the parent
     *
     * @param x X coordinate of rotation
     * @param y Y coordinate of rotation
     * @param z Z coordinate of rotation
     */
    public void setRotation(float x, float y, float z) {
        mLocalMatrix.setRotationXYZ(x, y, z);
        setUpdateFlag();
    }

    /**
     * Set the rotation of the object relative to the parent
     *
     * @param x X coordinate of rotation
     * @param y Y coordinate of rotation
     * @param z Z coordinate of rotation
     */
    public void setRotationDeg(float x, float y, float z) {
        setRotation(x * DEG_TO_RAD, y * DEG_TO_RAD, z * DEG_TO_RAD);
    }

    /**
     * Rotate the object with euler angles
     *
     * @param eulerAngles Vector containing euler angles to rotate object with
     */
    public void rotateRad(Vector3fc eulerAngles) {
        rotateRad(eulerAngles.x(), eulerAngles.y(), eulerAngles.z());
    }

    /**
     * Rotate the object with euler angles
     *
     * @param x Rotation in X-axis in radians
     * @param y Rotation in Y-axis in radians
     * @param z Rotation in Z-axis in radians
     */
    public void rotateRad(float x, float y, float z) {
        mLocalMatrix.rotateXYZ(x, y, z);
        setUpdateFlag();
    }

    /**
     * Rotate the object with euler angles
     *
     * @param x Rotation in X-axis in degrees
     * @param y Rotation in Y-axis in degrees
     * @param z Rotation in Z-axis in degrees
     */
    public void rotateDeg(float x, float y, float z) {
        mLocalMatrix.rotateXYZ(DEG_TO_RAD * x, DEG_TO_RAD * y, DEG_TO_RAD * z);
        setUpdateFlag();
    }

    /**
     * Rotate the object with a quaternion
     *
     * @param quaternion Quaternion to rotate object with
     */
    public void rotate(Quaternionfc quaternion) {
        mLocalMatrix.rotate(quaternion);
        setUpdateFlag();
    }

    /**
     * Translate the object
     *
     * @param translation Vector translation to perform
     */
    public void translate(Vector3fc translation) {
        mLocalMatrix.translate(translation);
        setUpdateFlag();
    }

    /**
     * Translate the object
     *
     * @param x Translation in X-axis
     * @param y Translation in Y-axis
     * @param z Translation in Z-axis
     */
    public void translate(float x, float y, float z) {
        mLocalMatrix.translateLocal(x, y, z);
        setUpdateFlag();
    }

    /**
     * Translate the object relative to current orientation
     *
     * @param x Translation in X-axis
     * @param y Translation in Y-axis
     * @param z Translation in Z-axis
     */
    public void translateLocal(float x, float y, float z) {
        mLocalMatrix.translate(x, y, z);
        setUpdateFlag();
    }

    /**
     * Scale the object
     *
     * @param scale Vector to scale object with
     */
    public void scale(Vector3fc scale) {
        mLocalMatrix.scaleLocal(scale.x(), scale.y(), scale.z());
        setUpdateFlag();
    }

    /**
     * Scale the object
     *
     * @param x Scale in the X-axis
     * @param y Scale in the Y-axis
     * @param z Scale in the Z-axis
     */
    public void scale(float x, float y, float z) {
        mLocalMatrix.scale(x, y, z);
        setUpdateFlag();
    }

    /**
     * Get the transformation matrix relative to the parent transform
     *
     * @return A constant reference to the local matrix
     */
    public Matrix4fc getLocalMatrix() {
        return mLocalMatrix;
    }

    /**
     * Get the transformation matrix relative to the parent transform (mutable version)
     *
     * <p>Calling this method will trigger transform update. Use only if you are going to modify the
     * matrix.
     *
     * @return Mutable reference to the local matrix
     */
    public Matrix4f getLocalMatrixMut() {
        setUpdateFlag();
        return mLocalMatrix;
    }

    /**
     * Set the transformation matrix relative to the parent transform
     *
     * @param from transformation matrix to set
     */
    public void setLocalMatrix(Matrix4fc from) {
        mLocalMatrix.set(from);
        setUpdateFlag();
    }

    /**
     * Get the transformation matrix relative to the parent transform
     *
     * @param dest Matrix to store a copy of the local matrix
     */
    public void getLocalMatrix(Matrix4f dest) {
        dest.set(mLocalMatrix);
    }

    /**
     * Get the normalised rotation of the transform
     *
     * @return Rotation of the transform as Quaternion
     */
    public Quaternionf getLocalRotation() {
        Quaternionf rotation = new Quaternionf();
        mLocalMatrix.getNormalizedRotation(rotation);
        return rotation;
    }

    /**
     * Get the normalised rotation of the transform, relative to the parent transform
     *
     * @param dest Quaternion to store the rotation
     */
    public void getLocalRotation(Quaternionf dest) {
        mLocalMatrix.getNormalizedRotation(dest);
    }

    /**
     * Get the rotation of the transform per axis, in radians, relative to the parent transform
     *
     * @return Rotation of the transform as AxisAngle
     */
    public AxisAngle4f getLocalRotationAngles() {
        AxisAngle4f rotation = new AxisAngle4f();
        mLocalMatrix.getRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform per axis, in radians, relative to the parent transform
     *
     * @param dest AxisAngle4f to store the rotation
     */
    public void getLocalRotationAngles(AxisAngle4f dest) {
        mLocalMatrix.getRotation(dest);
    }

    /**
     * Get the position of the transform
     *
     * @return Vector3f containing the XYZ position of the object
     */
    public Vector3f getLocalPosition() {
        Vector3f position = new Vector3f();
        mLocalMatrix.getColumn(3, position);
        return position;
    }

    /**
     * Get the position of the transform relative to the parent transform
     *
     * @param dest Vector3f to store the position
     */
    public void getLocalPosition(Vector3f dest) {
        mLocalMatrix.getColumn(3, dest);
    }

    /**
     * Get the scale of the transform
     *
     * @return Vector3f containing the XYZ scale of the object
     */
    public Vector3f getLocalScale() {
        Vector3f scale = new Vector3f();
        mLocalMatrix.getScale(scale);
        return scale;
    }

    /**
     * Get the scale of the transform
     *
     * @param dest Vector3f to store the scale
     */
    public void getLocalScale(Vector3f dest) {
        mLocalMatrix.getScale(dest);
    }

    /**
     * Get the world matrix for this transform. If the transform is on a root object, mLocalMatrix
     * is used as the worldMatrix. If it isn't a root object and mShouldUpdate is true, recursively
     * call getWorldMatrix up to the first Transform that has mShouldUpdate as false
     *
     * @return constant mWorldMatrix
     */
    public Matrix4fc getWorldMatrix() {
        if (mShouldUpdate) {
            mShouldUpdate = false;
            if (mGameObject == null || mGameObject.isRootObject()) {
                mWorldMatrix = mLocalMatrix;
            } else {
                // Multiply parent's world matrix by the local matrix
                // Which gives us the matrix multiplication mLocalMatrix * parentWorldMatrix
                // so when doing mWorldMatrix * (vector)
                // It is the same as doing parentWorldMatrix * mLocalMatrix * (vector)
                // so that any parent transformations are done prior to the local transformation
                mGameObject
                        .getParentTransform()
                        .getMatrixForChildren()
                        .mul(mLocalMatrix, mWorldMatrix);
            }
        }
        return mWorldMatrix;
    }

    /**
     * Get the rotation of the transform in the world as a Quaternion
     *
     * @return Quaternionf containing the rotation of the transform
     */
    public Quaternionf getRotation() {
        Quaternionf rotation = new Quaternionf();
        getWorldMatrix().getNormalizedRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform in the world as a Quaternion
     *
     * @param dest Quaternionf to store the rotation of the transform
     */
    public void getRotation(Quaternionf dest) {
        getWorldMatrix().getNormalizedRotation(dest);
    }

    /**
     * Get the rotation of the transform in the world as axis angles
     *
     * @return AxisAngle4f containing the rotation of the transform
     */
    public AxisAngle4f getRotationAngles() {
        AxisAngle4f rotation = new AxisAngle4f();
        getWorldMatrix().getRotation(rotation);
        return rotation;
    }

    /**
     * Get the rotation of the transform in the world as axis angles
     *
     * @param dest AxisAnglef to store the rotation of the transform
     */
    public void getRotationAngles(AxisAngle4f dest) {
        getWorldMatrix().getRotation(dest);
    }

    /**
     * Get the position of the transform in the world
     *
     * @return Vector3f containing the world position
     */
    public Vector3f getPosition() {
        Vector3f position = new Vector3f();
        getWorldMatrix().getColumn(3, position);
        return position;
    }

    /**
     * Get the position of the transform in the world
     *
     * @param dest Vector3f to store the position
     */
    public void getPosition(Vector3f dest) {
        getWorldMatrix().getColumn(3, dest);
    }

    /**
     * Get the scale of the transform in the world
     *
     * @return Vector3f containing the scale of the transform
     */
    public Vector3f getScale() {
        Vector3f scale = new Vector3f();
        getWorldMatrix().getScale(scale);
        return scale;
    }

    /**
     * Get the scale of the transform in the world
     *
     * @param dest Vector3f to store the scale
     */
    public void getScale(Vector3f dest) {
        getWorldMatrix().getScale(dest);
    }

    @Override
    protected void onDestroy() {}
}
