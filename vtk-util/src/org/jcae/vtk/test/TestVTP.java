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

package org.jcae.vtk.test;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.Utils;
import vtk.vtkPolyData;
import vtk.vtkRenderer;
import vtk.vtkXMLPolyDataWriter;

/**
 *
 * @author ibarz
 */
public class TestVTP {
public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Canvas canvas = new Canvas();
        vtkRenderer renderer = canvas.GetRenderer();
        
		float[] points = {
			0.f, 0.f, 0.f, // A - 0
			1.f, 0.f, 0.f, // B - 1
			0.f, 1.f, 0.f, // C - 2
			0.f, 0.f, 1.f, // D - 3
			0.f, 0.f, -1.f, // E - 4
			-1.f, -1.f, 0.f, // F - 5
			-1.f, 0.f, 0.f, // G - 6
			1.f, 1.f, 0.f, // H - 7
			0.f, -1.f, 0.f, // I - 8
		};
		
		int[] cells = {
			3, 0, 1, 3, // ABD
			3, 0, 1, 4, // ABE
			3, 0, 3, 6, // ADG
			4, 0, 1, 7, 2, // ABHC
			4, 0, 6, 5, 8 // AGFI
		};
		
		vtkPolyData data = new vtkPolyData();
		data.SetPoints(Utils.createPoints(points));
		data.SetPolys(Utils.createCells(5, cells));
		
		vtkXMLPolyDataWriter writer = new vtkXMLPolyDataWriter();
		writer.SetDataModeToAscii();
		writer.SetInputDataObject(data);
		writer.SetFileName("data.vtp");
		writer.Write();
		
        frame.add(canvas, BorderLayout.CENTER);
        frame.setVisible(true);
        frame.setSize(800, 600);
		

    // Delete all the java wrapped VTK objects
    //vtkGlobalJavaHash.DeleteAll();
    }
}
