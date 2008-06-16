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

package org.jcae.vtk;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.NoSuchElementException;
import javax.imageio.ImageIO;
import vtk.vtkActor;
import vtk.vtkAssembly;
import vtk.vtkCanvas;
import vtk.vtkCellArray;
import vtk.vtkConeSource;
import vtk.vtkDataArray;
import vtk.vtkDoubleArray;
import vtk.vtkFloatArray;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkPNGWriter;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkPolyLine;
import vtk.vtkProperty;
import vtk.vtkRenderer;
import vtk.vtkTransform;
import vtk.vtkTransformFilter;
import vtk.vtkWindowToImageFilter;

/**
 *
 * @author Jerome Robert
 */
public class Utils
{

	/**
	 * Use instead Arrays.method of java library
	 * @param tab
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static String stringDoubleTab(double[] tab)
	{
		String rep = "";
		for (double i : tab)
			rep = rep + " " + i;

		return rep;
	}
	
	public static Canvas retrieveCanvas(ComponentEvent e)
	{
		Component c = e.getComponent();
		if(c instanceof Canvas)
			return (Canvas)c;
		else
			throw new NoSuchElementException("Found "+c.getClass()+
				" when "+vtkCanvas.class+" expected.");
	}
	
	/**
	 * Use instead Arrays.method of java library
	 * @param tab
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static String stringFloatTab(float[] tab)
	{
		String rep = "";
		for (float i : tab)
			rep = rep + " " + i;

		return rep;
	}
		/**
	 * Use instead Arrays.method of java library
	 * @param tab
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static String stringIntTab(int[] tab)
	{
		String rep = "";
		for (int i : tab)
			rep = rep + " " + i;

		return rep;
	}

	/**
	 * Use vtkPropertySetColor instead
	 * @param actor
	 * @param color
	 * @deprecated
	 */
	@Deprecated
	public static void setColorActor(vtkActor actor, Color color)
	{
		actor.GetProperty().SetColor((double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255.);
		actor.GetProperty().SetOpacity((double) color.getAlpha() / 255.);
	}
	
	public static void vtkPropertySetColor(vtkProperty property, Color color)
	{
		property.SetColor((double) color.getRed() / 255., (double) color.getGreen() / 255., (double) color.getBlue() / 255.);
		property.SetOpacity((double) color.getAlpha() / 255.);
	}
			
	/**
	 * Use instead Arrays.method of java library
	 * @param tab
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public static <T> String stringTab(T[] tab)
	{
		String rep = "";
		for (T i : tab)
			rep = rep + " " + i;

		return rep;
	}

	public static void loadVTKLibraries()
	{
		System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary("vtkCommonJava");
		System.loadLibrary("vtkFilteringJava");
		System.loadLibrary("vtkIOJava");
		System.loadLibrary("vtkImagingJava");
		System.loadLibrary("vtkGraphicsJava");
		System.loadLibrary("vtkRenderingJava");
	}

	/** Create a dummy canvas with single actor */
	public static vtkCanvas createDummyCanvas(vtkActor actor)
	{
		final vtkCanvas canvas = new vtkCanvas()
		{

			/** Workaround for http://www.vtk.org/Bug/view.php?id=6268 */
			@Override
			public void setSize(int x, int y)
			{
				super.setSize(x, y);
				Lock();
				rw.SetSize(x, y);
				iren.SetSize(x, y);
				iren.ConfigureEvent();
				UnLock();
			}
		};
		// a renderer for the data
		final vtkRenderer ren1 = canvas.GetRenderer();

		ren1.AddActor(actor);
		// background color white
		ren1.SetBackground(1, 1, 1);
		// Make the canvas resizable with a splitter
		//layout
		canvas.setMinimumSize(new Dimension(0, 0));
		canvas.setPreferredSize(new Dimension(0, 0));
		ren1.ResetCamera();
		return canvas;
	}
	
	/**
	 * Transform z from eyes coordinates to screen coordinates (the z-buffer value)
	 * @param ze the z in eyes coordinnates
	 * @param n near plane distance
	 * @param f far plane distance
	 * @return
	 */
	public static double EyeZToNormalizedEyeZ(double ze, double n, double f)
	{
		return (f + n) / (f - n) - 2 * f * n / (f - n) / ze;
	}
	
	/**
	 * Used to compute the tolerance for VTK picking
	 * @param canvas 
	 * @param tolerance in percentage of error of distance in function of (f - n)
	 * @return the epsilon error in z space coordinates (z in [0,1])
	 */
	public static double computeTolerance(vtkCanvas canvas, double tolerance)
	{
		double[] range = canvas.GetRenderer().GetActiveCamera().GetClippingRange();
		double n = range[0];
		double f = range[1];
		double distance = canvas.GetRenderer().GetActiveCamera().GetDistance();
		
		double distanceTolerance = tolerance * (f - n);

		// Compute the z of the focal point (point of interest)
		double d1 = EyeZToNormalizedEyeZ(distance, n, f);

		// Compute the z of the distance tolerance
		// If we are before the far we take distance + distanceTolerance else we take -
		double d2 = (distance + distanceTolerance < f) ? distance + distanceTolerance : distance - distanceTolerance;
		d2 = EyeZToNormalizedEyeZ(d2, n, f);

		// Compute the delta tolerance for the z-buffer value
		return Math.abs(d1 - d2);
	}
	
	/**
	 * Compute the epsilon of the depth in screen coordinates (z mapped to [0,1])
	 * giving a distance in camera space coordinates.
	 * @param distance the distance on z  to the camera where the delta is computed.
	 * @param delta the distance in camera space coordinates.
	 * @param n near plane distance
	 * @param f far plane distance
	 * @return
	 * @see Mathematics for 3D Game Programming and Compute Graphics book.
	 */
	public static double computeZDistanceFromCameraSpaceToScreenSpace(double distance, double delta, double n, double f)
	{
		return (2 * f * n * delta) / ((f + n) * distance * (distance + delta));
	}
	
	/**
	 * Compute the epsilon error on screen coordinates of the Z-buffer 
	 * @param nbits the number of bits of the Z-buffer

	 * @return
	 */
	public static double computeZBufferError(int nbit)
	{
		return 1. / (Math.pow(2., (double)nbit) - 1.);
	}
	
	public static void addOffSet(int[] indices, int offSet)
	{
		for(int i = 0 ; i < indices.length ; ++i)
			indices[i] += offSet;
	}
	
	public static double[] computeVerticesFrustum(double x0, double y0, double x1, double y1, vtkRenderer renderer)
	{
		double X0 = Math.min(x0, x1);
		double Y0 = Math.min(y0, y1);
		double X1 = Math.max(x0, x1);
		double Y1 = Math.max(y0, y1);
		
		System.out.println("X0 : " + X0);
		System.out.println("Y0 : " + Y0);
		System.out.println("X1 : " + X1);
		System.out.println("Y1 : " + Y1);
		
		//compute world coordinates of the pick volume 
		double[] verts = new double[32];
		double[] vertice = new double[4];

		renderer.SetDisplayPoint(X0, Y0, 0);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 0, 4);
		renderer.SetDisplayPoint(X0, Y0, 1);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 4, 4);
		
