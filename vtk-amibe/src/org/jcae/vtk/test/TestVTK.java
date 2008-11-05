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
import java.awt.Dimension;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;

import org.jcae.vtk.Canvas;
import org.jcae.vtk.Utils;
import org.jcae.vtk.ViewableCAD;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkConeSource;
import vtk.vtkInteractorStyleRubberBandPick;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkPolyDataMapper;
import vtk.vtkPropPicker;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

//TODO faire un rapport de bug sur la non scalabilite du nombre d'acteur
//TODO Tester la selection de cellule
//TODO Tester la selection de points
//TODO Ecrire un export UNV
//TODO Que sont les blocks et les hierarchies
//TODO faire un liste des utilisateur de VTK et paraview en pompant la mailling list
public class TestVTK
{
	static double[] tetraPoints = { 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1 };
	static int[] tetraCells = { 4, 0, 1, 2, 3 };


	static Canvas createCanvas()
	{
		final Canvas canvas = new Canvas();
		
		// a renderer for the data
		final vtkRenderer ren1 = canvas.GetRenderer();
		/*System.out.println(ren1.GetRenderWindow());
		System.out.println(ren1.GetRenderWindow().SupportsOpenGL());
		System.out.println(ren1.GetRenderWindow().ReportCapabilities());*/
		
		// create sphere geometry
		vtkConeSource cone = new vtkConeSource();
		cone.SetHeight( 3.0 );
		cone.SetRadius( 1.0 );
		cone.SetResolution( 1000 );

		// map to graphics objects
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		map.SetInput(cone.GetOutput());

		// actor coordinates geometry, properties, transformation
		vtkActor aSphere = new vtkActor();
		aSphere.SetMapper(map);
		aSphere.GetProperty().SetColor(0, 0, 1); // color blue			
		//ren1.AddActor(aSphere);
		//final CascadeActor c = new CascadeActor("/home/jerome/OCCShapeGal/66_shaver3.brep");
		ViewableCAD cascadeActorManager = new ViewableCAD("/home/ibarz/models/axe.brep");
                cascadeActorManager.addCanvas(canvas);
		ren1.SetBackground(1, 1, 1); // background color white
		canvas.setMinimumSize(new Dimension(0, 0));
		canvas.setPreferredSize(new Dimension(0, 0));
		vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();		
		style.AutoAdjustCameraClippingRangeOn();		
		
		final vtkInteractorStyleRubberBandPick rubberBand = new vtkInteractorStyleRubberBandPick();

		rubberBand.SetPickColor(1, 0, 0 );

		vtkRenderWindowInteractor interactor = ren1.GetRenderWindow().GetInteractor();		
		//rubberBand.AddObserver("AnyEvent", new EventHandler(), "handleEvent");
		interactor.AddObserver("PickEvent", new EventHandler(), "pickEvent");
		interactor.AddObserver("StartPickEvent", new EventHandler(), "startPickEvent");
		interactor.AddObserver("EndPickEvent", new EventHandler(), "endPickEvent");
		rubberBand.DebugOn();
		canvas.getIren().SetInteractorStyle(rubberBand);
		
		final vtkPropPicker picker = new vtkPropPicker();
		canvas.getIren().SetPicker(picker);
		//picker.SetDebug('y');
		
		//Class for rectangle selection:
		//vtkExtractSelectedFrustum vtkVisibleCellSelector
		//vtkInteractorStyleRubberBandPick vtkAreaPicker
		/*canvas.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{			
				if(picker.Pick(new double[]{e.getX(), e.getY(), 0}, canvas.GetRenderer())!=0)				
				{
					vtkProp p;
					vtkAssemblyPath path = picker.GetPath();
					if(path!=null)
					{
						p = path.GetLastNode().GetViewProp();
						vtkActor a=(vtkActor)p;						
						a.GetProperty().SetColor(1,0,0);
						ren1.GetRenderWindow().GetInteractor().RenderSecured();
					}
				}							
			}
		});*/
		
		ren1.ResetCamera();
		return canvas;
	}

	// the main function
	public static void main(String[] args)
	{
		Utils.loadVTKLibraries();
		try
		{			
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			JFrame frame = new JFrame();
			frame.setVisible(true);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JSplitPane split = new JSplitPane();
			vtkCanvas c1 = createCanvas();
			//vtkCanvas c2 = createCanvas();
			split.add(c1, JSplitPane.BOTTOM);
			//split.add(c2, JSplitPane.TOP);
			frame.add(split);			
			JMenuBar bar = new JMenuBar();
			JMenu menu = new JMenu("File");
			menu.add(new JMenuItem("Open"));
			menu.add(new JMenuItem("Exit"));
			bar.add(menu);
			frame.add(bar, BorderLayout.NORTH);
			split.setDividerLocation(0.5);
			System.out.println(c1.getPreferredSize() + " " + c1.getMinimumSize());
			//System.out.println(c2.getPreferredSize() + " " + c1.getMinimumSize());
			frame.setSize(800, 600);
			split.setDividerLocation(0.5);
		}
		catch (Exception ex)
		{
			Logger.getLogger(TestVTK.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
//
}
