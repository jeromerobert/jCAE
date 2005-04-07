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
	// Number of points
	protected int nr = 0;
	protected double length = -1.0;
	protected double [] a;
	protected double start, end;
	
	public OCCDiscretizeCurve3D(Adaptor3d_Curve myCurve, double s, double e)
	{
		curve = myCurve;
		start = s;
		end = e;
	}
	
	public void discretizeMaxLength(double len)
	{
		logger.debug("Discretize with max length: "+len);
		int nsegments = 10;
		double [] xyz;
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			double delta = (end - start) / ((double) nsegments);
			xyz = new double[3*(nsegments+1)];
			for (int i = 0; i < nsegments; i++)
				xyz[3*i] = start + ((double) i) * delta;
			//  Avoid rounding errors
			xyz[3*nsegments] = end;
			curve.arrayValues(nsegments + 1, xyz);
			
			double abscissa, dist;
			nr = 1;
			a[0] = start;
			for (int ns = 1; ns < nsegments; ns++)
			{
				abscissa = start + ((double) ns) * delta;
				dist = Math.sqrt(
				  (xyz[3*nr-3] - xyz[3*ns  ]) * (xyz[3*nr-3] - xyz[3*ns  ]) +
				  (xyz[3*nr-2] - xyz[3*ns+1]) * (xyz[3*nr-2] - xyz[3*ns+1]) +
				  (xyz[3*nr-1] - xyz[3*ns+2]) * (xyz[3*nr-1] - xyz[3*ns+2]));
				if (dist > len)
				{
					a[nr] = abscissa;
					if (nr < ns)
					{
						xyz[3*nr]   = xyz[3*ns];
						xyz[3*nr+1] = xyz[3*ns+1];
						xyz[3*nr+2] = xyz[3*ns+2];
					}
					nr++;
				}
			}
			a[nr] = end;
			if (nr < nsegments)
			{
				xyz[3*nr]   = xyz[3*nsegments];
				xyz[3*nr+1] = xyz[3*nsegments+1];
				xyz[3*nr+2] = xyz[3*nsegments+2];
			}
			nr++;
			//  Stop when there are at least 10 points per segments
			if (nr * 10 < nsegments)
				break;
		}
		logger.debug("(length) Number of points: "+nr);
		length = -1.0;
		adjustAbscissas(xyz, new checkRatioLength());
	}
	
	public void setDiscretization(double [] param)
	{
		nr = param.length;
		a = new double[nr];
		for (int i = 0; i < nr; i++)
			a[i] = param[i];
		length = -1.0;
	}
	
	private void split(int n)
	{
		nr = n + 1;
		a = new double[nr];
		double delta = (end - start) / ((double) n);
		for (int i = 0; i < n; i++)
			a[i] = start + ((double) i) * delta;
		
		//  Avoid rounding errors
		a[n] = end;
		length = -1.0;
	}
	
	public void splitSubsegment(int numseg, int nrsub)
	{
		if (numseg < 0 || numseg >= nr)
			return;
		OCCDiscretizeCurve3D ref = new OCCDiscretizeCurve3D(curve, a[numseg], a[numseg+1]);
		ref.split(nrsub);
		double [] newA = new double[nr+ref.nr-2];
		if (numseg > 0)
			System.arraycopy(a, 0, newA, 0, numseg);
		if (ref.nr > 0)
			System.arraycopy(ref.a, 0, newA, numseg, ref.nr);
		if (nr-numseg-2 > 0)
			System.arraycopy(a, numseg+2, newA, numseg+ref.nr, nr-numseg-2);
		a = newA;
		nr += ref.nr - 2;
	}
	
	public void discretizeSubsegmentMaxLength(int numseg, double len)
	{
		if (numseg < 0 || numseg >= nr)
			return;
		OCCDiscretizeCurve3D ref = new OCCDiscretizeCurve3D(curve, a[numseg], a[numseg+1]);
		ref.discretizeMaxLength(len);
		double [] newA = new double[nr+ref.nr-2];
		if (numseg > 0)
			System.arraycopy(a, 0, newA, 0, numseg);
		if (ref.nr > 0)
			System.arraycopy(ref.a, 0, newA, numseg, ref.nr);
		if (nr-numseg-2 > 0)
			System.arraycopy(a, numseg+2, newA, numseg+ref.nr, nr-numseg-2);
		a = newA;
		nr += ref.nr - 2;
	}
	
	public void discretizeNrPoints(int n)
	{
		nr = n;
		int nsegments = n;
		double [] xyz;
		ArrayList abscissa = new ArrayList(nsegments);
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			double delta = (end - start) / ((double) nsegments);
			xyz = new double[3*(nsegments+1)];
			for (int i = 0; i < nsegments; i++)
				xyz[3*i] = start + ((double) i) * delta;
			//  Avoid rounding errors
			xyz[3*nsegments] = end;
			curve.arrayValues(nsegments + 1, xyz);
			
			a[0] = start;
			//  Compute length, a[] and xyz[]
			double length = 0.0;
			for (int ns = 1; ns <= nsegments; ns++)
			{
				a[ns] = start + ns * delta;
				length += Math.sqrt(
				  (xyz[3*ns-3] - xyz[3*ns  ]) * (xyz[3*ns-3] - xyz[3*ns  ]) +
				  (xyz[3*ns-2] - xyz[3*ns+1]) * (xyz[3*ns-2] - xyz[3*ns+1]) +
				  (xyz[3*ns-1] - xyz[3*ns+2]) * (xyz[3*ns-1] - xyz[3*ns+2]));
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
				  		(xyz[3*ns  ] - xyz[3*lastIndex  ]) * (xyz[3*ns  ] - xyz[3*lastIndex  ]) +
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
				if (lmax - lmin < 0.5 * delta)
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
		adjustAbscissas(xyz, new checkRatioLength());
	}
	
	public void discretizeMaxDeflection(double defl, boolean relDefl)
	{
		if (defl <= 0.0)
			return;
		int nsegments = 10;
		double [] xyz;
		while (true)
		{
			nsegments *= 10;
			a = new double[nsegments+1];
			double delta = (end - start) / ((double) nsegments);
			xyz = new double[3*(nsegments+1)];
			for (int i = 0; i < nsegments; i++)
				xyz[3*i] = start + ((double) i) * delta;
			//  Avoid rounding errors
			xyz[3*nsegments] = end;
			curve.arrayValues(nsegments + 1, xyz);
			
			double oldAbscissa, newAbscissa;
			double dist, arcLength;
			nr = 1;
			a[0] = start;
			arcLength   = 0.0;
			oldAbscissa = start;
			for (int ns = 1; ns < nsegments; ns++)
			{
				newAbscissa = start + ((double) ns) * delta;
				dist = Math.sqrt(
				  (xyz[3*nr-3] - xyz[3*ns  ]) * (xyz[3*nr-3] - xyz[3*ns  ]) +
				  (xyz[3*nr-2] - xyz[3*ns+1]) * (xyz[3*nr-2] - xyz[3*ns+1]) +
				  (xyz[3*nr-1] - xyz[3*ns+2]) * (xyz[3*nr-1] - xyz[3*ns+2]));
				arcLength += length(oldAbscissa, newAbscissa, 20);
				oldAbscissa = newAbscissa;
				double dmax = defl;
				if (relDefl)
					dmax *= arcLength;
				if (arcLength - dist > dmax)
				{
					a[nr] = newAbscissa;
					arcLength   = 0.0;
					if (nr < ns)
					{
						xyz[3*nr]   = xyz[3*ns];
						xyz[3*nr+1] = xyz[3*ns+1];
						xyz[3*nr+2] = xyz[3*ns+2];
					}
					nr++;
				}
			}
			a[nr] = end;
			if (nr < nsegments)
			{
				xyz[3*nr]   = xyz[3*nsegments];
				xyz[3*nr+1] = xyz[3*nsegments+1];
				xyz[3*nr+2] = xyz[3*nsegments+2];
			}
			nr++;
			//  Stop when there are at least 10 points per segments
			if (nr * 10 < nsegments)
				break;
		}
		logger.debug("(deflection) Number of points: "+nr);
		length = -1.0;
		adjustAbscissas(xyz, new checkRatioDeflection());
	}
	
	private void adjustAbscissas(double [] xyz, checkRatio func)
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
					redo |= func.move(i, xyz);
			}
			else
			{
				for (int i = 1; i < nr - 1; i++)
					redo |= func.move(i, xyz);
			}
			if (!redo)
				break;
		}
	}
	
	private interface checkRatio
	{
		boolean move(int i, double [] xyz);
	}
	
	private class checkRatioLength implements checkRatio
	{
		public boolean move(int i, double [] xyz)
		{
			boolean ret = false;
			double l1 = Math.sqrt(
			  (xyz[3*i  ] - xyz[3*i-3]) * (xyz[3*i  ] - xyz[3*i-3]) +
			  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
			  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
			double l2 = Math.sqrt(
			  (xyz[3*i  ] - xyz[3*i+3]) * (xyz[3*i  ] - xyz[3*i+3]) +
			  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
			  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
			double delta = Math.abs(l2 - l1);
			if (delta > 0.05 * (l1+l2))
			{
				double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
				double [] newXYZ = curve.value(newA);
				
				double newl1 = Math.sqrt(
				  (newXYZ[0] - xyz[3*i-3]) * (newXYZ[0] - xyz[3*i-3]) +
				  (newXYZ[1] - xyz[3*i-2]) * (newXYZ[1] - xyz[3*i-2]) +
				  (newXYZ[2] - xyz[3*i-1]) * (newXYZ[2] - xyz[3*i-1]));
				double newl2 = Math.sqrt(
				  (newXYZ[0] - xyz[3*i+3]) * (newXYZ[0] - xyz[3*i+3]) +
				  (newXYZ[1] - xyz[3*i+4]) * (newXYZ[1] - xyz[3*i+4]) +
				  (newXYZ[2] - xyz[3*i+5]) * (newXYZ[2] - xyz[3*i+5]));
				if (Math.abs(newl2 - newl1) < delta)
				{
					ret = true;
					a[i] = newA;
					xyz[3*i]   = newXYZ[0];
					xyz[3*i+1] = newXYZ[1];
					xyz[3*i+2] = newXYZ[2];
				}
			}
			return ret;
		}
	}
	
	private class checkRatioDeflection implements checkRatio
	{
		public boolean move(int i, double [] xyz)
		{
			boolean ret = false;
			double l1 = Math.sqrt(
			  (xyz[3*i  ] - xyz[3*i-3]) * (xyz[3*i  ] - xyz[3*i-3]) +
			  (xyz[3*i+1] - xyz[3*i-2]) * (xyz[3*i+1] - xyz[3*i-2]) +
			  (xyz[3*i+2] - xyz[3*i-1]) * (xyz[3*i+2] - xyz[3*i-1]));
			double l2 = Math.sqrt(
			  (xyz[3*i  ] - xyz[3*i+3]) * (xyz[3*i  ] - xyz[3*i+3]) +
			  (xyz[3*i+1] - xyz[3*i+4]) * (xyz[3*i+1] - xyz[3*i+4]) +
			  (xyz[3*i+2] - xyz[3*i+5]) * (xyz[3*i+2] - xyz[3*i+5]));
			double a1 = length(a[i-1], a[i], 20);
			double a2 = length(a[i], a[i+1], 20);
			double d1 = (a1 - l1) / a1;
			double d2 = (a2 - l2) / a2;
			double d3 = (a1 + a2 - l1 - l2) / (a1 + a2);
			double delta = Math.abs(d2 - d1);
			if (delta > 0.05 * d3)
			{
				double newA = a[i] + 0.8 * (a[i+1] - a[i-1]) * (l2 - l1) / (l1 + l2);
				double [] newXYZ = curve.value(newA);
				
				l1 = Math.sqrt(
				  (newXYZ[0] - xyz[3*i-3]) * (newXYZ[0] - xyz[3*i-3]) +
				  (newXYZ[1] - xyz[3*i-2]) * (newXYZ[1] - xyz[3*i-2]) +
				  (newXYZ[2] - xyz[3*i-1]) * (newXYZ[2] - xyz[3*i-1]));
				l2 = Math.sqrt(
				  (newXYZ[0] - xyz[3*i+3]) * (newXYZ[0] - xyz[3*i+3]) +
				  (newXYZ[1] - xyz[3*i+4]) * (newXYZ[1] - xyz[3*i+4]) +
				  (newXYZ[2] - xyz[3*i+5]) * (newXYZ[2] - xyz[3*i+5]));
				a1 = length(a[i-1], newA, 20);
				a2 = length(newA, a[i+1], 20);
				d1 = (a1 - l1) / a1;
				d2 = (a2 - l2) / a2;
				if (Math.abs(d2 - d1) < delta)
				{
					ret = true;
					a[i] = newA;
					xyz[3*i]   = newXYZ[0];
					xyz[3*i+1] = newXYZ[1];
					xyz[3*i+2] = newXYZ[2];
				}
			}
			return ret;
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
	
	private double length(double start, double end, int nrsub)
	{
		assert nr > 0;
		double delta = (end - start) / ((double) nrsub);
		double l = 0.0;
		double [] xyz = new double[3*(nrsub+1)];
		for (int i = 0; i < nrsub; i++)
			xyz[3*i] = start + ((double) i) * delta;
		//  Avoid rounding errors
		xyz[3*nrsub] = end;
		curve.arrayValues(nrsub + 1, xyz);
		
		for (int i = 0; i < 3 * nrsub; i+=3)
		{
			l += Math.sqrt(
			  (xyz[i+3] - xyz[i  ]) * (xyz[i+3] - xyz[i  ]) +
			  (xyz[i+4] - xyz[i+1]) * (xyz[i+4] - xyz[i+1]) +
			  (xyz[i+5] - xyz[i+2]) * (xyz[i+5] - xyz[i+2]));
		}
		return l;
	}
	
	public double length()
	{
		if (length >= 0.0)
			return length;
		
		assert nr > 0;
		double [] xyz = new double[3*nr];
		for (int i = 0; i < nr; i++)
			xyz[3*i] = a[i];
		curve.arrayValues(nr, xyz);
		
		length = 0.0;
		for (int i = 3; i < 3*nr; i+=3)
			length += Math.sqrt(
			  (xyz[i-3] - xyz[i  ]) * (xyz[i-3] - xyz[i  ]) +
			  (xyz[i-2] - xyz[i+1]) * (xyz[i-2] - xyz[i+1]) +
			  (xyz[i-1] - xyz[i+2]) * (xyz[i-1] - xyz[i+2]));
		return length;
	}
}
