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
 * (C) Copyright 2011, by EADS France
 */

package org.jcae.vtk;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import vtk.vtkGlobalJavaHash;
import vtk.vtkUnstructuredGrid;
import vtk.vtkXMLUnstructuredGridWriter;

/**
 * Convert a Netgen .vol file to VTK
 * @author Jerome Robert
 */
public class Netgen2VTK {
	private static int[] readTriangles(BufferedReader in) throws IOException
	{
		String line;
		do
		{
			line = in.readLine();
		}
		while(!"surfaceelements".equals(line));
		int nbElement = Integer.parseInt(in.readLine());
		int[] toReturn = new int[nbElement*4];
		int k = 0;
		for(int i = 0; i<nbElement; i++)
		{
			String[] l = in.readLine().split(" +");
			toReturn[k++]=3;
			toReturn[k++]=Integer.parseInt(l[5]);
			toReturn[k++]=Integer.parseInt(l[6]);
			toReturn[k++]=Integer.parseInt(l[7]);
		}
		return toReturn;
	}

	private static int[] readTetras(BufferedReader in) throws IOException
	{
		String line;
		do
		{
			line = in.readLine();
		}
		while(!"volumeelements".equals(line));
		int nbElement = Integer.parseInt(in.readLine());
		int[] toReturn = new int[nbElement*5];
		int k = 0;
		for(int i = 0; i<nbElement; i++)
		{
			String[] l = in.readLine().split(" +");
			toReturn[k++]=4;
			toReturn[k++]=Integer.parseInt(l[6])-1;
			toReturn[k++]=Integer.parseInt(l[4])-1;
			toReturn[k++]=Integer.parseInt(l[5])-1;
			toReturn[k++]=Integer.parseInt(l[3])-1;
		}
		return toReturn;
	}

	private static double[] readPoints(BufferedReader in) throws IOException
	{
		String line;
		do
		{
			line = in.readLine();
		}
		while(!"points".equals(line));
		int nbElement = Integer.parseInt(in.readLine());
		double[] toReturn = new double[nbElement*3];
		int k = 0;
		for(int i = 0; i<nbElement; i++)
		{
			String[] l = in.readLine().split(" +");
			for(int j = 1 ; j<=3; j++)
				toReturn[k++]=Double.parseDouble(l[j]);
		}
		return toReturn;
	}

	public static vtkUnstructuredGrid convert(BufferedReader in) throws IOException
	{
		vtkUnstructuredGrid u = new vtkUnstructuredGrid();
		int[] cells = readTetras(in);
		u.SetCells(10, Utils.createCells(cells.length/5, cells));
		cells = null;
		u.SetPoints(Utils.createPoints(readPoints(in)));
		return u;
	}

	public static void convert(BufferedReader in, String out) throws IOException
	{
		vtkXMLUnstructuredGridWriter w = new vtkXMLUnstructuredGridWriter();
		w.SetDataModeToAscii();
		w.SetInput(convert(in));
		w.SetFileName(out);
		w.Write();
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				vtkGlobalJavaHash.GC();
			}
		});
	}

	public static void convert(String in, String out) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(in));
		convert(br, out);
		br.close();
	}

	private Netgen2VTK() {
	}
}
