package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;
import org.jcae.opencascade.Utilities;

public class GeomPlaneDowncast {
    @Test public void downcast() {
        double[] p1=new double[]{0, 0, 0};
        double[] p2=new double[]{1, 1, 1};
        BRepPrimAPI_MakeBox makeBox=new BRepPrimAPI_MakeBox(p1, p2);
        Geom_Surface s = BRep_Tool.surface(Utilities.getFace(makeBox.shape(), 0));
        assertTrue(s.getClass() + " != " + Geom_Plane.class, s instanceof Geom_Plane);
    }
}
