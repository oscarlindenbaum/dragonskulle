/* (C) 2021 DragonSkulle */
package org.dragonskulle.components;

import static org.junit.Assert.assertEquals;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.Test;

// The below tests were generated by https://www.diffblue.com/

public class DiffBlueTransform3DTest {
    @Test
    public void testSetRotation() {
        Transform3D transform3D = new Transform3D();
        transform3D.setRotation(10.0f, 10.0f, 10.0f);
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(0.1836784f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(-0.8589406f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testSetRotation2() {
        Transform3D transform3D = new Transform3D();
        transform3D.setRotation(new Quaternionf(2.0, 3.0, 10.0, 10.0));
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(3.0f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(
                TransformHex.HEX_HEIGHT, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testSetRotation3() {
        Transform3D transform3D = new Transform3D();
        transform3D.setRotation(new Vector3f(10.0f));
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(0.1836784f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(-0.8589406f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testSetRotationDeg() {
        Transform3D transform3D = new Transform3D();
        transform3D.setRotationDeg(10.0f, 10.0f, 10.0f);
        assertEquals(0.078926474f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(0.09406091f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(0.078926474f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(0.9892896f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testSetRotationDeg2() {
        Transform3D transform3D = new Transform3D();
        transform3D.setRotationDeg(new Vector3f(10.0f));
        assertEquals(0.078926474f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(0.09406091f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(0.078926474f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(0.9892896f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testRotateRad() {
        Transform3D transform3D = new Transform3D();
        transform3D.rotateRad(10.0f, 10.0f, 10.0f);
        assertEquals(0.1836784f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(0.18367839f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(0.9045899f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testRotateRad2() {
        Transform3D transform3D = new Transform3D();
        transform3D.rotateRad(new Vector3f(10.0f));
        assertEquals(0.1836784f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(-0.33799654f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(0.18367839f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(0.9045899f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testRotateDeg() {
        Transform3D transform3D = new Transform3D();
        transform3D.rotateDeg(10.0f, 10.0f, 10.0f);
        assertEquals(0.09406091f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(0.078926474f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(0.09406091f, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(0.9879655f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testRotate() {
        Transform3D transform3D = new Transform3D();
        transform3D.rotate(new Quaternionf(2.0, 3.0, 10.0, 10.0));
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(3.0f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(
                TransformHex.HEX_HEIGHT, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
    }

    @Test
    public void testTranslate() {
        Transform3D transform3D = new Transform3D();
        transform3D.translate(10.0f, 10.0f, 10.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).y, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).x, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).z, 0.0f);
    }

    @Test
    public void testTranslate2() {
        Transform3D transform3D = new Transform3D();
        transform3D.translate(new Vector3f(10.0f));
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).x, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).y, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).z, 0.0f);
    }

    @Test
    public void testTranslateLocal() {
        Transform3D transform3D = new Transform3D();
        transform3D.translateLocal(10.0f, 10.0f, 10.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).y, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).x, 0.0f);
        assertEquals(TransformHex.HEX_SIZE, ((Vector3f) transform3D.getLocalForward()).y, 0.0f);
        assertEquals(0.0f, ((Vector3f) transform3D.getLocalForward()).x, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).z, 0.0f);
        assertEquals(0.0f, ((Vector3f) transform3D.getLocalForward()).z, 0.0f);
    }

    @Test
    public void testSetLocal3DTransformation() {
        Transform3D transform3D = new Transform3D();
        Vector3f position = new Vector3f(10.0f);
        Quaternionf rotation = new Quaternionf(2.0, 3.0, 10.0, 10.0);
        transform3D.setLocal3DTransformation(position, rotation, new Vector3f(10.0f));
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).z, 0.0f);
        assertEquals(3.0f, ((Quaternionf) transform3D.getLocalRotation()).y, 0.0f);
        assertEquals(
                TransformHex.HEX_HEIGHT, ((Quaternionf) transform3D.getLocalRotation()).x, 0.0f);
        assertEquals(10.0f, ((Quaternionf) transform3D.getLocalRotation()).w, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).x, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalScale()).x, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalScale()).y, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).y, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalPosition()).z, 0.0f);
        assertEquals(10.0f, ((Vector3f) transform3D.getLocalScale()).z, 0.0f);
    }
}
