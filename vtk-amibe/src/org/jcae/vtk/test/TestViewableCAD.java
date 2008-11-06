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
import org.jcae.vtk.FrustumPicker;
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
public class TestViewableCAD extends ViewableCAD implements SelectionListener, KeyListener
{
	public Canvas canvas;

	public TestViewableCAD(String filename)
	{
		super(filename);
	}
	
	public void selectionChanged(Viewable viewable)
	{
		System.out.println("DEBUG SELECTION EFFECTUEE");
		viewable.highlight();
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
				switch(getSelectionType())
				{
					case NODE:
						setSelectionType(SelectionType.CELL);
						break;
					case CELL:
						setSelectionType(SelectionType.POINT);
						canvas.lock();
						System.out.println("Capabilities : " + canvas.GetRenderWindow().ReportCapabilities());
						canvas.unlock();
						break;
					case POINT:
						setSelectionType(SelectionType.NODE);
						break;
				}
				break;
			case KeyEvent.VK_E:
				switch(getShapeTypeSelection())
				{
					case VERTEX:
						setShapeTypeSelection(ShapeType.EDGE);
						break;
					case EDGE:
						setShapeTypeSelection(ShapeType.FACE);
						break;
					case FACE:
						setShapeTypeSelection(ShapeType.VERTEX);
						break;
				}
				break;
	
			case KeyEvent.VK_A:
				setAppendSelection(!getAppendSelection());
				break;
				
/*			case KeyEvent.VK_I:
				System.out.println("TEST DATA !");
				performSelection(new FrustumPicker(Utils.retrieveCanvas(e), true,
					new Point(0,0), new Point(200,200)));
				//Utils.retrieveCanvas(e).Render();
				testDataChange();
				break;
*/		}
	}
	
	public static void main(String[] args)
	{
		Utils.loadVTKLibraries();
		if(args.length != 1)
		{
			System.err.println("This program receive one argument that is the path to the stp or brep file that will be used to construct the CAD model");
			return;
		}
		
		TestViewableCAD test = new TestViewableCAD(args[0]);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		View canvas = new View();
		vtkRenderer renderer = canvas.GetRenderer();
		test.canvas = canvas;
		canvas.add(test);
		canvas.addKeyListener(test);
		test.addCanvas(canvas);
		test.addSelectionListener(test);
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
		//canvas.getIren().SetPicker(new vtkAreaPicker());
		style.AutoAdjustCameraClippingRangeOn();
		canvas.getIren().SetInteractorStyle(style);
		renderer.ResetCamera();

		
		///home/ibarz/zebra/core/src/org/zebra/core/tag/tagError8.png
		/*vtkPNGReader reader = new vtkPNGReader();
		reader.SetFileName("/home/ibarz/zebra/core/src/org/zebra/core/tag/tagError8.png");
		vtkImageData image = reader.GetOutput();
		vtkTexture texture = new vtkTexture();
		texture.SetInput(image);
		image.Update();
		int[] dimensions = image.GetDimensions();
		
		
		int c = 30;
		int l = 30;
		
		
		
		vtkRenderer ren1 = canvas.GetRenderer();
		ren1.GetActiveCamera().SetPosition(c/2, l/2, Math.max(c, l)*2);
		ren1.GetActiveCamera().SetFocalPoint(c/2, l/2, 0.);
		
		for (int i = 0; i < c; ++i)
			for (int j = 0; j < l; ++j)
			{
				BillBoard tag = new BillBoard(texture, dimensions[0], dimensions[1]);
				tag.setPosition(i, j, 0);

				ren1.AddViewProp(tag.getActor());
			}*/

	
		frame.add(canvas, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.setSize(800, 600);

		// Delete all the java wrapped VTK objects
		//vtkGlobalJavaHash.DeleteAll();
	}
}
