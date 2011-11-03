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
 * 
 */
 
package org.jcae.vtk.test;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.jcae.opencascade.jni.BRepBuilderAPI_MakeEdge;
import org.jcae.opencascade.jni.BRepPrimAPI_MakeRevol;
import org.jcae.opencascade.jni.GeomAPI_Interpolate;
import org.jcae.opencascade.jni.Geom_BSplineCurve;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.vtk.Utils;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableCAD;

import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Jens Schmidt
 */

/*
 * Test for BRepPrimAPI_MakeRevol
 * 
 * first revol with straight line
 * second revol with spline
 * change test at the bottom of this file
 * 
 * line from [0,0,0] to [10,10,0]
 * axis 0,0,0,1,0,0
 * 
 * spline with [0,0,0] [0,10,0] [10,10,0]
 * axis 0,0,0,1,0,0
 * 
 */

public class TestMakeRevol {

	public static void main(String[] args) {
		Utils.loadVTKLibraries();

		//test with line
		double[] lp1 = new double[]{0,0,0};
		double[] lp2 = new double[]{10,10,0};
		TopoDS_Edge line = (TopoDS_Edge) new BRepBuilderAPI_MakeEdge(lp1, lp2).shape();

		double[] axis = new double[]{0,0,0,1,0,0};

		TopoDS_Shape revLine = new BRepPrimAPI_MakeRevol(line, axis, Math.toRadians(360)).shape();
		ViewableCAD viewableLine = new ViewableCAD(revLine);

		//test with spline
		//		double[] p1 = new double[]{0,0,0};
		//		double[] p2 = new double[]{0,10,0};
		//		double[] p3 = new double[]{10,10,0};

		double[] points = new double[]{0,0,0,0,10,0,10,10,0};

		GeomAPI_Interpolate repSpline = new GeomAPI_Interpolate(points, false, 1E-7);
		repSpline.Perform();
		Geom_BSplineCurve s = repSpline.Curve();
		TopoDS_Edge spline = (TopoDS_Edge) new BRepBuilderAPI_MakeEdge(s).shape();

		TopoDS_Shape revSpline = new BRepPrimAPI_MakeRevol(spline, axis, Math.toRadians(360)).shape();
		ViewableCAD viewableSpline = new ViewableCAD(revSpline);

		//graphix yadda yadda
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
		style.AutoAdjustCameraClippingRangeOn();
		View viewVTK = new View();
		viewVTK.getIren().SetInteractorStyle(style);
		viewVTK.GetRenderWindow().LineSmoothingOn();
		vtkRenderer renderer = viewVTK.GetRenderer();
		renderer.SetBackground(0.1, 0.1, 0.1);
		renderer.SetBackground2(0.7, 0.7, 0.7);
		renderer.GradientBackgroundOn();
		renderer.ResetCamera();
		JFrame frame = new JFrame();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.add(viewVTK, BorderLayout.CENTER);
		frame.doLayout();

		//change here:
		
		viewVTK.add(viewableLine);
//		viewVTK.add(viewableSpline);

	}

}
