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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.vtk.test;

import vtk.vtkDataArray;
import vtk.vtkDoubleArray;
import vtk.vtkQuadric;
import vtk.vtkSampleFunction;

public class BugWeakRef
{
    static
	{
		System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary("vtkCommonJava");
		System.loadLibrary("vtkImagingJava");
	}
	public static void main(String[] args)
	{
		vtkDataArray pseudoStatic = new vtkDoubleArray();
		while(true)
		{
			pseudoStatic.CreateDataArray(8).Delete();

			vtkQuadric quadric = new vtkQuadric();
			vtkSampleFunction sample = new vtkSampleFunction();
			sample.SetSampleDimensions(30, 30, 30);
			sample.SetImplicitFunction(quadric);
			sample.GetImplicitFunction();
		}
	}
}
