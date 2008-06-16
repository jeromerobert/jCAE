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
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import org.jcae.vtk.Canvas;
import org.jcae.vtk.SelectionListener;
import org.jcae.vtk.Utils;
import org.jcae.vtk.View;
import org.jcae.vtk.Viewable;
import org.jcae.vtk.ViewableCAD;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderer;

/**
 *
 * @author Jerome Robert
 */
public class TestRubberBand implements SelectionListener, KeyListener {
	public ViewableCAD viewable;
	public Canvas canvas;

	public void selectionChanged(Viewable viewable)
	{
		System.out.println("DEBUG SELECTION EFFECTUEE");
		viewable.highLight();
	}
	public void keyReleased(KeyEvent e)
	{
		// DO Nothing
	}

	public void keyTyped(KeyEvent e)
	{
		// DO Nothing
	}

	public void keyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{			
			case KeyEvent.VK_G:
				viewable.setSelectionType(Viewable.SelectionType.NODE);
				break;
			case KeyEvent.VK_F:
				viewable.setSelectionType(Viewable.SelectionType.CELL);
				break;
			case KeyEvent.VK_V:
				viewable.setSelectionType(Viewable.SelectionType.POINT);
				canvas.lock();
				System.out.println("Capabilities : " + canvas.GetRenderWindow().ReportCapabilities());
				canvas.unlock();
				break;
				case KeyEvent.VK_O:
					//viewable.hideMesh();
					break;
			case KeyEvent.VK_I:
				System.out.println("TEST DATA !");
				viewable.surfaceSelection(Utils.retrieveCanvas(e), new Point(0,0), new Point(200,200));
				//Utils.retrieveCanvas(e).Render();
				viewable.testDataChange();
				break;
				
		}
	}

    public static void main(String[] args) {
		TestRubberBand test = new TestRubberBand();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        View canvas = new View();
        vtkRenderer renderer = canvas.GetRenderer();
        ViewableCAD rbh = new ViewableCAD(args[0]);
		test.canvas = canvas;
		test.viewable = rbh;
		canvas.add(rbh);
		canvas.addKeyListener(test);
		rbh.addCanvas(canvas);
		rbh.addSelectionListener(test);
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
        //canvas.getIren().SetPicker(new vtkAreaPicker());
        style.AutoAdjustCameraClippingRangeOn();
        canvas.getIren().SetInteractorStyle(style);
        renderer.ResetCamera();

        frame.add(canvas, BorderLayout.CENTER);
        frame.setVisible(true);
        frame.setSize(800, 600);

    // Delete all the java wrapped VTK objects
    //vtkGlobalJavaHash.DeleteAll();
    }
}
