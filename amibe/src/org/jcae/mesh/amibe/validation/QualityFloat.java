/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

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
	private static final Logger logger=Logger.getLogger(QualityFloat.class.getName());
	private final TFloatArrayList data;
	private QualityProcedure qproc;
	private int [] sorted;
	private float [] bounds;
	private int layers = -1;
	private float scaleFactor = 1.0f;
	private float qmin, qmax;
	// qavg and qavg2 have to be stored into doubles, otherwise
	// standard deviation may be miscomputed with very large data
	// collections.
	private double qavg, qavg2;
	private int imin, imax;

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
	public final void setQualityProcedure(QualityProcedure q)
	{
		qproc = q;
		qproc.bindResult(data);
		scaleFactor = qproc.getScaleFactor();
	}
	
	/**
	 * Compute the quality of an object and add it to the list.
	 *
	 * @param x  the object on which quality is computed.
	 */
	public final void compute(Object x)
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
	public final void finish()
	{
		qproc.finish();
		qmin = Float.MAX_VALUE;
		qmax = Float.MIN_VALUE;
		qavg = 0.0;
		qavg2 = 0.0;
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i) * scaleFactor;
			data.set(i, val);
		}
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i);
			double dval = val;
			qavg += dval ;
			qavg2 += dval * dval;
			if (qmin > val)
			{
				qmin = val;
				imin = i;
			}
			if (qmax < val)
			{
				qmax = val;
				imax = i;
			}
		}
		qavg /= data.size();
		qavg2 /= data.size();
	}
	
	/**
	 * Return value by its distribution index.  Returned value is
	 * such that there are <code>p*N</code> values below it, where
	 * <code>N</code> is the total number of values.  For instance,
	 * <code>getValueByPercent(0.0)</code> (resp. 1 and 0.5) returns
	 * minimum value (resp. maximum value and median value).
	 *
	 * @param p  number between 0 and 1
	 * @return  value associated to this distribution index
	 */
	public final float getValueByPercent(double p)
	{
		if (p <= 0.0)
			return qmin;
		if (p >= 1.0)
			return qmax;
		float [] values = new float[1000];
		int [] number = new int[values.length+1];
		int target = (int) (p * data.size());
		return getValueByPercentPrivate(target, qmin, qmax, values, number);
	}
	
	private float getValueByPercentPrivate(int target, float q1, float q2, float [] values, int [] number)
	{
		float delta = (q2 - q1) / values.length;
		if (delta == 0.0f)
			return q1;
		if (delta < 0.0f)
			throw new IllegalArgumentException();
		for (int i = 0; i < values.length; i++)
			values[i] = q1 + i * delta;
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i);
			int cell = (int) ((val - q1) / delta + 1.001f);
			if (cell <= 0)
				number[0]++;
			else if (cell < number.length)
				number[cell]++;
			else
				number[number.length - 1]++;
		}
		for (int i = 1; i < number.length; i++)
			number[i] += number[i-1];
		for (int i = 1; i < number.length; i++)
		{
			if (number[i] == target)
				return values[i-1];
			else if (number[i] > target)
			{
				if (number[i]-number[i-1] <= 1 || i == number.length - 1)
					return values[i-1];
				return getValueByPercentPrivate(target, values[i-1], values[i], values, number);
			}
		}
		throw new RuntimeException();
	}

	/**
	 * Return mean value
	 */
	public float getMeanValue()
	{
		return (float) qavg;
	}
	
	/**
	 * Return standard deviation
	 */
	public float getStandardDeviation()
	{
		return (float) Math.sqrt(qavg2 - qavg*qavg);
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
	public final void setTarget(float factor)
	{
		scaleFactor = 1.0f / factor;
	}
	
	/**
	 * Split quality values into buckets.  The minimal and
	 * maximal quality values are computed, this range is divided
	 * into <code>n</code> subsegments of equal length, and 
	 * the number of quality values for each subsegment is
	 * computed.  These numbers can then be displayed by
	 * {@link #printLayers}.
	 *
	 * @param nr  the desired number of subsegments.
	 */
	public void split(int nr)
	{
		layers = nr;
		if (layers <= 0)
			return;
		//  min() and max() methods are buggy in trove 1.0.2
		float delta = (qmax - qmin) / layers;
		// In printLayers:
		//   sorted[0]: number of points with value < qmin
		//   sorted[layers+1]: number of points with value > qmax
		sorted = new int[layers+2];
		bounds = new float[layers+1];
		for (int i = 0; i < bounds.length; i++)
			bounds[i] = qmin + i * delta;
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i);
			int cell = (int) ((val - qmin) / delta + 1.001f);
			assert cell > 0 && cell <= layers;
			sorted[cell]++;
		}
	}
	
	/**
	 * Split quality values into buckets.  The range between minimal
	 * and maximal quality values is divided into <code>nr</code>
	 * subsegments of equal length, and the number of quality values
	 * for each subsegment is computed.  These numbers can then be
	 * displayed by {@link #printLayers}.
	 *
	 * @param v1  minimal value to consider.
	 * @param v2  maximal value to consider.
	 * @param nr  the desired number of subsegments.
	 */
	public final void split(float v1, float v2, int nr)
	{
		layers = nr;
		float vmin = v1;
		float vmax = v2;
		if (layers <= 0)
			return;
		//  The last cell is for v >= vmax
		float delta = (vmax - vmin) / layers;
		sorted = new int[layers+2];
		bounds = new float[layers+1];
		for (int i = 0; i < bounds.length; i++)
			bounds[i] = vmin + i * delta;
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i);
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
	
	public void split(float... v)
	{
		layers = v.length - 1;
		if (layers < 0)
			return;
		bounds = new float[layers+1];
		int cnt = 0;
		for (float f: v)
			bounds[cnt++] = f;
		sorted = new int[layers+2];
		for (int i = 0, n = data.size(); i < n; i++)
		{
			float val = data.get(i);
			int cell = 0;
			for (; cell < bounds.length; cell++)
				if (val < bounds[cell])
					break;
			sorted[cell]++;
		}
	}
	
	/**
	 * Display histogram about quality values.
	 */
	public final void printLayers()
	{
		if (layers < 0)
		{
			logger.severe("split() method must be called before printLayers()");
			return;
		}
		int nrTotal = data.size();
		if (sorted[0] > 0)
			System.out.printf(" < %g %d (%.4g%%)%n", bounds[0], sorted[0], (((float) 100.0 * sorted[0])/nrTotal));
		for (int i = 0; i < layers; i++)
		{
			System.out.printf(" %g ; %g %d (%.4g%%)%n", bounds[i], bounds[i+1], sorted[i+1], (((float) 100.0 * sorted[i+1])/nrTotal));
		}
		if (sorted[layers+1] > 0)
			System.out.printf(" > %g %d (%.4g%%)%n", bounds[layers], sorted[layers+1], (((float) 100.0 * sorted[layers+1])/nrTotal));
		printStatistics();
	}
	
	/**
	 * Display statistics about quality values.
	 */
	public final void printStatistics()
	{
		int nrTotal = data.size();
		System.out.println("total: "+nrTotal);
		System.out.printf("qmin: %.6g (index=%d starting from 0)%n", qmin, imin);
		System.out.printf("qmax: %.6g (index=%d starting from 0)%n", qmax, imax);
		System.out.printf("qavg: %.6g%n", qavg);
		System.out.printf("qdev: %.6g%n", Math.sqrt(qavg2 - qavg*qavg));
	}
	
	/**
	 * Write quality values into a file.  They are stored in the
	 * BB medit format, in the same order as they have been
	 * computed.  This means that a mesh file had been written with
	 * the same order.
	 *
	 * @param file   name of the output file
	 */
	public final void printMeshBB(String file)
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
			logger.severe("Cannot write into: "+file);
		}
	}

	/**
	 * Write quality values into a raw file.  They are stored in
	 * machine format, in the same order as they have been computed.
	 *
	 * @param file   name of the output file
	 */
	public void writeRawData(String file)
	{
		try
		{
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			for (int i = 0, n = data.size(); i < n; i++)
				out.writeFloat(data.get(i));
			out.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.severe("Cannot write into: "+file);
		}
		catch (IOException ex)
		{
			logger.severe("Error when writing data into "+file);
		}
	}
}
