/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.cad.occ;

import org.jcae.opencascade.jni.Adaptor3d_Curve;
import org.apache.log4j.Logger;
import java.util.ArrayList;

public class OCCDiscretizeCurve3D
{
	private static Logger logger=Logger.getLogger(OCCDiscretizeCurve3D.class);
	protected Adaptor3d_Curve curve = null;
	protected int nr = 0;
	protected double length = -1.0;
	protected double [] a;
	
	public void initialize(Adaptor3d_Curve myCurve, double len, double start, double end)
	{
		curve = myCurve;
		int nsegments = 10;
		double [] xyz;
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			xyz = new double[3*(nsegments+1)];
			double [] oldXYZ = curve.value(start);
			double [] newXYZ;
			double abscissa, dist;
			nr = 1;
			a[0] = start;
			for (int i = 0; i < 3; i++)
				xyz[i] = oldXYZ[i];
			for (int ns = 1; ns < nsegments; ns++)
			{
				abscissa = start + ns * (end - start) / ((double) nsegments);
				newXYZ = curve.value(abscissa);
				dist = Math.sqrt(
				  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
				  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
				  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
				if (dist > len)
				{
					a[nr] = abscissa;
					oldXYZ = newXYZ;
					xyz[3*nr]   = oldXYZ[0];
					xyz[3*nr+1] = oldXYZ[1];
					xyz[3*nr+2] = oldXYZ[2];
					nr++;
				}
			}
			a[nr] = end;
			oldXYZ = curve.value(end);
			xyz[3*nr]   = oldXYZ[0];
			xyz[3*nr+1] = oldXYZ[1];
			xyz[3*nr+2] = oldXYZ[2];
			nr++;
			//  Stop when there are at least 10 points per segments
			if (nr * 10 < nsegments)
				break;
		}
		logger.debug("Number of ponts: "+nr);
		length = -1.0;
		adjustAbscissas(xyz);
	}
	
	//  Placeholder
	public void initialize(Adaptor3d_Curve myCurve, double len, double defl, double start, double end)
	{
		initialize(myCurve, len, start, end);
	}
	
	public void initialize(Adaptor3d_Curve myCurve, int n, double start, double end)
	{
		curve = myCurve;
		nr = n;
		int nsegments = n;
		double [] xyz;
		ArrayList abscissa = new ArrayList(nsegments);
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			xyz = new double[3*(nsegments+1)];
			double [] oldXYZ = curve.value(start);
			double [] newXYZ;
			a[0] = start;
			for (int i = 0; i < 3; i++)
				xyz[i] = oldXYZ[i];
			double deltap = (end - start) / ((double) nsegments);
			//  Compute length, a[] and xyz[]
			double length = 0.0;
			for (int ns = 1; ns <= nsegments; ns++)
			{
				a[ns] = start + ns * deltap;
				newXYZ = curve.value(a[ns]);
				length += Math.sqrt(
				  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
				  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
				  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
				oldXYZ = newXYZ;
				xyz[3*ns]   = oldXYZ[0];
				xyz[3*ns+1] = oldXYZ[1];
				xyz[3*ns+2] = oldXYZ[2];
			}
			double lmax = 2.0 * length / ((double) nr);
			double lmin = 0.0;
			double maxlen, dist;
			while (true)
			{
				maxlen = 0.5 * (lmin + lmax);
				int lastIndex = 0;
				abscissa.clear();
				abscissa.add(new Integer(0));
				nr = 1;
				for (int ns = 1; ns < nsegments; ns++)
				{
					dist = Math.sqrt(
				  		(xyz[3*ns] - xyz[3*lastIndex]) * (xyz[3*ns] - xyz[3*lastIndex]) +
				  		(xyz[3*ns+1] - xyz[3*lastIndex+1]) * (xyz[3*ns+1] - xyz[3*lastIndex+1]) +
				  		(xyz[3*ns+2] - xyz[3*lastIndex+2]) * (xyz[3*ns+2] - xyz[3*lastIndex+2]));
					if (dist > maxlen)
					{
						lastIndex = ns;
						nr++;
						abscissa.add(new Integer(ns));
					}
				}
				nr++;
				abscissa.add(new Integer(nsegments));
				if (n == nr)
					break;
				else if (nr < n)
					lmax = lmax - 0.5 * (lmax - lmin);
				else
					lmin = lmin + 0.5 * (lmax - lmin);
				if (lmax - lmin < 0.5 * deltap)
					break;
			}
			if (n == nr)
				break;
		}
		for (int i = 0; i < nr; i++)
		{
			int ind = ((Integer) abscissa.get(i)).intValue();
			if (ind != i)
			{
				a[i] = a[ind];
				xyz[3*i]   = xyz[3*ind];
				xyz[3*i+1] = xyz[3*ind+1];
				xyz[3*i+2] = xyz[3*ind+2];
			}
		}
		length = -1.0;
		adjustAbscissas(xyz);
	}
	