		renderer.SetDisplayPoint(X0, Y1, 0);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 8, 4);

		renderer.SetDisplayPoint(X0, Y1, 1);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 12, 4);

		renderer.SetDisplayPoint(X1, Y0, 0);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 16, 4);

		renderer.SetDisplayPoint(X1, Y0, 1);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 20, 4);

		renderer.SetDisplayPoint(X1, Y1, 0);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 24, 4);		
		
		renderer.SetDisplayPoint(X1, Y1, 1);
		renderer.DisplayToWorld();
		vertice = renderer.GetWorldPoint();
		System.arraycopy(vertice, 0, verts, 28, 4);
		
		System.out.println("length vertice : " + vertice.length);
		
		System.out.println("verts : " + Arrays.toString(verts));
		
		return verts;
	}
	
	public static double getOffSetFactor()
	{
		return 2.;
	}
	
	public static double getOffSetValue()
	{
		return 2.;
	}
	
	/**
	 * Take a screenshot of the canvas and use ScreenShotLister to get the BufferedImage
	 * WARNING : Make sure the call of this function is made in the same thread than the
	 * principal thread of AWT. If not you will have a black screenshot.
	 * @param canvas
	 * @param listener
	 */
	public static BufferedImage takeScreenShot(final vtkCanvas canvas)
	{
		if (!canvas.isWindowSet())
			System.err.println("Attention : Lors de la prise de screenshot le windows n'?tait pas set !");
		
		final vtkWindowToImageFilter w2i = new vtkWindowToImageFilter();
		final vtkPNGWriter writer = new vtkPNGWriter();
		w2i.SetInput(canvas.getIren().GetRenderWindow());
		writer.SetInputConnection(w2i.GetOutputPort());

		BufferedImage buffer = null;
		File file = null;
		try
		{
			file = File.createTempFile("screen", "png");
			writer.SetFileName(file.getAbsolutePath());
			canvas.lock();
			w2i.Update();
			canvas.unlock();
			writer.Write();
			buffer = ImageIO.read(file);
		} catch (Exception e)
		{
			System.err.println("Error writing the screenshot : " + e.getLocalizedMessage());
		}

		return buffer;
	}

	/** Create a dummy cone actor
	 * @return 
	 */
	public static vtkActor createDummyActor()
	{
		// create sphere geometry
		vtkConeSource cone = new vtkConeSource();
		cone.SetHeight(3.0);
		cone.SetRadius(1.0);
		cone.SetResolution(10);

		// map to graphics objects
		vtkPolyDataMapper map = new vtkPolyDataMapper();
		map.SetInput(cone.GetOutput());

		// actor coordinates geometry, properties, transformation
		vtkActor aSphere = new vtkActor();
		aSphere.SetMapper(map);
		aSphere.GetProperty().SetColor(0, 0, 1); // color blue
		return aSphere;
	}

	/** Create a dummy cone actor */
	public static vtkAssembly createDummyAssembly(int n, int m)
	{
		vtkAssembly assembly = new vtkAssembly();
		assembly.SetPickable(0);
		for (int i = 0; i < n; i++)
			for (int j = 0; j < m; j++)
			{
				vtkActor actor = createDummyActor();
				actor.SetPosition(4 * i, 4 * j, 0);
				actor.SetPickable(1);
				assembly.AddPart(actor);
			}
		return assembly;
	}

	/** Same as createDummyAssembly, but add actors directly in the scene */
	public static void addDummyCones(int n, int m, vtkRenderer renderer)
	{
		for (int i = 0; i < n; i++)
			for (int j = 0; j < m; j++)
			{
				// create sphere geometry
				vtkConeSource cone = new vtkConeSource();
				cone.SetHeight(3.0);
				cone.SetRadius(1.0);
				cone.SetResolution(50);
				vtkTransformFilter filter = new vtkTransformFilter();
				vtkTransform transform = new vtkTransform();
				transform.Translate(4 * (i + 1), 4 * (j + 1), 0);
				filter.SetTransform(transform);
				filter.SetInput(cone.GetOutput());
				vtkPolyDataMapper map = new vtkPolyDataMapper();
				map.SetInput((vtkPolyData) filter.GetOutput());
				vtkActor actor = new vtkActor();
				actor.SetMapper(map);

				if (i == 0 && j == 0)
					actor.GetProperty().SetColor(0, 1, 0);
				actor.PickableOn();
				renderer.AddActor(actor);
			}
	}

			
	
	/** Create vertices cells to be used as input from createCells */
	public static int[] createVerticesCells(int numberOfVertices)
	{
		int[] toReturn = new int[numberOfVertices * 2];
		int k = 0;
		for (int i = 0; i < numberOfVertices; i++)
		{
			toReturn[k++] = 1;
			toReturn[k++] = i;
		}
		return toReturn;
	}

	/** Create a vtkPoints array from floats */
	public static vtkPoints createPoints(float[] points)
	{
		vtkPoints vtkPoints = new vtkPoints();
		vtkFloatArray d = new vtkFloatArray();
		d.SetJavaArray(points);
		d.SetNumberOfComponents(3);
		vtkPoints.SetData(d);
		return vtkPoints;
	}

	/** Create a vtkPoints array from doubles */
	public static vtkPoints createPoints(double[] points)
	{
		vtkPoints vtkPoints = new vtkPoints();
		vtkDoubleArray d = new vtkDoubleArray();
		d.SetJavaArray(points);
		d.SetNumberOfComponents(3);
		vtkPoints.SetData(d);
		return vtkPoints;
	}
	
	public static int[] createQuadsCells(int nbrOfCells)
	{
		int[] indices = new int[nbrOfCells * 5];
		int offSet = 0;
		for(int i = 0 ; i < indices.length ; )
		{
			indices[i++] = 4;
			indices[i++] = offSet++;
			indices[i++] = offSet++;
			indices[i++] = offSet++;
			indices[i++] = offSet++;
		}
		
		return indices;
	}
	
	/** Create an index array for quads to be used as input from createCells */
	public static int[] createQuadsCells(int[] cells, int offSetID)
	{
		int k = 0;
		int nCell = cells.length / 4;
		int[] fCells = new int[nCell * 5];
		for (int i = 0; i < nCell * 4;)
		{
			fCells[k++] = 4;
			fCells[k++] = cells[i++] + offSetID;
			fCells[k++] = cells[i++] + offSetID;
			fCells[k++] = cells[i++] + offSetID;
			fCells[k++] = cells[i++] + offSetID;
		}
		
		return fCells;
	}
	
	/**
	 * Compute the VTK tolerance for point pickers to have a box of size size in pixel.
	 * @param renderer
	 * @return
	 */	public static double getTolerance(vtkRenderer renderer, int pixelSize)
	{
		int[] size = renderer.GetSize();
		// The diagonal of the renderer
		double diagonal = Math.sqrt(size[0]*size[0] + size[1]*size[1]);
	
		return ((double) pixelSize) / (diagonal * 2.);
	}
	
	public static int[] createTriangleCells(int nbrOfCells)
	{
		int[] indices = new int[nbrOfCells * 4];
		int offSet = 0;
		for(int i = 0 ; i < indices.length ; )
		{
			indices[i++] = 3;
			indices[i++] = offSet++;
			indices[i++] = offSet++;
			indices[i++] = offSet++;
		}
		
		return indices;
	}
	/** Create an index array for triangles to be used as input from createCells */
	public static int[] createTriangleCells(int[] cells, int offSetID)
	{
		int k = 0;
		int nCell = cells.length / 3;
		int[] fCells = new int[nCell * 4];
		for (int i = 0; i < nCell * 3; i += 3)
		{
			fCells[k++] = 3;
			fCells[k++] = cells[i] + offSetID;
			fCells[k++] = cells[i + 1] + offSetID;
			fCells[k++] = cells[i + 2] + offSetID;
		}
		return fCells;
	}

	/** Create an index array for beams to be used as input from createCells */
	public static int[] createBeamCells(int numberOfBeam)
	{
		int k = 0;
		int[] fCells = new int[3 * numberOfBeam];
		for (int i = 0; i < fCells.length ; )
		{
			fCells[i++] = 2;
			fCells[i++] = k++;
			fCells[i++] = k++;
		}
		return fCells;
	}

	/**
	 * Create an index array for beams to be used as input from createCells
	 * change {a, b, c, d} to {2, a, b, 2, c, d}
	 */
	public static int[] createBeamCells(int[] beams)
	{
		int numberOfBeam = beams.length / 2;
		int k = 0;
		int j = 0;
		int[] fCells = new int[3 * numberOfBeam];
		for (int i = 0; i < numberOfBeam; i++)
		{
			fCells[k++] = 2;
			fCells[k++] = beams[j++];
			fCells[k++] = beams[j++];
		}
		return fCells;
	}

	/** Create a vtkCellArray from indexes */
	public static vtkCellArray createCells(int cellNumber, int[] cells)
	{
		vtkCellArray vtkCells = new vtkCellArray();
		vtkIdTypeArray array = new vtkIdTypeArray();
		vtkIntArray intArray = new vtkIntArray();
		intArray.SetJavaArray(cells);
		array.DeepCopy(intArray);
		vtkCells.SetCells(cellNumber, array);
		return vtkCells;
	}

	/** Create a vtkPolyLine from indexes */
	public static vtkPolyLine createPolyLine(int nbOfPoints, int offSet)
	{
		vtkPolyLine line = new vtkPolyLine();

		// TODO: do not do it in java
		line.GetPointIds().SetNumberOfIds(nbOfPoints);
		for (int i = 0; i < nbOfPoints; ++i)
			line.GetPointIds().SetId(i, i + offSet);

		return line;
	}

	public static int[] getValues(vtkIdTypeArray idarray)
	{
		vtkIntArray iarray = new vtkIntArray();
		iarray.DeepCopy(idarray);
		return iarray.GetJavaArray();
	}

	public static vtkIdTypeArray setValues(int[] values)
	{
		vtkIntArray iarray = new vtkIntArray();
		iarray.SetJavaArray(values);
		vtkIdTypeArray array = new vtkIdTypeArray();
		array.DeepCopy(iarray);

		System.out.println("values : " + values.length);
		System.out.println("iarray : " + array.GetNumberOfTuples());

		return array;
	}

	public static int[] getValues(vtkCellArray array)
	{
		return getValues(array.GetData());
	}

	public static float[] getValues(vtkPoints points)
	{
		vtkDataArray array = points.GetData();
		//TODO not sure it's always a float[] array
		return ((vtkFloatArray) array).GetJavaArray();
	}

	/**
	 * Remove size of cell from the cell array. 
	 * Convert {2, 4, 5, 3, 7, 8, 9} to {4, 5, 7, 8, 9}.
	 * Use it only on cell array containing only one type of cells
	 */
	public static int[] stripCellArray(int[] indices)
	{
		//Compute the size of the ouput array
		int t = indices[0];
		int tp = t + 1;
		int n = indices.length / tp;
		int[] toReturn = new int[n * t];
		for (int i = 0; i < n; i++)
			System.arraycopy(indices, i * tp + 1, toReturn, i * t, t);
		return toReturn;
	}

	public static double[] floatToDouble(float[] array)
	{
		double[] toReturn = new double[array.length];
		for (int i = 0; i < array.length; i++)
			toReturn[i] = array[i];
		return toReturn;
	}
}
