/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.amibe.validation;

import gnu.trove.TDoubleArrayList;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

public class QualityDouble
{
	private static Logger logger=Logger.getLogger(QualityDouble.class);
	private TDoubleArrayList data;
	private QualityProcedure qproc;
	private int [] sorted;
	private int layers = 0;
	private double vmin, vmax;

	public QualityDouble(int n)
	{
		data = new TDoubleArrayList(n);
	}
	
	public void setQualityProcedure(QualityProcedure q)
	{
		qproc = q;
	}
	
	public void compute(Object x)
	{
		assert qproc != null;
		data.add(qproc.quality(x));
	}
	
	public void add(double x)
	{
		data.add(x);
	}
	
	public void finish()
	{
		qproc.finish(data);
	}
	
	public int size()
	{
		return data.size();
	}
	
	//  TDoubleArrayList already extends min() and max()
	public void split(int n)
	{
		layers = n;
		if (layers <= 0)
			return;
		int nrTotal = data.size();
		//  min() and max() methods are buggy in trove 1.0.2
		vmin = Double.MAX_VALUE;
		vmax = Double.MIN_VALUE;
		for (int i = 0; i < nrTotal; i++)
		{
			double val = data.get(i);
			if (vmin > val)
				vmin = val;
			if (vmax < val)
				vmax = val;
		}
		double delta = (vmax - vmin) / ((double) layers);
		sorted = new int[layers];
		for (int i = 0; i < layers; i++)
			sorted[i] = 0;
		for (int i = 0; i < nrTotal; i++)
		{
			int cell = (int) ((data.get(i) - vmin) / delta + 0.001);
			if (cell < 0)
				cell = 0;
			if (cell >= layers)
				cell = layers - 1;
			sorted[cell]++;
		}
	}
	
	public void split(double v1, double v2, int n)
	{
		layers = n;
		vmin = v1;
		vmax = v2;
		if (layers <= 0)
			return;
		int nrTotal = data.size();
		double delta = (vmax - vmin) / ((double) layers);
		sorted = new int[layers];
		for (int i = 0; i < layers; i++)
			sorted[i] = 0;
		for (int i = 0; i < nrTotal; i++)
		{
			int cell = (int) ((data.get(i) - vmin) / delta + 0.001);
			if (cell < 0)
				cell = 0;
			if (cell >= layers)
				cell = layers - 1;
			sorted[cell]++;
		}
	}
	
	public void printLayers()
	{
		if (layers <= 0)
			return;
		int nrTotal = data.size();
		double delta = (vmax - vmin) / ((double) layers);
		for (int i = 0; i < layers; i++)
		{
			System.out.println(""+(vmin+i*delta)+" ; "+(vmin+(i+1)*delta)+" "+sorted[i]+" ("+(((float) 100.0 * sorted[i])/((float) nrTotal))+"%)");
		}
		System.out.println("total: "+nrTotal);
	}
	
	public void printMeshBB(String file)
	{
		int nrTotal = data.size();
		try
		{
			PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
			out.println("3 1 "+nrTotal+" "+qproc.getType());
			for (int i = 0; i < nrTotal; i++)
				out.println(""+data.get(i));
			out.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("Cannot write into: "+file);
		}
	}
}
