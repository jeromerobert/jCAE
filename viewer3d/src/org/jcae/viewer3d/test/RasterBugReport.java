/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.viewer3d.test;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.DepthComponent;
import javax.media.j3d.DepthComponentFloat;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.Raster;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;

/**
 * This is the program send to the java3d issue.
 * @author Julian Ibarz
 */

public class RasterBugReport extends Applet {

    public BranchGroup createSceneGraph() {
        // Create the root of the branch graph
        BranchGroup objRoot = new BranchGroup();

        // Create a Transform3D that is a rotation about the
        // Y-axis.  Add this to a TransformGroup and add the
        // TransformGroup to the root of the subgraph.      
        Transform3D rotateY = new Transform3D();
        rotateY.rotY(.6);
        TransformGroup objTrans = 
            new TransformGroup(rotateY);
        objRoot.addChild(objTrans);

        // Create a simple Shape3D node; add it to the scene graph.
        objTrans.addChild(new ColorCube(0.4));

	
        // Have Java 3D perform optimizations on this scene graph.
        objRoot.compile();

        return objRoot;
    }

    public RasterBugReport() {
        setLayout(new BorderLayout());
        GraphicsConfiguration config =
             SimpleUniverse.getPreferredConfiguration();

        Canvas3D c = new Canvas3D(config) {

			@Override
			public void postSwap()
			{
				super.postSwap();

				int width = getWidth() / 2;
				int height = getHeight() / 2;
				DepthComponentFloat depthComponent = new DepthComponentFloat(width, height);
				Point depthCapturePosition = new Point();
				depthCapturePosition.x = width;
				depthCapturePosition.y = height;

				GraphicsContext3D ctx = getGraphicsContext3D();
				depthComponent.setCapability(DepthComponent.ALLOW_DATA_READ);
				Raster ras = new Raster(new Point3f(0.0f, 0.0f, 0.0f),
						Raster.RASTER_DEPTH, depthCapturePosition.x, depthCapturePosition.y, depthComponent.getWidth(), depthComponent.getHeight(), null,
						depthComponent);
				ras.setCapability(Raster.ALLOW_DEPTH_COMPONENT_READ);
				ras.setCapability(Raster.ALLOW_IMAGE_READ);
				ras.setCapability(Raster.ALLOW_TYPE_READ);
				
				ctx.readRaster(ras);
			}
			
		};
        add("Center", c);

        SimpleUniverse u = new SimpleUniverse(c);
        BranchGroup scene = createSceneGraph();

        // This will move the ViewPlatform back a bit so the
        // objects in the scene can be viewed.
        u.getViewingPlatform().setNominalViewingTransform();

        u.addBranchGraph(scene);
    }

    //
    // The following allows HelloUniverse to be run as an application
    // as well as an applet
    //
    public static void main(String[] args) {
        new MainFrame(new RasterBugReport(), 256, 256);
    }
}