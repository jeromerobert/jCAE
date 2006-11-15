/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

import gnu.trove.TFloatArrayList;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import org.apache.log4j.Logger;

/**
 * Manage statistics for quality values.
 *
 * This class allows easy computation of mesh quality.  A criterion
 * factor can be selected, then quality is computed and results are
 * printed on screen or in files.  Quality values are stored in a list
 * of floats.
 *
 * Example:
 * <pre>
 *    QualityFloat data = new QualityFloat();
 *    data.setQualityProcedure(new DihedralAngle());
 *    for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
 *    {
 *        Triangle f = (Triangle) itf.next();
 *        data.compute(f);
 *    }
 *    //  Print all results in the BB mesh format.
 *    data.printMeshBB("foo.bb");
 *    //  Gather results into 10 blocks...
 *    data.split(10);
 *    //  ... and display them on screen.
 *    data.printLayers();
 * </pre>
 */
public class QualityFloat
{
	private static Logger logger=Logger.getLogger(QualityFloat.class);
	private TFloatArrayList data;
	private QualityProcedure qproc;
	private int [] sorted;
	private int layers = 0;
	private float vmin, vmax;
	private float qmin, qmax, qavg;

	public QualityFloat()
	{
		data = new TFloatArrayList();
	}
	
	/**
	 * Create a new <code>QualityFloat</code> instance 
	 *
	 * @param n  initial capacity of the list.
	 */
	public QualityFloat(int n)
	{
		data = new TFloatArrayList(n);
	}
	
	/**
	 * Define the procedure which will compute quality values.
	 *
	 * @param q  the procedure which will compute quality values.
	 */
	public void setQualityProcedure(QualityProcedure q)
	{
		qproc = q;
		qproc.bindResult(data);
	}
	
	/**
	 * Compute the quality of an object and add it to the list.
	 *
	 * @param x  the object on which quality is computed.
	 */
	public void compute(Object x)
	{
		assert qproc != null;
		data.add(qproc.quality(x));
	}
	
	/**
	 * Add a value to the list.
	 *
	 * @param x  the value to add to the list.
	 */
	public void add(float x)
	{
		data.add(x);
	}
	
	/**
	 * Call the {@link QualityProcedure#finish} procedure.
	 */
	public void finish()
	{
		qproc.finish();
	}
	
	/**
	 * Return the number of quality values.
	 *
	 * @return the number of quality values.
	 */
	public int size()
	{
		return data.size();
	}
	
	/**
	 * Normalize quality target.  This method divides all values
	 * by the given factor.  This is useful to scale quality
	 * factors so that they are in the range </code>[0..1]</code>.
	 *
	 * @param factor   the scale factor.
	 */
	public void setTarget(float factor)
	{
		int nrTotal = data.size();
		if (factor == 0.0f)
			return;
		else
			factor = 1.0f/factor;
		for (int i = 0; i < nrTotal; i++)
		{
			float val = data.get(i);
			val *= factor;
			data.set(i, val);
		}
	}
	
	/**
	 * Split quality values into buckets.  The minimal and
	 * maximal quality values are computed, this range is divided
	 * into <code>n</code> subsegments of equal length, and 
	 * the number of quality values for each subsegment is
	 * computed.  These numbers can then be displayed by
	 * {@link #printLayers}.
	 *
	 * @param n  the desired number of subsegments.
	 */
	public void split(int n)
	{
		layers = n;
		if (layers <= 0)
			return;
		int nrTotal = data.size();
		//  min() and max() methods are buggy in trove 1.0.2
		vmin = Float.MAX_VALUE;
		vmax = Float.MIN_VALUE;
		qavg = 0.0f;
		for (int i = 0; i < nrTotal; i++)
		{
			float val = data.get(i);
			qavg += val / nrTotal;
			if (vmin > val)
				vmin = val;
			if (vmax < val)
				vmax = val;
		}
		qmin = vmin;
		qmax = vmax;
		float delta = (vmax - vmin) / ((float) layers);
		// In printLayers:
		//   sorted[0]: number of points with value < vmin
		//   sorted[layers+1]: number of points with value > vmax
		sorted = new int[layers+2];
		for (int i = 0; i < layers + 2; i++)
			sorted[i] = 0;
		for (int i = 0; i < nrTotal; i++)
		{
			float val = data.get(i);
			int cell = (int) ((val - vmin) / delta + 1.001f);
			assert cell > 0 && cell <= layers;
			sorted[cell]++;
		}
	}
	
	/**
	 * Split quality values into buckets.  The range between minimal
	 * and maximal quality values is divided into <code>n</code>
	 * subsegments of equal length, and the number of quality values
	 * for each subsegment is computed.  These numbers can then be
	 * displayed by {@link #printLayers}.
	 *
	 * @param v1  minimal value to consider.
	 * @param v2  maximal value to consider.
	 * @param n  the desired number of subsegments.
	 */
	public void split(float v1, float v2, int n)
	{
		layers = n;
		vmin = v1;
		vmax = v2;
		qavg = 0.0f;
		qmin = Float.MAX_VALUE;
		qmax = Float.MIN_VALUE;
		if (layers <= 0)
			return;
		int nrTotal = data.size();
		//  The last cell is for v >= vmax
		float delta = (vmax - vmin) / ((float) layers);
		sorted = new int[layers+2];
		for (int i = 0; i < layers+2; i++)
			sorted[i] = 0;
		for (int i = 0; i < nrTotal; i++)
		{
			float val = data.get(i);
			if (qmin > val)
				qmin = val;
			if (qmax < val)
				qmax = val;
			qavg += data.get(i) / nrTotal;
			int cell = (int) ((val - vmin) / delta + 1.001f);
			if (cell < 0)
				cell = 0;
			else if (cell >= layers + 1)
			{
				if (val > vmax)
					cell = layers + 1;
				else
					cell = layers;
			}
			sorted[cell]++;
		}
	}
	
	/**
	 * Display statistics about quality values.
	 */
	public void printLayers()
	{
		if (layers <= 0)
		{
			logger.error("split() method must be called before printLayers()");
			return;
		}
		int nrTotal = data.size();
		float delta = (vmax - vmin) / ((float) layers);
		if (sorted[0] > 0)
			System.out.println(" < "+vmin+" "+sorted[0]+" ("+(((float) 100.0 * sorted[0])/((float) nrTotal))+"%)");
		for (int i = 0; i < layers; i++)
		{
			System.out.println(""+(vmin+i*delta)+" ; "+(vmin+(i+1)*delta)+" "+sorted[i+1]+" ("+(((float) 100.0 * sorted[i+1])/((float) nrTotal))+"%)");
		}
		if (sorted[layers+1] > 0)
			System.out.println(" > "+vmax+" "+sorted[layers+1]+" ("+(((float) 100.0 * sorted[layers+1])/((float) nrTotal))+"%)");
		System.out.println("total: "+nrTotal);
		System.out.println("qmin: "+qmin);
		System.out.println("qmax: "+qmax);
		System.out.println("qavg: "+qavg);
	}
	
	/**
	 * Write quality values into a file.  They are stored in the
	 * BB medit format, in the same order as they have been
	 * computed.  This means that a mesh file had been written with
	 * the same order.
	 *
	 * @param file   name of the output file
	 */
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