	private void adjustAbscissas(double [] xyz)
	{
		boolean backward = false;
		int niter = 2*nr;
		while (niter > 0)
		{
			niter--;
			backward = ! backward;
			boolean redo = false;
			if (backward)
			{
				for (int i = nr - 2; i > 0; i--)
				{
					double l1 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i-3]) * (xyz[3*i] - xyz[3*i-3]) +
					  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
					  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
					double l2 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i+3]) * (xyz[3*i] - xyz[3*i+3]) +
					  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
					  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
					double delta = Math.abs(l2 - l1) / (l1+l2);
					if (delta > 0.01 * 0.5)
					{
						redo = true;
						double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
						double [] newXYZ = curve.value(a[i]);

						double newl1 = Math.sqrt(
						  (newXYZ[0] - xyz[3*i-3]) * (newXYZ[0] - xyz[3*i-3]) +
						  (newXYZ[1] - xyz[3*i-2]) * (newXYZ[1] - xyz[3*i-2]) +
						  (newXYZ[2] - xyz[3*i-1]) * (newXYZ[2] - xyz[3*i-1]));
						double newl2 = Math.sqrt(
						  (newXYZ[0] - xyz[3*i+3]) * (newXYZ[0] - xyz[3*i+3]) +
						  (newXYZ[1] - xyz[3*i+4]) * (newXYZ[1] - xyz[3*i+4]) +
						  (newXYZ[2] - xyz[3*i+5]) * (newXYZ[2] - xyz[3*i+5]));
						if (Math.abs(newl2 - newl1)/(newl1+newl2) < delta)
						{
							a[i] = newA;
							xyz[3*i]   = newXYZ[0];
							xyz[3*i+1] = newXYZ[1];
							xyz[3*i+2] = newXYZ[2];
						}
					}
				}
			}
			else
			{
				for (int i = 1; i < nr - 1; i++)
				{
					double l1 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i-3]) * (xyz[3*i] - xyz[3*i-3]) +
					  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
					  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
					double l2 = Math.sqrt(
					  (xyz[3*i] - xyz[3*i+3]) * (xyz[3*i] - xyz[3*i+3]) +
					  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
					  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
					if (Math.abs(l2 - l1) > 0.01 * 0.5 * (l1+l2))
					{
						redo = true;
						double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
						if (newA > a[i-1] && newA < a[i+1])
							a[i] = newA;
						double [] newXYZ = curve.value(a[i]);
						xyz[3*i]   = newXYZ[0];
						xyz[3*i+1] = newXYZ[1];
						xyz[3*i+2] = newXYZ[2];
					}
				}
			}
			if (!redo)
				break;
		}
	}
	
	public int nbPoints()
	{
		return nr;
	}
	
	public double parameter(int index)
	{
		return a[index-1];
	}
	
	public double length()
	{
		if (length >= 0.0)
			return length;
		
		assert nr > 0;
		double [] oldXYZ;
		double [] newXYZ = curve.value(a[0]);
		length = 0.0;
		for (int i = 1; i < nr; i++)
		{
			oldXYZ = newXYZ;
			newXYZ = curve.value(a[i]);
			length += Math.sqrt(
			  (oldXYZ[0] - newXYZ[0]) * (oldXYZ[0] - newXYZ[0]) +
			  (oldXYZ[1] - newXYZ[1]) * (oldXYZ[1] - newXYZ[1]) +
			  (oldXYZ[2] - newXYZ[2]) * (oldXYZ[2] - newXYZ[2]));
		}
		return length;
	}
}
